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

package org.springframework.web.reactive.result.method.annotation;

import org.junit.Test;
import org.reactivestreams.Publisher;
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
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.junit.Assert.assertEquals;


/**
 * {@code @RequestMapping} integration tests with exception handling scenarios.
 *
 * @author Rossen Stoyanchev
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
	public void controllerThrowingException() throws Exception {
		String expected = "Recovered from error: State";
		assertEquals(expected, performGet("/thrown-exception", new HttpHeaders(), String.class).getBody());
	}

	@Test
	public void controllerReturnsMonoError() throws Exception {
		String expected = "Recovered from error: Argument";
		assertEquals(expected, performGet("/mono-error", new HttpHeaders(), String.class).getBody());
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

		@GetMapping("/mono-error")
		public Publisher<String> handleWithError() {
			return Mono.error(new IllegalArgumentException("Argument"));
		}

		@ExceptionHandler
		public Publisher<String> handleArgumentException(IllegalArgumentException ex) {
			return Mono.just("Recovered from error: " + ex.getMessage());
		}

		@ExceptionHandler
		public ResponseEntity<Publisher<String>> handleStateException(IllegalStateException ex) {
			return ResponseEntity.ok(Mono.just("Recovered from error: " + ex.getMessage()));
		}

	}

}
