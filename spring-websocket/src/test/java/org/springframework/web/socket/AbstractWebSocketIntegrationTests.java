/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.socket;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameter;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.server.DefaultHandshakeHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.support.JettyRequestUpgradeStrategy;



/**
 * Base class for WebSocket integration tests.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractWebSocketIntegrationTests {

	private static Map<Class<?>, Class<?>> upgradeStrategyConfigTypes = new HashMap<Class<?>, Class<?>>();

	static {
		upgradeStrategyConfigTypes.put(JettyTestServer.class, JettyUpgradeStrategyConfig.class);
	}

	@Parameter(0)
	public TestServer server;

	@Parameter(1)
	public WebSocketClient webSocketClient;


	@Before
	public void setup() throws Exception {
		if (this.webSocketClient instanceof Lifecycle) {
			((Lifecycle) this.webSocketClient).start();
		}
	}

	@After
	public void teardown() throws Exception {
		try {
			if (this.webSocketClient instanceof Lifecycle) {
				((Lifecycle) this.webSocketClient).stop();
			}
		}
		finally {
			this.server.stop();
		}
	}

	protected String getWsBaseUrl() {
		return "ws://localhost:" + this.server.getPort();
	}

	protected Class<?> getUpgradeStrategyConfigClass() {
		return upgradeStrategyConfigTypes.get(this.server.getClass());
	}


	static abstract class AbstractRequestUpgradeStrategyConfig {

		@Bean
		public HandshakeHandler handshakeHandler() {
			return new DefaultHandshakeHandler(requestUpgradeStrategy());
		}

		public abstract RequestUpgradeStrategy requestUpgradeStrategy();
	}


	@Configuration
	static class JettyUpgradeStrategyConfig extends AbstractRequestUpgradeStrategyConfig {

		@Bean
		public RequestUpgradeStrategy requestUpgradeStrategy() {
			return new JettyRequestUpgradeStrategy();
		}
	}

}
