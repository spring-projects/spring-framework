/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.invocation.reactive;

import java.util.Collections;

import io.reactivex.rxjava3.core.Completable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.springframework.messaging.handler.invocation.ResolvableMethod.on;

/**
 * Tests for {@link AbstractEncoderMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
class EncoderMethodReturnValueHandlerTests {

	private final TestEncoderMethodReturnValueHandler handler = new TestEncoderMethodReturnValueHandler(
			Collections.singletonList(CharSequenceEncoder.textPlainOnly()),
			ReactiveAdapterRegistry.getSharedInstance());

	private final Message<?> message = new GenericMessage<>("shouldn't matter");


	@Test
	void stringReturnValue() {
		MethodParameter parameter = on(TestController.class).resolveReturnType(String.class);
		this.handler.handleReturnValue("foo", parameter, this.message).block();
		Flux<String> result = this.handler.getContentAsStrings();

		StepVerifier.create(result).expectNext("foo").verifyComplete();
	}

	@Test
	void objectReturnValue() {
		MethodParameter parameter = on(TestController.class).resolveReturnType(Object.class);
		this.handler.handleReturnValue("foo", parameter, this.message).block();
		Flux<String> result = this.handler.getContentAsStrings();

		StepVerifier.create(result).expectNext("foo").verifyComplete();
	}

	@Test
	void fluxStringReturnValue() {
		MethodParameter parameter = on(TestController.class).resolveReturnType(Flux.class, String.class);
		this.handler.handleReturnValue(Flux.just("foo", "bar"), parameter, this.message).block();
		Flux<String> result = this.handler.getContentAsStrings();

		StepVerifier.create(result).expectNext("foo").expectNext("bar").verifyComplete();
	}

	@Test
	void fluxObjectReturnValue() {
		MethodParameter parameter = on(TestController.class).resolveReturnType(Flux.class, Object.class);
		this.handler.handleReturnValue(Flux.just("foo", "bar"), parameter, this.message).block();
		Flux<String> result = this.handler.getContentAsStrings();

		StepVerifier.create(result).expectNext("foo").expectNext("bar").verifyComplete();
	}

	@Test
	void voidReturnValue() {
		testVoidReturnType(null, on(TestController.class).resolveReturnType(void.class));
		testVoidReturnType(Mono.empty(), on(TestController.class).resolveReturnType(Mono.class, Void.class));
		testVoidReturnType(Completable.complete(), on(TestController.class).resolveReturnType(Completable.class));
	}

	private void testVoidReturnType(@Nullable Object value, MethodParameter bodyParameter) {
		this.handler.handleReturnValue(value, bodyParameter, this.message).block();
		Flux<String> result = this.handler.getContentAsStrings();
		StepVerifier.create(result).expectComplete().verify();
	}

	@Test
	void noEncoder() {
		MethodParameter parameter = on(TestController.class).resolveReturnType(Object.class);
		StepVerifier.create(this.handler.handleReturnValue(new Object(), parameter, this.message))
				.expectErrorMessage("No encoder for java.lang.Object, current value type is class java.lang.Object")
				.verify();
	}


	@SuppressWarnings({"unused", "ConstantConditions"})
	private static class TestController {

		String string() { return null; }

		Object object() { return null; }

		Flux<String> fluxString() { return null; }

		Flux<Object> fluxObject() { return null; }

		void voidReturn() { }

		Mono<Void> monoVoid() { return null; }

		Completable completable() { return null; }
	}

}
