/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * {@code @RequestMapping} integration tests with exception handling scenarios.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
class RequestMappingExceptionHandlingIntegrationTests extends AbstractRequestMappingIntegrationTests {

	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();
		return wac;
	}


	@ParameterizedHttpServerTest
	void thrownException(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		doTest("/thrown-exception", "Recovered from error: State");
	}

	@ParameterizedHttpServerTest
	void thrownExceptionWithCause(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		doTest("/thrown-exception-with-cause", "Recovered from error: State");
	}

	@ParameterizedHttpServerTest
	void thrownExceptionWithCauseToHandle(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		doTest("/thrown-exception-with-cause-to-handle", "Recovered from error: IO");
	}

	@ParameterizedHttpServerTest
	void errorBeforeFirstItem(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		doTest("/mono-error", "Recovered from error: Argument");
	}

	@ParameterizedHttpServerTest  // SPR-16051
	void exceptionAfterSeveralItems(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
				performGet("/SPR-16051", new HttpHeaders(), String.class).getBody())
			.withMessageStartingWith("Error while extracting response");
	}

	@ParameterizedHttpServerTest  // SPR-16318
	void exceptionFromMethodWithProducesCondition(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Accept", "text/plain, application/problem+json");
		assertThatExceptionOfType(HttpStatusCodeException.class).isThrownBy(() ->
				performGet("/SPR-16318", headers, String.class).getBody())
			.satisfies(ex -> {
				assertThat(ex.getRawStatusCode()).isEqualTo(500);
				assertThat(ex.getResponseHeaders().getContentType().toString()).isEqualTo("application/problem+json");
				assertThat(ex.getResponseBodyAsString()).isEqualTo("{\"reason\":\"error\"}");
			});
	}

	private void doTest(String url, String expected) throws Exception {
		assertThat(performGet(url, new HttpHeaders(), String.class).getBody()).isEqualTo(expected);
	}


	@Configuration
	@EnableWebFlux
	@ComponentScan(resourcePattern = "**/RequestMappingExceptionHandlingIntegrationTests$*.class")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig {
	}


	@RestController
	@SuppressWarnings("unused")
	private static class TestController {

		@GetMapping("/thrown-exception")
		public Publisher<String> handleAndThrowException() {
			throw new IllegalStateException("State");
		}

		@GetMapping("/thrown-exception-with-cause")
		public Publisher<String> handleAndThrowExceptionWithCause() {
			throw new IllegalStateException("State", new IOException("IO"));
		}

		@GetMapping("/thrown-exception-with-cause-to-handle")
		public Publisher<String> handleAndThrowExceptionWithCauseToHandle() {
			throw new RuntimeException("State", new IOException("IO"));
		}

		@GetMapping(path = "/mono-error")
		public Publisher<String> handleWithError() {
			return Mono.error(new IllegalArgumentException("Argument"));
		}

		@GetMapping("/SPR-16051")
		public Flux<String> errors() {
			return Flux.range(1, 10000)
					.map(i -> {
						if (i == 1000) {
							throw new RuntimeException("Random error");
						}
						return i + ". foo bar";
					});
		}

		@GetMapping(path = "/SPR-16318", produces = "text/plain")
		public Mono<String> handleTextPlain() throws Exception {
			return Mono.error(new Spr16318Exception());
		}

		@ExceptionHandler
		public Publisher<String> handleArgumentException(IOException ex) {
			return Mono.just("Recovered from error: " + ex.getMessage());
		}

		@ExceptionHandler
		public Publisher<String> handleArgumentException(IllegalArgumentException ex) {
			return Mono.just("Recovered from error: " + ex.getMessage());
		}

		@ExceptionHandler
		public ResponseEntity<Publisher<String>> handleStateException(IllegalStateException ex) {
			return ResponseEntity.ok(Mono.just("Recovered from error: " + ex.getMessage()));
		}

		@ExceptionHandler
		public ResponseEntity<Map<String, String>> handle(Spr16318Exception ex) {
			return ResponseEntity.status(500).body(Collections.singletonMap("reason", "error"));
		}
	}


	@SuppressWarnings("serial")
	private static class Spr16318Exception extends Exception {
	}

}
