/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs.client;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Before;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.JettyWebSocketTestServer;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;

/**
 * SockJS integration tests using Jetty for client and server.
 *
 * @author Rossen Stoyanchev
 */
public class JettySockJsIntegrationTests extends AbstractSockJsIntegrationTests {

	private WebSocketClient webSocketClient;

	private HttpClient httpClient;


	@Before
	public void setup() throws Exception {
		super.setup();
		this.webSocketClient = new WebSocketClient();
		this.webSocketClient.start();
		this.httpClient = new HttpClient();
		this.httpClient.start();
	}

	@After
	public void teardown() throws Exception {
		super.teardown();
		try {
			this.webSocketClient.stop();
		}
		catch (Throwable ex) {
			logger.error("Failed to stop Jetty WebSocketClient", ex);
		}
		try {
			this.httpClient.stop();
		}
		catch (Throwable ex) {
			logger.error("Failed to stop Jetty HttpClient", ex);
		}
	}

	@Override
	protected JettyWebSocketTestServer createWebSocketTestServer() {
		return new JettyWebSocketTestServer();
	}

	@Override
	protected Class<?> upgradeStrategyConfigClass() {
		return JettyTestConfig.class;
	}

	@Override
	protected Transport getWebSocketTransport() {
		return new WebSocketTransport(new JettyWebSocketClient(this.webSocketClient));
	}

	@Override
	protected AbstractXhrTransport getXhrTransport() {
		return new JettyXhrTransport(this.httpClient);
	}


	@Configuration
	static class JettyTestConfig {

		@Bean
		public RequestUpgradeStrategy upgradeStrategy() {
			return new JettyRequestUpgradeStrategy();
		}
	}

}
