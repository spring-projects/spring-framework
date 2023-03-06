/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.service.annotation;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.web.bind.annotation.RequestBody;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;

/**
 * Tests for {@link HttpExchangeReflectiveProcessor}.
 *
 * @author Sebastien Deleuze
 */
class HttpExchangeReflectiveProcessorTests {

	private final HttpExchangeReflectiveProcessor processor = new HttpExchangeReflectiveProcessor();

	private final RuntimeHints hints = new RuntimeHints();


	@Test
	void registerReflectiveHintsForMethodWithReturnValue() throws NoSuchMethodException {
		Method method = SampleService.class.getDeclaredMethod("get");
		processor.registerReflectionHints(hints.reflection(), method);
		assertThat(reflection().onType(SampleService.class)).accepts(hints);
		assertThat(reflection().onMethod(SampleService.class, "get")).accepts(hints);
		assertThat(reflection().onType(Response.class)).accepts(hints);
		assertThat(reflection().onMethod(Response.class, "getMessage")).accepts(hints);
		assertThat(reflection().onMethod(Response.class, "setMessage")).accepts(hints);
	}

	@Test
	void registerReflectiveHintsForMethodWithRequestBodyParameter() throws NoSuchMethodException {
		Method method = SampleService.class.getDeclaredMethod("post", Request.class);
		processor.registerReflectionHints(hints.reflection(), method);
		assertThat(reflection().onType(SampleService.class)).accepts(hints);
		assertThat(reflection().onMethod(SampleService.class, "post")).accepts(hints);
		assertThat(reflection().onType(Request.class)).accepts(hints);
		assertThat(reflection().onMethod(Request.class, "getMessage")).accepts(hints);
		assertThat(reflection().onMethod(Request.class, "setMessage")).accepts(hints);
	}


	interface SampleService {

		@GetExchange
		Response get();

		@PostExchange
		void post(@RequestBody Request request);
	}

	static class Request {

		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	static class Response {

		private String message;

		public Response(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

}
