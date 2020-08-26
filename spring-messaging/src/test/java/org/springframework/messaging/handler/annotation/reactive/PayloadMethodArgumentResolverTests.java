/*
 * Copyright 2002-2020 the original author or authors.
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
package org.springframework.messaging.handler.annotation.reactive;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.messaging.handler.invocation.ResolvableMethod;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeTypeUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit tests for {@link PayloadMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class PayloadMethodArgumentResolverTests {

	private final List<Decoder<?>> decoders = new ArrayList<>();

	private final ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	public void supportsParameter() {

		boolean useDefaultResolution = true;
		PayloadMethodArgumentResolver resolver = createResolver(null, useDefaultResolution);

		assertThat(resolver.supportsParameter(this.testMethod.annotPresent(Payload.class).arg())).isTrue();
		assertThat(resolver.supportsParameter(this.testMethod.annotNotPresent(Payload.class).arg(String.class))).isTrue();

		useDefaultResolution = false;
		resolver = createResolver(null, useDefaultResolution);

		assertThat(resolver.supportsParameter(this.testMethod.annotPresent(Payload.class).arg())).isTrue();
		assertThat(resolver.supportsParameter(this.testMethod.annotNotPresent(Payload.class).arg(String.class))).isFalse();
	}

	@Test
	public void emptyBodyWhenRequired() {
		MethodParameter param = this.testMethod.arg(ResolvableType.forClassWithGenerics(Mono.class, String.class));
		Mono<Object> mono = resolveValue(param, Mono.empty(), null);

		StepVerifier.create(mono)
				.consumeErrorWith(ex -> {
					assertThat(ex.getClass()).isEqualTo(MethodArgumentResolutionException.class);
					assertThat(ex.getMessage().contains("Payload content is missing")).as(ex.getMessage()).isTrue();
				})
				.verify();
	}

	@Test
	public void emptyBodyWhenNotRequired() {
		MethodParameter param = this.testMethod.annotPresent(Payload.class).arg();
		assertThat(this.<Object>resolveValue(param, Mono.empty(), null)).isNull();
	}

	@Test
	public void stringMono() {
		String body = "foo";
		MethodParameter param = this.testMethod.arg(ResolvableType.forClassWithGenerics(Mono.class, String.class));
		Mono<Object> mono = resolveValue(param,
				Mono.delay(Duration.ofMillis(10)).map(aLong -> toDataBuffer(body)), null);

		assertThat(mono.block()).isEqualTo(body);
	}

	@Test
	public void stringFlux() {
		List<String> body = Arrays.asList("foo", "bar");
		ResolvableType type = ResolvableType.forClassWithGenerics(Flux.class, String.class);
		MethodParameter param = this.testMethod.arg(type);
		Flux<Object> flux = resolveValue(param,
				Flux.fromIterable(body).delayElements(Duration.ofMillis(10)).map(this::toDataBuffer), null);

		assertThat(flux.collectList().block()).isEqualTo(body);
	}

	@Test
	public void string() {
		String body = "foo";
		MethodParameter param = this.testMethod.annotNotPresent(Payload.class).arg(String.class);
		Object value = resolveValue(param, Mono.just(toDataBuffer(body)), null);

		assertThat(value).isEqualTo(body);
	}

	@Test
	public void validateStringMono() {
		TestValidator validator = new TestValidator();
		ResolvableType type = ResolvableType.forClassWithGenerics(Mono.class, String.class);
		MethodParameter param = this.testMethod.arg(type);
		Mono<Object> mono = resolveValue(param, Mono.just(toDataBuffer("12345")), validator);

		StepVerifier.create(mono).expectNextCount(0)
				.expectError(MethodArgumentNotValidException.class).verify();
	}

	@Test
	public void validateStringFlux() {
		TestValidator validator = new TestValidator();
		ResolvableType type = ResolvableType.forClassWithGenerics(Flux.class, String.class);
		MethodParameter param = this.testMethod.arg(type);
		Flux<DataBuffer> content = Flux.just(toDataBuffer("12345678"), toDataBuffer("12345"));
		Flux<Object> flux = resolveValue(param, content, validator);

		StepVerifier.create(flux)
				.expectNext("12345678")
				.expectError(MethodArgumentNotValidException.class)
				.verify();
	}


	private DataBuffer toDataBuffer(String value) {
		return DefaultDataBufferFactory.sharedInstance.wrap(value.getBytes(StandardCharsets.UTF_8));
	}


	@SuppressWarnings("unchecked")
	@Nullable
	private <T> T resolveValue(MethodParameter param, Publisher<DataBuffer> content, Validator validator) {

		Message<?> message = new GenericMessage<>(content,
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN));

		Mono<Object> result = createResolver(validator, true).resolveArgument(param, message);

		Object value = result.block(Duration.ofSeconds(5));
		if (value != null) {
			Class<?> expectedType = param.getParameterType();
			assertThat(expectedType.isAssignableFrom(value.getClass())).as("Unexpected return value type: " + value).isTrue();
		}
		return (T) value;
	}

	private PayloadMethodArgumentResolver createResolver(@Nullable Validator validator, boolean useDefaultResolution) {
		if (this.decoders.isEmpty()) {
			this.decoders.add(StringDecoder.allMimeTypes());
		}
		List<StringDecoder> decoders = Collections.singletonList(StringDecoder.allMimeTypes());
		return new PayloadMethodArgumentResolver(decoders, validator, null, useDefaultResolution) {};
	}


	@SuppressWarnings("unused")
	private void handle(
			@Validated Mono<String> valueMono,
			@Validated Flux<String> valueFlux,
			@Payload(required = false) String optionalValue,
			String value) {
	}


	private static class TestValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.equals(String.class);
		}

		@Override
		public void validate(@Nullable Object target, Errors errors) {
			if (target instanceof String && ((String) target).length() < 8) {
				errors.reject("Invalid length");
			}
		}
	}

}
