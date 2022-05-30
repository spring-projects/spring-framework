/*
 * Copyright 2002-2020 the original author or authors.
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

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.server.WebFilter;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.TomcatHttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests with server-side {@link WebSocketHandler}s.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 */
class WebSocketIntegrationTests extends AbstractWebSocketIntegrationTests {

	private static final Log logger = LogFactory.getLog(WebSocketIntegrationTests.class);

	private static final Duration TIMEOUT = Duration.ofMillis(5000);


	@Override
	protected Class<?> getWebConfigClass() {
		return WebConfig.class;
	}


	@ParameterizedWebSocketTest
	void echo(WebSocketClient client, HttpServer server, Class<?> serverConfigClass) throws Exception {
		startServer(client, server, serverConfigClass);

		if (server instanceof TomcatHttpServer) {
			Mono.fromRunnable(this::testEcho)
					.retryWhen(Retry.max(3).filter(ex -> ex instanceof IllegalStateException))
					.block();
		}
		else {
			testEcho();
		}
	}

	private void testEcho() {
		int count = 100;
		Flux<String> input = Flux.range(1, count).map(index -> "msg-" + index);
		AtomicReference<List<String>> actualRef = new AtomicReference<>();
		this.client.execute(getUrl("/echo"), session ->
				session.send(input.map(session::textMessage))
						.thenMany(session.receive().take(count).map(WebSocketMessage::getPayloadAsText))
						.collectList()
						.doOnNext(actualRef::set)
						.then())
				.block(TIMEOUT);
		assertThat(actualRef.get()).isNotNull();
		assertThat(actualRef.get()).isEqualTo(input.collectList().block());
	}

	@ParameterizedWebSocketTest
	void subProtocol(WebSocketClient client, HttpServer server, Class<?> serverConfigClass) throws Exception {
		startServer(client, server, serverConfigClass);

		String protocol = "echo-v1";
		AtomicReference<HandshakeInfo> infoRef = new AtomicReference<>();
		AtomicReference<Object> protocolRef = new AtomicReference<>();

		this.client.execute(getUrl("/sub-protocol"),
				new WebSocketHandler() {
					@Override
					public List<String> getSubProtocols() {
						return Collections.singletonList(protocol);
					}

					@Override
					public Mono<Void> handle(WebSocketSession session) {
						infoRef.set(session.getHandshakeInfo());
						return session.receive()
								.map(WebSocketMessage::getPayloadAsText)
								.doOnNext(protocolRef::set)
								.doOnError(protocolRef::set)
								.then();
					}
				})
				.block(TIMEOUT);

		HandshakeInfo info = infoRef.get();
		assertThat(info.getHeaders().getFirst("Upgrade")).isEqualToIgnoringCase("websocket");
		assertThat(info.getHeaders().getFirst("Sec-WebSocket-Protocol")).isEqualTo(protocol);
		assertThat(info.getSubProtocol()).as("Wrong protocol accepted").isEqualTo(protocol);
		assertThat(protocolRef.get()).as("Wrong protocol detected on the server side").isEqualTo(protocol);
	}

	@ParameterizedWebSocketTest
	void customHeader(WebSocketClient client, HttpServer server, Class<?> serverConfigClass) throws Exception {
		startServer(client, server, serverConfigClass);

		HttpHeaders headers = new HttpHeaders();
		headers.add("my-header", "my-value");
		AtomicReference<Object> headerRef = new AtomicReference<>();

		this.client.execute(getUrl("/custom-header"), headers,
				session -> session.receive()
						.map(WebSocketMessage::getPayloadAsText)
						.doOnNext(headerRef::set)
						.doOnError(headerRef::set)
						.then())
				.block(TIMEOUT);

		assertThat(headerRef.get()).isEqualTo("my-header:my-value");
	}

