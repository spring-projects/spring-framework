/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		assertThat(reflection().onMethodInvocation(SampleService.class, "get")).accepts(hints);
		assertThat(reflection().onType(Response.class)).accepts(hints);
		assertThat(reflection().onMethodInvocation(Response.class, "getMessage")).accepts(hints);
		assertThat(reflection().onMethodInvocation(Response.class, "setMessage")).accepts(hints);
		assertThat(reflection().onType(Request.class)).accepts(hints);
		assertThat(reflection().onMethodInvocation(Request.class, "getMessage")).accepts(hints);
		assertThat(reflection().onMethodInvocation(Request.class, "setMessage")).accepts(hints);
		assertThat(reflection().onType(Variable.class)).accepts(hints);
		assertThat(reflection().onMethodInvocation(Variable.class, "getValue")).accepts(hints);
		assertThat(reflection().onMethodInvocation(Variable.class, "setValue")).accepts(hints);
		assertThat(reflection().onType(Metadata.class)).accepts(hints);
		assertThat(reflection().onMethodInvocation(Metadata.class, "getValue")).accepts(hints);
		assertThat(reflection().onMethodInvocation(Metadata.class, "setValue")).accepts(hints);
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
