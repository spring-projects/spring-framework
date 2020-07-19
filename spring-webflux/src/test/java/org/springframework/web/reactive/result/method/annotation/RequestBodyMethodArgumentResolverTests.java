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

package org.springframework.web.reactive.result.method.annotation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.web.testfixture.method.MvcAnnotationPredicates.requestBody;

/**
 * Unit tests for {@link RequestBodyMethodArgumentResolver}. When adding a test also
 * consider whether the logic under test is in a parent class, then see:
 * {@link MessageReaderArgumentResolverTests}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestBodyMethodArgumentResolverTests {

	private RequestBodyMethodArgumentResolver resolver;

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@BeforeEach
	public void setup() {
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		readers.add(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		this.resolver = new RequestBodyMethodArgumentResolver(readers, ReactiveAdapterRegistry.getSharedInstance());
	}


	@Test
	public void supports() {
		MethodParameter param;

		param = this.testMethod.annot(requestBody()).arg(Mono.class, String.class);
		assertThat(this.resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotNotPresent(RequestBody.class).arg(String.class);
		assertThat(this.resolver.supportsParameter(param)).isFalse();
	}

	@Test
	public void stringBody() {
		String body = "line1";
		MethodParameter param = this.testMethod.annot(requestBody()).arg(String.class);
		String value = resolveValue(param, body);

		assertThat(value).isEqualTo(body);
	}

	@Test
	public void emptyBodyWithString() {
		MethodParameter param = this.testMethod.annot(requestBody()).arg(String.class);
		assertThatExceptionOfType(ServerWebInputException.class).isThrownBy(() ->
				resolveValueWithEmptyBody(param));
	}

	@Test
	public void emptyBodyWithStringNotRequired() {
		MethodParameter param = this.testMethod.annot(requestBody().notRequired()).arg(String.class);
		String body = resolveValueWithEmptyBody(param);

		assertThat(body).isNull();
	}

	@Test // SPR-15758
	public void emptyBodyWithoutContentType() {
		MethodParameter param = this.testMethod.annot(requestBody().notRequired()).arg(Map.class);
		String body = resolveValueWithEmptyBody(param);

		assertThat(body).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void emptyBodyWithMono() {
		MethodParameter param = this.testMethod.annot(requestBody()).arg(Mono.class, String.class);
		StepVerifier.create((Mono<Void>) resolveValueWithEmptyBody(param))
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		param = this.testMethod.annot(requestBody().notRequired()).arg(Mono.class, String.class);
		StepVerifier.create((Mono<Void>) resolveValueWithEmptyBody(param))
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void emptyBodyWithFlux() {
		MethodParameter param = this.testMethod.annot(requestBody()).arg(Flux.class, String.class);
		StepVerifier.create((Flux<Void>) resolveValueWithEmptyBody(param))
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		param = this.testMethod.annot(requestBody().notRequired()).arg(Flux.class, String.class);
		StepVerifier.create((Flux<Void>) resolveValueWithEmptyBody(param))
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithSingle() {
		MethodParameter param = this.testMethod.annot(requestBody()).arg(Single.class, String.class);
		Single<String> single = resolveValueWithEmptyBody(param);
		StepVerifier.create(single.toFlowable())
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		param = this.testMethod.annot(requestBody().notRequired()).arg(Single.class, String.class);
		single = resolveValueWithEmptyBody(param);
		StepVerifier.create(single.toFlowable())
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	public void emptyBodyWithMaybe() {
		MethodParameter param = this.testMethod.annot(requestBody()).arg(Maybe.class, String.class);
		Maybe<String> maybe = resolveValueWithEmptyBody(param);
		StepVerifier.create(maybe.toFlowable())
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		param = this.testMethod.annot(requestBody().notRequired()).arg(Maybe.class, String.class);
		maybe = resolveValueWithEmptyBody(param);
		StepVerifier.create(maybe.toFlowable())
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithObservable() {
		MethodParameter param = this.testMethod.annot(requestBody()).arg(Observable.class, String.class);
		Observable<String> observable = resolveValueWithEmptyBody(param);
		StepVerifier.create(observable.toFlowable(BackpressureStrategy.BUFFER))
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		param = this.testMethod.annot(requestBody().notRequired()).arg(Observable.class, String.class);
		observable = resolveValueWithEmptyBody(param);
		StepVerifier.create(observable.toFlowable(BackpressureStrategy.BUFFER))
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithCompletableFuture() {
		MethodParameter param = this.testMethod.annot(requestBody()).arg(CompletableFuture.class, String.class);
		CompletableFuture<String> future = resolveValueWithEmptyBody(param);
		future.whenComplete((text, ex) -> {
			assertThat(text).isNull();
			assertThat(ex).isNotNull();
		});

		param = this.testMethod.annot(requestBody().notRequired()).arg(CompletableFuture.class, String.class);
		future = resolveValueWithEmptyBody(param);
		future.whenComplete((text, ex) -> {
			assertThat(text).isNotNull();
			assertThat(ex).isNull();
		});
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveValue(MethodParameter param, String body) {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/path").body(body));
		Mono<Object> result = this.resolver.readBody(param, true, new BindingContext(), exchange);
		Object value = result.block(Duration.ofSeconds(5));

		assertThat(value).isNotNull();
		assertThat(param.getParameterType().isAssignableFrom(value.getClass())).as("Unexpected return value type: " + value).isTrue();

		//no inspection unchecked
		return (T) value;
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveValueWithEmptyBody(MethodParameter param) {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/path"));
		Mono<Object> result = this.resolver.resolveArgument(param, new BindingContext(), exchange);
		Object value = result.block(Duration.ofSeconds(5));

		if (value != null) {
			assertThat(param.getParameterType().isAssignableFrom(value.getClass())).as("Unexpected parameter type: " + value).isTrue();
		}

		//no inspection unchecked
		return (T) value;
	}


	@SuppressWarnings("unused")
	void handle(
			@RequestBody String string,
			@RequestBody Mono<String> mono,
			@RequestBody Flux<String> flux,
			@RequestBody Single<String> single,
			@RequestBody Maybe<String> maybe,
			@RequestBody Observable<String> observable,
			@RequestBody CompletableFuture<String> future,
			@RequestBody(required = false) String stringNotRequired,
			@RequestBody(required = false) Mono<String> monoNotRequired,
			@RequestBody(required = false) Flux<String> fluxNotRequired,
			@RequestBody(required = false) Single<String> singleNotRequired,
			@RequestBody(required = false) Maybe<String> maybeNotRequired,
			@RequestBody(required = false) Observable<String> observableNotRequired,
			@RequestBody(required = false) CompletableFuture<String> futureNotRequired,
			@RequestBody(required = false) Map<?, ?> mapNotRequired,
			String notAnnotated) {}

}
