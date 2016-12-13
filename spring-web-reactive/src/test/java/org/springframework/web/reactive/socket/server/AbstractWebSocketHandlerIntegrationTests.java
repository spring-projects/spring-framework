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

import java.io.File;

import org.apache.tomcat.websocket.server.WsContextListener;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.http.server.reactive.bootstrap.ReactorHttpServer;
import org.springframework.http.server.reactive.bootstrap.JettyHttpServer;
import org.springframework.http.server.reactive.bootstrap.RxNettyHttpServer;
import org.springframework.http.server.reactive.bootstrap.TomcatHttpServer;
import org.springframework.http.server.reactive.bootstrap.UndertowHttpServer;
import org.springframework.util.SocketUtils;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.JettyRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.RxNettyRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.TomcatRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.UndertowRequestUpgradeStrategy;

/**
 * Base class for WebSocket integration tests involving a server-side
 * {@code WebSocketHandler}. Sub-classes to return a Spring configuration class
 * via {@link #getWebConfigClass()} containing a SimpleUrlHandlerMapping with
 * pattern-to-WebSocketHandler mappings.
 *
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class AbstractWebSocketHandlerIntegrationTests {

	protected int port;

	@Parameter(0)
	public HttpServer server;

	@Parameter(1)
	public Class<?> handlerAdapterConfigClass;


	@Parameters(name = "server [{0}]")
	public static Object[][] arguments() {
		File base = new File(System.getProperty("java.io.tmpdir"));
		return new Object[][] {
				{new ReactorHttpServer(), ReactorNettyConfig.class},
				{new RxNettyHttpServer(), RxNettyConfig.class},
				{new TomcatHttpServer(base.getAbsolutePath(), WsContextListener.class), TomcatConfig.class},
				{new UndertowHttpServer(), UndertowConfig.class},
				{new JettyHttpServer(), JettyConfig.class}
		};
	}


	@Before
	public void setup() throws Exception {
		this.port = SocketUtils.findAvailableTcpPort();
		this.server.setPort(this.port);
		this.server.setHandler(createHttpHandler());
		this.server.afterPropertiesSet();
		this.server.start();
	}

	private HttpHandler createHttpHandler() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(DispatcherConfig.class, this.handlerAdapterConfigClass);
		context.register(getWebConfigClass());
		context.refresh();
		return DispatcherHandler.toHttpHandler(context);
	}

	protected abstract Class<?> getWebConfigClass();

	@After
	public void tearDown() throws Exception {
		this.server.stop();
	}


	@Configuration
	static class DispatcherConfig {

		@Bean
		public DispatcherHandler webHandler() {
			return new DispatcherHandler();
		}
	}

	static abstract class AbstractHandlerAdapterConfig {

		@Bean
		public WebSocketHandlerAdapter handlerAdapter() {
			return new WebSocketHandlerAdapter(webSocketService());
		}

		@Bean
		public WebSocketService webSocketService() {
			return new HandshakeWebSocketService(getUpgradeStrategy());
		}

		protected abstract RequestUpgradeStrategy getUpgradeStrategy();

	}

	@Configuration
	static class ReactorNettyConfig extends AbstractHandlerAdapterConfig {

		@Override
		protected RequestUpgradeStrategy getUpgradeStrategy() {
			return new ReactorNettyRequestUpgradeStrategy();
		}
	}

	@Configuration
	static class RxNettyConfig extends AbstractHandlerAdapterConfig {

		@Override
		protected RequestUpgradeStrategy getUpgradeStrategy() {
			return new RxNettyRequestUpgradeStrategy();
		}
	}

	@Configuration
	static class TomcatConfig extends AbstractHandlerAdapterConfig {

		@Override
		protected RequestUpgradeStrategy getUpgradeStrategy() {
			return new TomcatRequestUpgradeStrategy();
		}
	}

	@Configuration
	static class UndertowConfig extends AbstractHandlerAdapterConfig {

		@Override
		protected RequestUpgradeStrategy getUpgradeStrategy() {
			return new UndertowRequestUpgradeStrategy();
		}
	}

	@Configuration
	static class JettyConfig extends AbstractHandlerAdapterConfig {

		@Override
		protected RequestUpgradeStrategy getUpgradeStrategy() {
			return new JettyRequestUpgradeStrategy();
		}
	}

}
