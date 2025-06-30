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

package org.springframework.web.socket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import jakarta.servlet.Filter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.context.Lifecycle;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.StandardWebSocketUpgradeStrategy;
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

	static Stream<Arguments> argumentsFactory() {
		return Stream.of(
				arguments(named("Jetty", new JettyWebSocketTestServer()), named("Standard", new StandardWebSocketClient())),
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


	protected void setup(WebSocketTestServer server, WebSocketClient client, TestInfo info) throws Exception {
		setup(server, null, client, info);
	}

	protected void setup(
			WebSocketTestServer server, @Nullable Filter filter, WebSocketClient client, TestInfo info)
			throws Exception {

		this.server = server;
		this.webSocketClient = client;

		logger.debug("Setting up '" + info.getTestMethod().get().getName() + "', client=" +
				this.webSocketClient.getClass().getSimpleName() + ", server=" +
				this.server.getClass().getSimpleName());

		this.wac = new AnnotationConfigWebApplicationContext();
		this.wac.register(getAnnotatedConfigClasses());
		this.wac.register(this.server instanceof JettyWebSocketTestServer ? JettyHandshakeHandler.class :
				StandardHandshakeHandler.class);

		if (this.webSocketClient instanceof Lifecycle) {
			((Lifecycle) this.webSocketClient).start();
		}

		this.server.setup();
		if (filter != null) {
			this.server.deployConfig(this.wac, filter);
		}
		else {
			this.server.deployConfig(this.wac);
		}
		this.server.start();

		this.wac.setServletContext(this.server.getServletContext());
		this.wac.refresh();
	}

	protected abstract Class<?>[] getAnnotatedConfigClasses();

	@AfterEach
	void teardown() {
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


	static class JettyHandshakeHandler extends DefaultHandshakeHandler {

		public JettyHandshakeHandler() {
			super(new JettyRequestUpgradeStrategy());
		}
	}


	static class StandardHandshakeHandler extends DefaultHandshakeHandler {

		public StandardHandshakeHandler() {
			super(new StandardWebSocketUpgradeStrategy());
		}
	}

}