	@ParameterizedWebSocketTest
	void sessionClosing(WebSocketClient client, HttpServer server, Class<?> serverConfigClass) throws Exception {
		startServer(client, server, serverConfigClass);

		AtomicReference<Object> statusRef = new AtomicReference<>();
		this.client.execute(getUrl("/close"),
				session -> {
					logger.debug("Starting..");
					session.closeStatus().subscribe(statusRef::set, statusRef::set, () -> {});
					return session.receive()
							.doOnNext(s -> logger.debug("inbound " + s))
							.then()
							.doFinally(signalType ->
									logger.debug("Completed with: " + signalType)
							);
				})
				.block(TIMEOUT);

		assertThat(statusRef.get()).isEqualTo(CloseStatus.GOING_AWAY);
	}

	@ParameterizedWebSocketTest
	void cookie(WebSocketClient client, HttpServer server, Class<?> serverConfigClass) throws Exception {
		startServer(client, server, serverConfigClass);

		AtomicReference<String> cookie = new AtomicReference<>();
		AtomicReference<Object> receivedCookieRef = new AtomicReference<>();
		this.client.execute(getUrl("/cookie"),
				session -> {
					cookie.set(session.getHandshakeInfo().getHeaders().getFirst("Set-Cookie"));
					return session.receive()
							.map(WebSocketMessage::getPayloadAsText)
							.doOnNext(receivedCookieRef::set)
							.doOnError(receivedCookieRef::set)
							.then();
				})
				.block(TIMEOUT);
		assertThat(receivedCookieRef.get()).isEqualTo("cookie");
		assertThat(cookie.get()).isEqualTo("project=spring");
	}


	@Configuration
	static class WebConfig {

		@Bean
		public HandlerMapping handlerMapping() {
			Map<String, WebSocketHandler> map = new HashMap<>();
			map.put("/echo", new EchoWebSocketHandler());
			map.put("/sub-protocol", new SubProtocolWebSocketHandler());
			map.put("/custom-header", new CustomHeaderHandler());
			map.put("/close", new SessionClosingHandler());
			map.put("/cookie", new CookieHandler());
			return new SimpleUrlHandlerMapping(map);
		}

		@Bean
		public WebFilter cookieWebFilter() {
			return (exchange, chain) -> {
				if (exchange.getRequest().getPath().value().startsWith("/cookie")) {
					exchange.getResponse().addCookie(ResponseCookie.from("project", "spring").build());
				}
				return chain.filter(exchange);
			};
		}
	}


	private static class EchoWebSocketHandler implements WebSocketHandler {

		@Override
		public Mono<Void> handle(WebSocketSession session) {
			return Mono.deferContextual(contextView -> {
				String key = ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_ATTRIBUTE;
				assertThat(contextView.getOrEmpty(key).orElse(null)).isNotNull();
				return session.send(session.receive().doOnNext(WebSocketMessage::retain));
			});
		}
	}


	private static class SubProtocolWebSocketHandler implements WebSocketHandler {

		@Override
		public List<String> getSubProtocols() {
			return Collections.singletonList("echo-v1");
		}

		@Override
		public Mono<Void> handle(WebSocketSession session) {
			String protocol = session.getHandshakeInfo().getSubProtocol();
			WebSocketMessage message = session.textMessage(protocol != null ? protocol : "none");
			return session.send(Mono.just(message));
		}
	}


	private static class CustomHeaderHandler implements WebSocketHandler {

		@Override
		public Mono<Void> handle(WebSocketSession session) {
			HttpHeaders headers = session.getHandshakeInfo().getHeaders();
			String payload = "my-header:" + headers.getFirst("my-header");
			WebSocketMessage message = session.textMessage(payload);
			return session.send(Mono.just(message));
		}
	}


	private static class SessionClosingHandler implements WebSocketHandler {

		@Override
		public Mono<Void> handle(WebSocketSession session) {
			return session.send(Flux
					.error(new Throwable())
					.onErrorResume(ex -> session.close(CloseStatus.GOING_AWAY)) // SPR-17306 (nested close)
					.cast(WebSocketMessage.class));
		}
	}

	private static class CookieHandler implements WebSocketHandler {

		@Override
		public Mono<Void> handle(WebSocketSession session) {
			WebSocketMessage message = session.textMessage("cookie");
			return session.send(Mono.just(message));
		}
	}

}
