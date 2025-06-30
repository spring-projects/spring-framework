/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.client;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link WebSocketConnectionManager}.
 *
 * @author Rossen Stoyanchev
 */
class WebSocketConnectionManagerTests {

	@Test
	void openConnection() {
		List<String> subprotocols = List.of("abc");

		TestLifecycleWebSocketClient client = new TestLifecycleWebSocketClient(false);
		WebSocketHandler handler = new TextWebSocketHandler();

		WebSocketConnectionManager manager = new WebSocketConnectionManager(client, handler , "/path/{id}", "123");
		manager.setSubProtocols(subprotocols);
		manager.openConnection();

		WebSocketHttpHeaders expectedHeaders = new WebSocketHttpHeaders();
		expectedHeaders.setSecWebSocketProtocol(subprotocols);

		assertThat(client.headers).isEqualTo(expectedHeaders);
		assertThat(client.uri).isEqualTo(URI.create("/path/123"));

		WebSocketHandlerDecorator loggingHandler = (WebSocketHandlerDecorator) client.webSocketHandler;
		assertThat(loggingHandler.getClass()).isEqualTo(LoggingWebSocketHandlerDecorator.class);

		assertThat(loggingHandler.getDelegate()).isSameAs(handler);
	}

	@Test
	void clientLifecycle() throws Exception {
		TestLifecycleWebSocketClient client = new TestLifecycleWebSocketClient(false);
		WebSocketHandler handler = new TextWebSocketHandler();
		WebSocketConnectionManager manager = new WebSocketConnectionManager(client, handler , "/a");

		manager.startInternal();
		assertThat(client.isRunning()).isTrue();

		manager.stopInternal();
		assertThat(client.isRunning()).isFalse();
	}


	private static class TestLifecycleWebSocketClient implements WebSocketClient, Lifecycle {

		private boolean running;

		private WebSocketHandler webSocketHandler;

		private HttpHeaders headers;

		private URI uri;


		public TestLifecycleWebSocketClient(boolean running) {
			this.running = running;
		}

		@Override
		public void start() {
			this.running = true;
		}

		@Override
		public void stop() {
			this.running = false;
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

		@Override
		public CompletableFuture<WebSocketSession> execute(WebSocketHandler handler,
				String uriTemplate, @Nullable Object... uriVars) {

			URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode().toUri();
			return execute(handler, null, uri);
		}

		@Override
		public CompletableFuture<WebSocketSession> execute(WebSocketHandler handler,
				WebSocketHttpHeaders headers, URI uri) {

			this.webSocketHandler = handler;
			this.headers = headers;
			this.uri = uri;
			return CompletableFuture.supplyAsync(() -> null);
		}
	}

}
