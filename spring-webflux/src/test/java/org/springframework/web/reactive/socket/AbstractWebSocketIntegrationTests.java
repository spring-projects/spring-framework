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
import org.springframework.http.server.reactive.bootstrap.RxNettyHttpServer;
import org.springframework.http.server.reactive.bootstrap.TomcatHttpServer;
import org.springframework.http.server.reactive.bootstrap.UndertowHttpServer;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.socket.client.JettyWebSocketClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.RxNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.StandardWebSocketClient;
import org.springframework.web.reactive.socket.client.UndertowWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.JettyRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.RxNettyRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.TomcatRequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.UndertowRequestUpgradeStrategy;

import static org.junit.Assume.*;

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

		Flux<? extends WebSocketClient> clients = Flux.concat(
				Flux.just(new StandardWebSocketClient()).repeat(5),
				Flux.just(new JettyWebSocketClient()).repeat(5),
				Flux.just(new ReactorNettyWebSocketClient()).repeat(5),
				Flux.just(new RxNettyWebSocketClient()).repeat(5),
				Flux.just(new UndertowWebSocketClient(Xnio.getInstance().createWorker(OptionMap.EMPTY))).repeat(5));

		Flux<? extends HttpServer> servers = Flux.just(
				new TomcatHttpServer(TMP_DIR.getAbsolutePath(), WsContextListener.class),
				new JettyHttpServer(),
				new ReactorHttpServer(),
				new RxNettyHttpServer(),
				new UndertowHttpServer()).repeat(5);

		Flux<? extends Class<?>> configs = Flux.just(
				TomcatConfig.class,
				JettyConfig.class,
				ReactorNettyConfig.class,
				RxNettyConfig.class,
				UndertowConfig.class).repeat(5);

		return Flux.zip(clients, servers, configs)
				.map(Tuple3::toArray)
				.collectList()
				.block()
				.toArray(new Object[25][2]);
	}


	@Before
	public void setup() throws Exception {
		// TODO
		// Caused by: java.io.IOException: Upgrade responses cannot have a transfer coding
		// at org.xnio.http.HttpUpgrade$HttpUpgradeState.handleUpgrade(HttpUpgrade.java:490)
		// at org.xnio.http.HttpUpgrade$HttpUpgradeState.access$1200(HttpUpgrade.java:165)
		// at org.xnio.http.HttpUpgrade$HttpUpgradeState$UpgradeResultListener.handleEvent(HttpUpgrade.java:461)
		// at org.xnio.http.HttpUpgrade$HttpUpgradeState$UpgradeResultListener.handleEvent(HttpUpgrade.java:400)
		// at org.xnio.ChannelListeners.invokeChannelListener(ChannelListeners.java:92)

		assumeFalse(this.client instanceof UndertowWebSocketClient && this.server instanceof RxNettyHttpServer);

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
		return DispatcherHandler.toHttpHandler(context);
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
