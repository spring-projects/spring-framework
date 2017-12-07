/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.socket;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.apache.tomcat.websocket.server.WsContextListener;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple3;

import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.http.server.reactive.bootstrap.JettyHttpServer;
import org.springframework.http.server.reactive.bootstrap.ReactorHttpServer;
import org.springframework.http.server.reactive.bootstrap.TomcatHttpServer;
import org.springframework.http.server.reactive.bootstrap.UndertowHttpServer;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.socket.client.JettyWebSocketClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.TomcatWebSocketClient;
import org.springframework.web.reactive.socket.client.UndertowWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.JettyRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.TomcatRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.UndertowRequestUpgradeStrategy;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Base class for WebSocket integration tests. Sub-classes must implement
 * {@link #getWebConfigClass()} to return Spring config class with (server-side)
 * handler mappings to {@code WebSocketHandler}'s.
 *
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class AbstractWebSocketIntegrationTests {

	private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));


	protected int port;

	@Parameter(0)
	public WebSocketClient client;

	@Parameter(1)
	public HttpServer server;

	@Parameter(2)
	public Class<?> serverConfigClass;


	@Parameters(name = "client[{0}] - server [{1}]")
	public static Object[][] arguments() throws IOException {

		WebSocketClient[] clients = new WebSocketClient[] {
				new TomcatWebSocketClient(),
				new JettyWebSocketClient(),
				new ReactorNettyWebSocketClient(),
				new UndertowWebSocketClient(Xnio.getInstance().createWorker(OptionMap.EMPTY))
		};

		Map<HttpServer, Class<?>> servers = new LinkedHashMap<>();
		servers.put(new TomcatHttpServer(TMP_DIR.getAbsolutePath(), WsContextListener.class), TomcatConfig.class);
		servers.put(new JettyHttpServer(), JettyConfig.class);
		servers.put(new ReactorHttpServer(), ReactorNettyConfig.class);
		servers.put(new UndertowHttpServer(), UndertowConfig.class);

		Flux<WebSocketClient> f1 = Flux.fromArray(clients).concatMap(c -> Flux.just(c).repeat(servers.size()));
		Flux<HttpServer> f2 = Flux.fromIterable(servers.keySet()).repeat(clients.length);
		Flux<Class<?>> f3 = Flux.fromIterable(servers.values()).repeat(clients.length);

		return Flux.zip(f1, f2, f3).map(Tuple3::toArray).collectList().block()
				.toArray(new Object[clients.length * servers.size()][2]);
	}


	@Before
	public void setup() throws Exception {

		this.server.setHandler(createHttpHandler());
		this.server.afterPropertiesSet();
		this.server.start();

		// Set dynamically chosen port
		this.port = this.server.getPort();

		if (this.client instanceof Lifecycle) {
			((Lifecycle) this.client).start();
		}
	}

	@After
	public void stop() throws Exception {
		if (this.client instanceof Lifecycle) {
			((Lifecycle) this.client).stop();
		}
		this.server.stop();
	}


	private HttpHandler createHttpHandler() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(DispatcherConfig.class, this.serverConfigClass);
		context.register(getWebConfigClass());
		context.refresh();
		return WebHttpHandlerBuilder.applicationContext(context).build();
	}

	protected URI getUrl(String path) throws URISyntaxException {
		return new URI("ws://localhost:" + this.port + path);
	}

	protected abstract Class<?> getWebConfigClass();


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
