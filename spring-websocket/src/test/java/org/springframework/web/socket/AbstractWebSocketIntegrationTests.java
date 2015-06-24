/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runners.Parameterized.Parameter;

import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.UndertowRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Base class for WebSocket integration tests.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public abstract class AbstractWebSocketIntegrationTests {

	protected Log logger = LogFactory.getLog(getClass());

	private static Map<Class<?>, Class<?>> upgradeStrategyConfigTypes = new HashMap<Class<?>, Class<?>>();

	static {
		upgradeStrategyConfigTypes.put(JettyWebSocketTestServer.class, JettyUpgradeStrategyConfig.class);
		upgradeStrategyConfigTypes.put(TomcatWebSocketTestServer.class, TomcatUpgradeStrategyConfig.class);
		upgradeStrategyConfigTypes.put(UndertowTestServer.class, UndertowUpgradeStrategyConfig.class);
	}

	@Rule
	public final TestName testName = new TestName();

	@Parameter(0)
	public WebSocketTestServer server;

	@Parameter(1)
	public WebSocketClient webSocketClient;

	protected AnnotationConfigWebApplicationContext wac;


	@Before
	public void setup() throws Exception {

		logger.debug("Setting up '" + this.testName.getMethodName() + "', client=" +
				this.webSocketClient.getClass().getSimpleName() + ", server=" +
				this.server.getClass().getSimpleName());

		this.wac = new AnnotationConfigWebApplicationContext();
		this.wac.register(getAnnotatedConfigClasses());
		this.wac.register(upgradeStrategyConfigTypes.get(this.server.getClass()));

		if (this.webSocketClient instanceof Lifecycle) {
			((Lifecycle) this.webSocketClient).start();
		}

		this.server.setup();
		this.server.deployConfig(this.wac);
		// Set ServletContext in WebApplicationContext after deployment but before
		// starting the server.
		this.wac.setServletContext(this.server.getServletContext());
		this.wac.refresh();
		this.server.start();
	}

	protected abstract Class<?>[] getAnnotatedConfigClasses();

	@After
	public void teardown() throws Exception {
		try {
			if (this.webSocketClient instanceof Lifecycle) {
				((Lifecycle) this.webSocketClient).stop();
			}
		}
		catch (Throwable t) {
			logger.error("Failed to stop WebSocket client", t);
		}
		try {
			this.server.undeployConfig();
		}
		catch (Throwable t) {
			logger.error("Failed to undeploy application config", t);
		}
		try {
			this.server.stop();
		}
		catch (Throwable t) {
			logger.error("Failed to stop server", t);
		}
		try {
			this.wac.close();
		}
		catch (Throwable t) {
			logger.error("Failed to close WebApplicationContext", t);
		}
	}

	protected String getWsBaseUrl() {
		return "ws://localhost:" + this.server.getPort();
	}

	protected ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler clientHandler, String endpointPath) {
		return this.webSocketClient.doHandshake(clientHandler, getWsBaseUrl() + endpointPath);
	}


	static abstract class AbstractRequestUpgradeStrategyConfig {

		@Bean
		public DefaultHandshakeHandler handshakeHandler() {
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

	@Configuration
	static class TomcatUpgradeStrategyConfig extends AbstractRequestUpgradeStrategyConfig {

		@Bean
		public RequestUpgradeStrategy requestUpgradeStrategy() {
			return new TomcatRequestUpgradeStrategy();
		}
	}

	@Configuration
	static class UndertowUpgradeStrategyConfig extends AbstractRequestUpgradeStrategyConfig {

		@Bean
		public RequestUpgradeStrategy requestUpgradeStrategy() {
			return new UndertowRequestUpgradeStrategy();
		}
	}

}
