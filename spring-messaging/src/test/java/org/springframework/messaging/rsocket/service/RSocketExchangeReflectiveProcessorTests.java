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

package org.springframework.messaging.rsocket.service;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;

/**
 * Tests for {@link RSocketExchangeReflectiveProcessor}.
 *
 * @author Sebastien Deleuze
 * @author Olga Maciaszek-Sharma
 * @since 6.0.5
 */
class RSocketExchangeReflectiveProcessorTests {

	private final RSocketExchangeReflectiveProcessor processor = new RSocketExchangeReflectiveProcessor();

	private final RuntimeHints hints = new RuntimeHints();


	@Test
	void shouldRegisterReflectionHintsForMethod() throws NoSuchMethodException {
		Method method = SampleService.class.getDeclaredMethod("get", Request.class, Variable.class,
				Metadata.class, MimeType.class);

		processor.registerReflectionHints(hints.reflection(), method);

		assertThat(reflection().onType(SampleService.class)).accepts(hints);
		assertThat(reflection().onMethod(SampleService.class, "get")).accepts(hints);
		assertThat(reflection().onType(Response.class)).accepts(hints);
		assertThat(reflection().onMethod(Response.class, "getMessage")).accepts(hints);
		assertThat(reflection().onMethod(Response.class, "setMessage")).accepts(hints);
		assertThat(reflection().onType(Request.class)).accepts(hints);
		assertThat(reflection().onMethod(Request.class, "getMessage")).accepts(hints);
		assertThat(reflection().onMethod(Request.class, "setMessage")).accepts(hints);
		assertThat(reflection().onType(Variable.class)).accepts(hints);
		assertThat(reflection().onMethod(Variable.class, "getValue")).accepts(hints);
		assertThat(reflection().onMethod(Variable.class, "setValue")).accepts(hints);
		assertThat(reflection().onType(Metadata.class)).accepts(hints);
		assertThat(reflection().onMethod(Metadata.class, "getValue")).accepts(hints);
		assertThat(reflection().onMethod(Metadata.class, "setValue")).accepts(hints);
	}


	interface SampleService {

		@RSocketExchange
		Response get(@Payload Request request, @DestinationVariable Variable variable,
				Metadata metadata, MimeType mimeType);

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

	static class Variable {

		private String value;

		public Variable(String value) {
			this.value = value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	static class Metadata {

		private String value;

		public Metadata(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

}
