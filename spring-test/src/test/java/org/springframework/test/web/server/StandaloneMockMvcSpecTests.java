/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.server;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tests for {@link StandaloneMockMvcSpec}.
 *
 * @author Rob Worsnop
 */
public class StandaloneMockMvcSpecTests {

	@Test
	public void controller() {
		new StandaloneMockMvcSpec(new MyController()).build()
				.get().uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Success");

		new StandaloneMockMvcSpec(new MyController()).build()
				.get().uri("")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Success");
	}

	@Test
	public void controllerAdvice() {
		new StandaloneMockMvcSpec(new MyController())
				.controllerAdvice(new MyControllerAdvice())
				.build()
				.get().uri("/exception")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(String.class).isEqualTo("Handled exception");
	}

	@Test
	public void controllerAdviceWithClassArgument() {
		new StandaloneMockMvcSpec(MyController.class)
				.controllerAdvice(MyControllerAdvice.class)
				.build()
				.get().uri("/exception")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(String.class).isEqualTo("Handled exception");
	}

	@SuppressWarnings("unused")
	@RestController
	private static class MyController {

		@GetMapping
		public String handleRootPath() {
			return "Success";
		}

		@GetMapping("/exception")
		public void handleWithError() {
			throw new IllegalStateException();
		}

	}


	@ControllerAdvice
	private static class MyControllerAdvice {

		@ExceptionHandler
		public ResponseEntity<String> handle(IllegalStateException ex) {
			return ResponseEntity.status(400).body("Handled exception");
		}
	}


	private static class TestConsumer<T> implements Consumer<T> {

		private T value;

		public T getValue() {
			return this.value;
		}

		@Override
		public void accept(T t) {
			this.value = t;
		}
	}

}
