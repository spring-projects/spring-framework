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

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.Matchers;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.ReplayProcessor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Integration tests with server-side {@link WebSocketHandler}s.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketIntegrationTests extends AbstractWebSocketIntegrationTests {

	private static final Log logger = LogFactory.getLog(WebSocketIntegrationTests.class);


	@Override
	protected Class<?> getWebConfigClass() {
		return WebConfig.class;
	}


	@Test
	public void echo() throws Exception {
		int count = 100;
		Flux<String> input = Flux.range(1, count).map(index -> "msg-" + index);
		ReplayProcessor<Object> output = ReplayProcessor.create(count);

		this.client.execute(getUrl("/echo"),
				session -> {
					logger.debug("Starting to send messages");
					return session
							.send(input.doOnNext(s -> logger.debug("outbound " + s)).map(session::textMessage))
							.thenMany(session.receive().take(count).map(WebSocketMessage::getPayloadAsText))
							.subscribeWith(output)
							.doOnNext(s -> logger.debug("inbound " + s))
							.then()
							.doOnSuccessOrError((aVoid, ex) ->
									logger.debug("Done with " + (ex != null ? ex.getMessage() : "success")));
				})
				.block(Duration.ofMillis(5000));

		assertEquals(input.collectList().block(Duration.ofMillis(5000)),
				output.collectList().block(Duration.ofMillis(5000)));
	}

	@Test
	public void subProtocol() throws Exception {
		String protocol = "echo-v1";
		AtomicReference<HandshakeInfo> infoRef = new AtomicReference<>();
		MonoProcessor<Object> output = MonoProcessor.create();

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
								.subscribeWith(output)
								.then();
					}
				})
				.block(Duration.ofMillis(5000));

		HandshakeInfo info = infoRef.get();
		assertThat(info.getHeaders().getFirst("Upgrade"), Matchers.equalToIgnoringCase("websocket"));
		assertEquals(protocol, info.getHeaders().getFirst("Sec-WebSocket-Protocol"));
		assertEquals("Wrong protocol accepted", protocol, info.getSubProtocol());
		assertEquals("Wrong protocol detected on the server side", protocol, output.block(Duration.ofMillis(5000)));
	}

	@Test
	public void customHeader() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("my-header", "my-value");
		MonoProcessor<Object> output = MonoProcessor.create();

		this.client.execute(getUrl("/custom-header"), headers,
				session -> session.receive()
						.map(WebSocketMessage::getPayloadAsText)
						.subscribeWith(output)
						.then())
				.block(Duration.ofMillis(5000));

		assertEquals("my-header:my-value", output.block(Duration.ofMillis(5000)));
	}


	@Configuration
	static class WebConfig {

		@Bean
		public HandlerMapping handlerMapping() {
			Map<String, WebSocketHandler> map = new HashMap<>();
			map.put("/echo", new EchoWebSocketHandler());
			map.put("/sub-protocol", new SubProtocolWebSocketHandler());
			map.put("/custom-header", new CustomHeaderHandler());

			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			mapping.setUrlMap(map);
			return mapping;
		}

	}


	private static class EchoWebSocketHandler implements WebSocketHandler {

		@Override
		public Mono<Void> handle(WebSocketSession session) {
			// Use retain() for Reactor Netty
			return session.send(session.receive().doOnNext(WebSocketMessage::retain));
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
			WebSocketMessage message = session.textMessage(protocol);
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

}
