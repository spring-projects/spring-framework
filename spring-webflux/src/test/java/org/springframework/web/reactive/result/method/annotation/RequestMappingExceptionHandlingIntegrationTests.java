/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.junit.Assert.*;

/**
 * {@code @RequestMapping} integration tests with exception handling scenarios.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class RequestMappingExceptionHandlingIntegrationTests extends AbstractRequestMappingIntegrationTests {

	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();
		return wac;
	}


	@Test
	public void thrownException() throws Exception {
		doTest("/thrown-exception", "Recovered from error: State");
	}

	@Test
	public void thrownExceptionWithCause() throws Exception {
		doTest("/thrown-exception-with-cause", "Recovered from error: State");
	}

	@Test
	public void thrownExceptionWithCauseToHandle() throws Exception {
		doTest("/thrown-exception-with-cause-to-handle", "Recovered from error: IO");
	}

	@Test
	public void errorBeforeFirstItem() throws Exception {
		doTest("/mono-error", "Recovered from error: Argument");
	}

	@Test  // SPR-16051
	public void exceptionAfterSeveralItems() {
		try {
			performGet("/SPR-16051", new HttpHeaders(), String.class).getBody();
			fail();
		}
		catch (Throwable ex) {
			String message = ex.getMessage();
			assertNotNull(message);
			assertTrue("Actual: " + message, message.startsWith("Error while extracting response"));
		}
	}

	@Test  // SPR-16318
	public void exceptionFromMethodWithProducesCondition() throws Exception {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Accept", "text/plain, application/problem+json");
			performGet("/SPR-16318", headers, String.class).getBody();
			fail();
		}
		catch (HttpStatusCodeException ex) {
			assertEquals(500, ex.getRawStatusCode());
			assertEquals("application/problem+json;charset=UTF-8", ex.getResponseHeaders().getContentType().toString());
			assertEquals("{\"reason\":\"error\"}", ex.getResponseBodyAsString());
		}
	}

	private void doTest(String url, String expected) throws Exception {
		assertEquals(expected, performGet(url, new HttpHeaders(), String.class).getBody());
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
