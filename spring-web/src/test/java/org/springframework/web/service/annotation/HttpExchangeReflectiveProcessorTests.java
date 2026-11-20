/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		assertThat(reflection().onMethodInvocation(SampleService.class, "get")).accepts(hints);
		assertThat(reflection().onType(Response.class)).accepts(hints);
		assertThat(reflection().onMethodInvocation(Response.class, "getMessage")).accepts(hints);
		assertThat(reflection().onMethodInvocation(Response.class, "setMessage")).accepts(hints);
	}

	@Test
	void registerReflectiveHintsForMethodWithRequestBodyParameter() throws NoSuchMethodException {
		Method method = SampleService.class.getDeclaredMethod("post", Request.class);
		processor.registerReflectionHints(hints.reflection(), method);
		assertThat(reflection().onType(SampleService.class)).accepts(hints);
		assertThat(reflection().onMethodInvocation(SampleService.class, "post")).accepts(hints);
		assertThat(reflection().onType(Request.class)).accepts(hints);
		assertThat(reflection().onMethodInvocation(Request.class, "getMessage")).accepts(hints);
		assertThat(reflection().onMethodInvocation(Request.class, "setMessage")).accepts(hints);
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
