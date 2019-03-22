/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.Assert.*;

/**
 * Integration tests for server response flushing behavior.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FlushingIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private WebClient webClient;


	@Before
	public void setup() throws Exception {
		super.setup();
		this.webClient = WebClient.create("http://localhost:" + this.port);
	}


	@Test
	public void writeAndFlushWith() {
		Mono<String> result = this.webClient.get()
				.uri("/write-and-flush")
				.retrieve()
				.bodyToFlux(String.class)
				.takeUntil(s -> s.endsWith("data1"))
				.reduce((s1, s2) -> s1 + s2);

		StepVerifier.create(result)
				.expectNext("data0data1")
				.expectComplete()
				.verify(Duration.ofSeconds(10L));
	}

	@Test  // SPR-14991
	public void writeAndAutoFlushOnComplete() {
		Mono<String> result = this.webClient.get()
				.uri("/write-and-complete")
				.retrieve()
				.bodyToMono(String.class);

		try {
			StepVerifier.create(result)
					.consumeNextWith(value -> assertEquals(64 * 1024, value.length()))
					.expectComplete()
					.verify(Duration.ofSeconds(10L));
		}
		catch (AssertionError err) {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("windows") && err.getMessage() != null &&
					err.getMessage().startsWith("VerifySubscriber timed out")) {
				// TODO: Reactor usually times out on Windows ...
				err.printStackTrace();
				return;
			}
			throw err;
		}
	}

	@Test  // SPR-14992
	public void writeAndAutoFlushBeforeComplete() {
		Mono<String> result = this.webClient.get()
				.uri("/write-and-never-complete")
				.retrieve()
				.bodyToFlux(String.class)
				.next();

		StepVerifier.create(result)
				.expectNextMatches(s -> s.startsWith("0123456789"))
				.expectComplete()
				.verify(Duration.ofSeconds(10L));
	}


	@Override
	protected HttpHandler createHttpHandler() {
		return new FlushingHandler();
	}


	private static class FlushingHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			String path = request.getURI().getPath();
			switch (path) {
				case "/write-and-flush":
					return response.writeAndFlushWith(
							testInterval(Duration.ofMillis(50), 2)
									.map(longValue -> wrap("data" + longValue + "\n", response))
									.map(Flux::just)
									.mergeWith(Flux.never()));

				case "/write-and-complete":
					return response.writeWith(
							chunks1K().take(64).map(s -> wrap(s, response)));

				case "/write-and-never-complete":
					// Reactor requires at least 50 to flush, Tomcat/Undertow 8, Jetty 1
					return response.writeWith(
							chunks1K().take(64).map(s -> wrap(s, response)).mergeWith(Flux.never()));

				default:
					return response.writeWith(Flux.empty());
			}
		}

		private Flux<String> chunks1K() {
			return Flux.generate(sink -> {
				StringBuilder sb = new StringBuilder();
				do {
					for (char c : "0123456789".toCharArray()) {
						sb.append(c);
						if (sb.length() + 1 == 1024) {
							sink.next(sb.append("\n").toString());
							return;
						}
					}
				} while (true);
			});
		}

		private DataBuffer wrap(String value, ServerHttpResponse response) {
			byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
			return response.bufferFactory().wrap(bytes);
		}
	}

}
