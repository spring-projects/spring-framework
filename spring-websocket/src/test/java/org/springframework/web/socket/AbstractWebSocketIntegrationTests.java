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

package org.springframework.web.socket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.UndertowRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Base class for WebSocket integration tests.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public abstract class AbstractWebSocketIntegrationTests {

	private static Map<Class<?>, Class<?>> upgradeStrategyConfigTypes = Map.of(
			JettyWebSocketTestServer.class, JettyUpgradeStrategyConfig.class, //
			TomcatWebSocketTestServer.class, TomcatUpgradeStrategyConfig.class, //
			UndertowTestServer.class, UndertowUpgradeStrategyConfig.class);

	@SuppressWarnings("removal")
	static Stream<Arguments> argumentsFactory() {
		return Stream.of(
				arguments(named("Jetty", new JettyWebSocketTestServer()), named("Jetty", new org.springframework.web.socket.client.jetty.JettyWebSocketClient())),
				arguments(named("Tomcat", new TomcatWebSocketTestServer()), named("Standard", new StandardWebSocketClient())),
				arguments(named("Undertow", new UndertowTestServer()), named("Standard", new StandardWebSocketClient())));
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest(name = "[{index}] server = {0}, client = {1}")
	@MethodSource("argumentsFactory")
	protected @interface ParameterizedWebSocketTest {
	}


	protected final Log logger = LogFactory.getLog(getClass());

	protected WebSocketTestServer server;

	protected WebSocketClient webSocketClient;

	protected AnnotationConfigWebApplicationContext wac;


	protected void setup(WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
		this.server = server;
		this.webSocketClient = webSocketClient;

		logger.debug("Setting up '" + testInfo.getTestMethod().get().getName() + "', client=" +
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
		this.server.start();

		this.wac.setServletContext(this.server.getServletContext());
		this.wac.refresh();
	}

	protected abstract Class<?>[] getAnnotatedConfigClasses();

	@AfterEach
	void teardown() throws Exception {
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

	protected CompletableFuture<WebSocketSession> execute(WebSocketHandler clientHandler, String endpointPath) {
		return this.webSocketClient.execute(clientHandler, getWsBaseUrl() + endpointPath);
	}


	static abstract class AbstractRequestUpgradeStrategyConfig {

		@Bean
		public DefaultHandshakeHandler handshakeHandler() {
			return new DefaultHandshakeHandler(requestUpgradeStrategy());
		}

		public abstract RequestUpgradeStrategy requestUpgradeStrategy();
	}


	@Configuration(proxyBeanMethods = false)
	static class JettyUpgradeStrategyConfig extends AbstractRequestUpgradeStrategyConfig {

		@Override
		@Bean
		public RequestUpgradeStrategy requestUpgradeStrategy() {
			return new JettyRequestUpgradeStrategy();
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class TomcatUpgradeStrategyConfig extends AbstractRequestUpgradeStrategyConfig {

		@Override
		@Bean
		public RequestUpgradeStrategy requestUpgradeStrategy() {
			return new TomcatRequestUpgradeStrategy();
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class UndertowUpgradeStrategyConfig extends AbstractRequestUpgradeStrategyConfig {

		@Override
		@Bean
		public RequestUpgradeStrategy requestUpgradeStrategy() {
			return new UndertowRequestUpgradeStrategy();
		}
	}

}
