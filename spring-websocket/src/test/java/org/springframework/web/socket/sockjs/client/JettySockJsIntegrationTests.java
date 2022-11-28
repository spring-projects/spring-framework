/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.socket.sockjs.client;

import org.eclipse.jetty.client.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.JettyWebSocketTestServer;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;

/**
 * SockJS integration tests using Jetty for client and server.
 *
 * @author Rossen Stoyanchev
 */
class JettySockJsIntegrationTests extends AbstractSockJsIntegrationTests {

	@Override
	protected Class<?> upgradeStrategyConfigClass() {
		return JettyTestConfig.class;
	}

	@Override
	protected JettyWebSocketTestServer createWebSocketTestServer() {
		return new JettyWebSocketTestServer();
	}

	@SuppressWarnings("removal")
	@Override
	protected Transport createWebSocketTransport() {
		return new WebSocketTransport(new org.springframework.web.socket.client.jetty.JettyWebSocketClient());
	}

	@Override
	protected AbstractXhrTransport createXhrTransport() {
		return new JettyXhrTransport(new HttpClient());
	}


	@Configuration(proxyBeanMethods = false)
	static class JettyTestConfig {
		@Bean
		RequestUpgradeStrategy upgradeStrategy() {
			return new JettyRequestUpgradeStrategy();
		}
	}

}
