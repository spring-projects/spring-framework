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

package org.springframework.web.reactive.socket;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.tomcat.websocket.server.WsContextListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.socket.client.ReactorNetty2WebSocketClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.TomcatWebSocketClient;
import org.springframework.web.reactive.socket.client.UndertowWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.JettyRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNetty2RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.TomcatRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.UndertowRequestUpgradeStrategy;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.JettyHttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.ReactorHttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.ReactorNetty2HttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.TomcatHttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.UndertowHttpServer;

/**
 * Base class for reactive WebSocket integration tests. Subclasses must implement
 * {@link #getWebConfigClass()} to return Spring config class with (server-side)
 * handler mappings to {@code WebSocketHandler}'s.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@SuppressWarnings({"unused", "WeakerAccess"})
abstract class AbstractReactiveWebSocketIntegrationTests {

	private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest(name = "[{index}] client[{0}], server[{1}]")
	@MethodSource("arguments")
	@interface ParameterizedWebSocketTest {
	}

	static Stream<Object[]> arguments() throws IOException {

		@SuppressWarnings("removal")
		WebSocketClient[] clients = new WebSocketClient[] {
				new TomcatWebSocketClient(),
				new org.springframework.web.reactive.socket.client.JettyWebSocketClient(),
				new ReactorNettyWebSocketClient(),
				new ReactorNetty2WebSocketClient(),
				new UndertowWebSocketClient(Xnio.getInstance().createWorker(OptionMap.EMPTY))
		};

		Map<HttpServer, Class<?>> servers = new LinkedHashMap<>();
		servers.put(new TomcatHttpServer(TMP_DIR.getAbsolutePath(), WsContextListener.class), TomcatConfig.class);
		servers.put(new JettyHttpServer(), JettyConfig.class);
		servers.put(new ReactorHttpServer(), ReactorNettyConfig.class);
		servers.put(new ReactorNetty2HttpServer(), ReactorNetty2Config.class);
		servers.put(new UndertowHttpServer(), UndertowConfig.class);

		// Try each client once against each server..

		Flux<WebSocketClient> f1 = Flux.fromArray(clients)
				.concatMap(c -> Mono.just(c).repeat(servers.size() - 1));

		Flux<Map.Entry<HttpServer, Class<?>>> f2 = Flux.fromIterable(servers.entrySet())
				.repeat(clients.length - 1)
				.share();

		return Flux.zip(f1, f2.map(Map.Entry::getKey), f2.map(Map.Entry::getValue))
				.map(Tuple3::toArray)
				.toStream();
	}


	protected WebSocketClient client;

	protected HttpServer server;

	protected Class<?> serverConfigClass;

	protected int port;


	protected void startServer(WebSocketClient client, HttpServer server, Class<?> serverConfigClass) throws Exception {
		this.client = client;
		this.server = server;
		this.serverConfigClass = serverConfigClass;

		this.server.setHandler(createHttpHandler());
		this.server.afterPropertiesSet();
		this.server.start();

		// Set dynamically chosen port
		this.port = this.server.getPort();

		if (this.client instanceof Lifecycle lifecycle) {
			lifecycle.start();
		}
	}

	@AfterEach
	void stopServer() {
		if (this.client instanceof Lifecycle lifecycle) {
			lifecycle.stop();
		}
		this.server.stop();
	}


	private HttpHandler createHttpHandler() {
		ApplicationContext context = new AnnotationConfigApplicationContext(
				DispatcherConfig.class, this.serverConfigClass, getWebConfigClass());
		return WebHttpHandlerBuilder.applicationContext(context).build();
	}

	protected URI getUrl(String path) {
		return URI.create("ws://localhost:" + this.port + path);
	}

	protected abstract Class<?> getWebConfigClass();


	@Configuration
	static class DispatcherConfig {

		@Bean
		public WebFilter contextFilter() {
			return new ServerWebExchangeContextFilter();
		}

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
	static class ReactorNetty2Config extends AbstractHandlerAdapterConfig {

		@Override
		protected RequestUpgradeStrategy getUpgradeStrategy() {
			return new ReactorNetty2RequestUpgradeStrategy();
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
