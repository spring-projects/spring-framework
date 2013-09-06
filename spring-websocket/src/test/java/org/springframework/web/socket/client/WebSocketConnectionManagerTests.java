/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.socket.client;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Test;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.WebSocketHandlerAdapter;
import org.springframework.web.socket.support.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.support.WebSocketHandlerDecorator;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link WebSocketConnectionManager}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketConnectionManagerTests {

	@Test
	public void openConnection() throws Exception {

		List<String> subprotocols = Arrays.asList("abc");

		TestLifecycleWebSocketClient client = new TestLifecycleWebSocketClient(false);
		WebSocketHandler handler = new WebSocketHandlerAdapter();

		WebSocketConnectionManager manager = new WebSocketConnectionManager(client, handler , "/path/{id}", "123");
		manager.setSubProtocols(subprotocols);
		manager.openConnection();

		HttpHeaders expectedHeaders = new HttpHeaders();
		expectedHeaders.setSecWebSocketProtocol(subprotocols);

		assertEquals(expectedHeaders, client.headers);
		assertEquals(new URI("/path/123"), client.uri);

		WebSocketHandlerDecorator loggingHandler = (WebSocketHandlerDecorator) client.webSocketHandler;
		assertEquals(LoggingWebSocketHandlerDecorator.class, loggingHandler.getClass());

		assertSame(handler, loggingHandler.getDelegate());
	}

	@Test
	public void syncClientLifecycle() throws Exception {

		TestLifecycleWebSocketClient client = new TestLifecycleWebSocketClient(false);
		WebSocketHandler handler = new WebSocketHandlerAdapter();
		WebSocketConnectionManager manager = new WebSocketConnectionManager(client, handler , "/a");

		manager.startInternal();
		assertTrue(client.isRunning());

		manager.stopInternal();
		assertFalse(client.isRunning());
	}

	@Test
	public void dontSyncClientLifecycle() throws Exception {

		TestLifecycleWebSocketClient client = new TestLifecycleWebSocketClient(true);
		WebSocketHandler handler = new WebSocketHandlerAdapter();
		WebSocketConnectionManager manager = new WebSocketConnectionManager(client, handler , "/a");

		manager.startInternal();
		assertTrue(client.isRunning());

		manager.stopInternal();
		assertTrue(client.isRunning());
	}


	private static class TestLifecycleWebSocketClient implements WebSocketClient, SmartLifecycle {

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
		public int getPhase() {
			return 0;
		}

		@Override
		public boolean isAutoStartup() {
			return false;
		}

		@Override
		public void stop(Runnable callback) {
			this.running = false;
		}

		@Override
		public ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
				String uriTemplate, Object... uriVars) {

			URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode().toUri();
			return doHandshake(webSocketHandler, null, uri);
		}

		@Override
		public ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
				HttpHeaders headers, URI uri) {

			this.webSocketHandler = webSocketHandler;
			this.headers = headers;
			this.uri = uri;

			return new ListenableFutureTask<WebSocketSession>(new Callable<WebSocketSession>() {
				@Override
				public WebSocketSession call() throws Exception {
					return null;
				}
			});
		}
	}

}
