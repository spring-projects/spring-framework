/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.reactive.socket.server;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.ws.client.WebSocketResponse;
import org.junit.Test;
import reactor.core.publisher.Mono;
import rx.Observable;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

import static org.junit.Assert.assertEquals;

/**
 * Basic WebSocket integration tests.
 * @author Rossen Stoyanchev
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class BasicWebSocketHandlerIntegrationTests extends AbstractWebSocketHandlerIntegrationTests {


	@Override
	protected Class<?> getWebConfigClass() {
		return WebConfig.class;
	}


	@Test
	public void echo() throws Exception {
		Observable<String> messages = Observable.range(1, 10).map(i -> "Interval " + i);
		List<String> actual = HttpClient.newClient("localhost", this.port)
				.createGet("/echo")
				.requestWebSocketUpgrade()
				.flatMap(WebSocketResponse::getWebSocketConnection)
				.flatMap(conn -> conn.write(messages
						.map(TextWebSocketFrame::new)
						.cast(WebSocketFrame.class)
						.concatWith(Observable.just(new CloseWebSocketFrame())))
						.cast(WebSocketFrame.class)
						.mergeWith(conn.getInput())
				)
				.take(10)
				.map(frame -> frame.content().toString(StandardCharsets.UTF_8))
				.toList().toBlocking().first();
		List<String> expected = messages.toList().toBlocking().first();
		assertEquals(expected, actual);
	}


	@Configuration
	static class WebConfig {

		@Bean
		public HandlerMapping handlerMapping() {

			Map<String, WebSocketHandler> map = new HashMap<>();
			map.put("/echo", new EchoWebSocketHandler());

			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			mapping.setUrlMap(map);
			return mapping;
		}

	}

	private static class EchoWebSocketHandler implements WebSocketHandler {

		@Override
		public Mono<Void> handle(WebSocketSession session) {
			return session.send(session.receive());
		}
	}

}
