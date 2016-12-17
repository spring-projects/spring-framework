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
import java.util.Map;

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
 * Integration tests with server-side {@link WebSocketHandler}s.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ServerWebSocketIntegrationTests extends AbstractWebSocketIntegrationTests {


	@Override
	protected Class<?> getWebConfigClass() {
		return WebConfig.class;
	}


	@Test
	public void echo() throws Exception {
		int count = 100;
		Observable<String> input = Observable.range(1, count).map(index -> "msg-" + index);
		Observable<String> output = HttpClient.newClient("localhost", this.port)
				.createGet("/echo")
				.requestWebSocketUpgrade()
				.flatMap(WebSocketResponse::getWebSocketConnection)
				.flatMap(conn -> conn
						.write(input.map(TextWebSocketFrame::new)).cast(WebSocketFrame.class)
						.mergeWith(conn.getInput())
						.take(count)
						.map(frame -> {
							String text = frame.content().toString(StandardCharsets.UTF_8);
							frame.release();
							return text;
						}));
		assertEquals(input.toList().toBlocking().first(), output.toList().toBlocking().first());
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
