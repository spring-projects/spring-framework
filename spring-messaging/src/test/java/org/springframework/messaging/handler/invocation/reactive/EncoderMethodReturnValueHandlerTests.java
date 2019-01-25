/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.messaging.handler.invocation.reactive;

import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.messaging.handler.invocation.ResolvableMethod.*;

/**
 * Unit tests for {@link AbstractEncoderMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class EncoderMethodReturnValueHandlerTests {

	private final TestEncoderMethodReturnValueHandler handler = new TestEncoderMethodReturnValueHandler(
			Collections.singletonList(CharSequenceEncoder.textPlainOnly()),
			ReactiveAdapterRegistry.getSharedInstance());

	private final Message<?> message = mock(Message.class);


	@Test
	public void stringReturnValue() {
		MethodParameter parameter = on(TestController.class).resolveReturnType(String.class);
		this.handler.handleReturnValue("foo", parameter, message).block();
		Flux<DataBuffer> result = this.handler.encodedContent;

		StepVerifier.create(result)
				.consumeNextWith(buffer -> assertEquals("foo", DataBufferTestUtils.dumpString(buffer, UTF_8)))
				.verifyComplete();
	}

	@Test
	public void objectReturnValue() {
		MethodParameter parameter = on(TestController.class).resolveReturnType(Object.class);
		this.handler.handleReturnValue("foo", parameter, message).block();
		Flux<DataBuffer> result = this.handler.encodedContent;

		StepVerifier.create(result)
				.consumeNextWith(buffer -> assertEquals("foo", DataBufferTestUtils.dumpString(buffer, UTF_8)))
				.verifyComplete();
	}

	@Test
	public void fluxStringReturnValue() {
		MethodParameter parameter = on(TestController.class).resolveReturnType(Flux.class, String.class);
		this.handler.handleReturnValue(Flux.just("foo", "bar"), parameter, message).block();
		Flux<DataBuffer> result = this.handler.encodedContent;

		StepVerifier.create(result)
				.consumeNextWith(buffer -> assertEquals("foo", DataBufferTestUtils.dumpString(buffer, UTF_8)))
				.consumeNextWith(buffer -> assertEquals("bar", DataBufferTestUtils.dumpString(buffer, UTF_8)))
				.verifyComplete();
	}

	@Test
	public void voidReturnValue() {
		testVoidReturnType(null, on(TestController.class).resolveReturnType(void.class));
		testVoidReturnType(Mono.empty(), on(TestController.class).resolveReturnType(Mono.class, Void.class));
		testVoidReturnType(Completable.complete(), on(TestController.class).resolveReturnType(Completable.class));

	}

	private void testVoidReturnType(@Nullable Object value, MethodParameter bodyParameter) {
		this.handler.handleReturnValue(value, bodyParameter, message).block();
		Flux<DataBuffer> result = this.handler.encodedContent;
		StepVerifier.create(result).expectComplete().verify();
	}

	@Test
	public void noEncoder() {
		MethodParameter parameter = on(TestController.class).resolveReturnType(Object.class);
		this.handler.handleReturnValue(new Object(), parameter, message).block();
		Flux<DataBuffer> result = this.handler.encodedContent;

		StepVerifier.create(result)
				.expectErrorMessage("No encoder for method 'object' parameter -1")
				.verify();
	}


	@SuppressWarnings({"unused", "ConstantConditions"})
	private static class TestController {

		String string() { return null; }

		Object object() { return null; }

		Flux<String> fluxString() { return null; }

		void voidReturn() { }

		Mono<Void> monoVoid() { return null; }

		Completable completable() { return null; }
	}


	private static class TestEncoderMethodReturnValueHandler extends AbstractEncoderMethodReturnValueHandler {

		private Flux<DataBuffer> encodedContent;


		public Flux<DataBuffer> getEncodedContent() {
			return this.encodedContent;
		}

		protected TestEncoderMethodReturnValueHandler(List<Encoder<?>> encoders, ReactiveAdapterRegistry registry) {
			super(encoders, registry);
		}

		@Override
		protected Mono<Void> handleEncodedContent(
				Flux<DataBuffer> encodedContent, MethodParameter returnType, Message<?> message) {

			this.encodedContent = encodedContent;
			return Mono.empty();
		}
	}

}
