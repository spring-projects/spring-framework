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

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.RxNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests with server-side {@link WebSocketHandler}s.
 * @author Rossen Stoyanchev
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class WebSocketIntegrationTests extends AbstractWebSocketIntegrationTests {


	@Override
	protected Class<?> getWebConfigClass() {
		return WebConfig.class;
	}


	@Test
	public void echoReactorNettyClient() throws Exception {
		testEcho(new ReactorNettyWebSocketClient());
	}

	@Test
	public void echoRxNettyClient() throws Exception {
		testEcho(new RxNettyWebSocketClient());
	}

	private void testEcho(WebSocketClient client) throws URISyntaxException {
		int count = 100;
		Flux<String> input = Flux.range(1, count).map(index -> "msg-" + index);
		ReplayProcessor<Object> output = ReplayProcessor.create(count);

		client.execute(getUrl("/echo"),
				session -> session
						.send(input.map(session::textMessage))
						.thenMany(session.receive().take(count).map(WebSocketMessage::getPayloadAsText))
						.subscribeWith(output)
						.then())
				.blockMillis(5000);

		assertEquals(input.collectList().blockMillis(5000), output.collectList().blockMillis(5000));
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
