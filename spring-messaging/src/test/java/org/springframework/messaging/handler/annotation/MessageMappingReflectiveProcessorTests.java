/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.messaging.handler.annotation;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.Principal;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MessageMappingReflectiveProcessor}.
 *
 * @author Sebastien Deleuze
 */
class MessageMappingReflectiveProcessorTests {

	private final MessageMappingReflectiveProcessor processor = new MessageMappingReflectiveProcessor();

	private final RuntimeHints hints = new RuntimeHints();

	@Test
	void registerReflectiveHintsForMethodWithReturnValue() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("returnValue");
		processor.registerReflectionHints(hints.reflection(), method);
		assertThat(hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(OutgoingMessage.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.DECLARED_FIELDS);
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
							hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
				},
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithExplicitPayload() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("explicitPayload", IncomingMessage.class);
		processor.registerReflectionHints(hints.reflection(), method);
		assertThat(hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(IncomingMessage.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.DECLARED_FIELDS);
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
							hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
				},
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithImplicitPayload() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("implicitPayload", IncomingMessage.class);
		processor.registerReflectionHints(hints.reflection(), method);
		assertThat(hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(IncomingMessage.class)),
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithMessage() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("message", Message.class);
		processor.registerReflectionHints(hints.reflection(), method);
		assertThat(hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(IncomingMessage.class)),
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithImplicitPayloadAndIgnoredAnnotations() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("implicitPayloadWithIgnoredAnnotations",
				IncomingMessage.class, Ignored.class, Ignored.class, Ignored.class, MessageHeaders.class,
				MessageHeaderAccessor.class, Principal.class);
		processor.registerReflectionHints(hints.reflection(), method);
		assertThat(hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(IncomingMessage.class)),
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForClass() {
		processor.registerReflectionHints(hints.reflection(), SampleAnnotatedController.class);
		assertThat(hints.reflection().typeHints()).singleElement().satisfies(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleAnnotatedController.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithSubscribeMapping() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("handleSubscribe");
		processor.registerReflectionHints(hints.reflection(), method);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleController.class, "handleSubscribe")).accepts(hints);
	}

	@Test
	void registerReflectiveHintsForMethodWithMessageExceptionHandler() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("handleIOException");
		processor.registerReflectionHints(hints.reflection(), method);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleController.class, "handleIOException")).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(IOException.class)).accepts(hints);
	}


	static class SampleController {

		@MessageMapping
		OutgoingMessage returnValue() {
			return new OutgoingMessage("message");
		}

		@MessageMapping
		void explicitPayload(@Payload IncomingMessage incomingMessage) {
		}

		@MessageMapping
		void implicitPayload(IncomingMessage incomingMessage) {
		}

		@MessageMapping
		void message(Message<IncomingMessage> message) {
		}

		@MessageMapping
		void implicitPayloadWithIgnoredAnnotations(IncomingMessage incomingMessage,
				@DestinationVariable Ignored destinationVariable,
				@Header Ignored header,
				@Headers Ignored headers,
				MessageHeaders messageHeaders,
				MessageHeaderAccessor messageHeaderAccessor,
				Principal principal) {
		}

		@SubscribeMapping("/foo")
		public String handleSubscribe() {
			return "bar";
		}

		@MessageExceptionHandler(IOException.class)
		public void handleIOException() {
		}

	}

	@MessageMapping
	static class SampleAnnotatedController {
	}

	static class IncomingMessage {

		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	static class OutgoingMessage {

		private String message;

		public OutgoingMessage(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	static class Ignored {}
}
