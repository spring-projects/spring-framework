/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.ResolvableType.forClassWithGenerics;

/**
 * Unit tests for {@link RequestBodyArgumentResolver}. When adding a test also
 * consider whether the logic under test is in a parent class, then see:
 * {@link MessageReaderArgumentResolverTests}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestBodyArgumentResolverTests {

	private RequestBodyArgumentResolver resolver;

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Before
	public void setup() {
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		readers.add(new DecoderHttpMessageReader<>(new StringDecoder()));
		this.resolver = new RequestBodyArgumentResolver(readers);
	}


	@Test
	public void supports() throws Exception {

		ResolvableType type = forClassWithGenerics(Mono.class, String.class);
		assertTrue(this.resolver.supportsParameter(
				this.testMethod.annotated(RequestBody.class, required()).arg(type)));

		MethodParameter parameter = this.testMethod.notAnnotated(RequestBody.class).arg(String.class);
		assertFalse(this.resolver.supportsParameter(parameter));
	}

	@Test
	public void stringBody() throws Exception {
		String body = "line1";
		MethodParameter param = this.testMethod.annotated(RequestBody.class, required()).arg(String.class);
		String value = resolveValue(param, body);

		assertEquals(body, value);
	}

	@Test(expected = ServerWebInputException.class)
	public void emptyBodyWithString() throws Exception {
		MethodParameter param = this.testMethod.annotated(RequestBody.class, required()).arg(String.class);
		resolveValueWithEmptyBody(param);
	}

	@Test
	public void emptyBodyWithStringNotRequired() throws Exception {
		MethodParameter param = this.testMethod.annotated(RequestBody.class, notRequired()).arg(String.class);
		String body = resolveValueWithEmptyBody(param);

		assertNull(body);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void emptyBodyWithMono() throws Exception {
		ResolvableType type = forClassWithGenerics(Mono.class, String.class);

		MethodParameter param = this.testMethod.annotated(RequestBody.class, required()).arg(type);
		StepVerifier.create((Mono<Void>) resolveValueWithEmptyBody(param))
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		param = this.testMethod.annotated(RequestBody.class, notRequired()).arg(type);
		StepVerifier.create((Mono<Void>) resolveValueWithEmptyBody(param))
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void emptyBodyWithFlux() throws Exception {
		ResolvableType type = forClassWithGenerics(Flux.class, String.class);

		MethodParameter param = this.testMethod.annotated(RequestBody.class, required()).arg(type);
		StepVerifier.create((Flux<Void>) resolveValueWithEmptyBody(param))
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		param = this.testMethod.annotated(RequestBody.class, notRequired()).arg(type);
		StepVerifier.create((Flux<Void>) resolveValueWithEmptyBody(param))
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithSingle() throws Exception {
		ResolvableType type = forClassWithGenerics(Single.class, String.class);

		MethodParameter param = this.testMethod.annotated(RequestBody.class, required()).arg(type);
		Single<String> single = resolveValueWithEmptyBody(param);
		StepVerifier.create(RxReactiveStreams.toPublisher(single))
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		param = this.testMethod.annotated(RequestBody.class, notRequired()).arg(type);
		single = resolveValueWithEmptyBody(param);
		StepVerifier.create(RxReactiveStreams.toPublisher(single))
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	public void emptyBodyWithMaybe() throws Exception {
		ResolvableType type = forClassWithGenerics(Maybe.class, String.class);

		MethodParameter param = this.testMethod.annotated(RequestBody.class, required()).arg(type);
		Maybe<String> maybe = resolveValueWithEmptyBody(param);
		StepVerifier.create(maybe.toFlowable())
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		param = this.testMethod.annotated(RequestBody.class, notRequired()).arg(type);
		maybe = resolveValueWithEmptyBody(param);
		StepVerifier.create(maybe.toFlowable())
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithObservable() throws Exception {
		ResolvableType type = forClassWithGenerics(Observable.class, String.class);

		MethodParameter param = this.testMethod.annotated(RequestBody.class, required()).arg(type);
		Observable<String> observable = resolveValueWithEmptyBody(param);
		StepVerifier.create(RxReactiveStreams.toPublisher(observable))
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		param = this.testMethod.annotated(RequestBody.class, notRequired()).arg(type);
		observable = resolveValueWithEmptyBody(param);
		StepVerifier.create(RxReactiveStreams.toPublisher(observable))
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithCompletableFuture() throws Exception {
		ResolvableType type = forClassWithGenerics(CompletableFuture.class, String.class);

		MethodParameter param = this.testMethod.annotated(RequestBody.class, required()).arg(type);
		CompletableFuture<String> future = resolveValueWithEmptyBody(param);
		future.whenComplete((text, ex) -> {
			assertNull(text);
			assertNotNull(ex);
		});

		param = this.testMethod.annotated(RequestBody.class, notRequired()).arg(type);
		future = resolveValueWithEmptyBody(param);
		future.whenComplete((text, ex) -> {
			assertNotNull(text);
			assertNull(ex);
		});
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveValue(MethodParameter param, String body) {
		MockServerHttpRequest request = MockServerHttpRequest.post("/path").body(body);
		ServerWebExchange exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse());
		Mono<Object> result = this.resolver.readBody(param, true, new BindingContext(), exchange);
		Object value = result.block(Duration.ofSeconds(5));

		assertNotNull(value);
		assertTrue("Unexpected return value type: " + value,
				param.getParameterType().isAssignableFrom(value.getClass()));

		//no inspection unchecked
		return (T) value;
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveValueWithEmptyBody(MethodParameter param) {
		MockServerHttpRequest request = MockServerHttpRequest.post("/path").build();
		ServerWebExchange exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse());
		Mono<Object> result = this.resolver.resolveArgument(param, new BindingContext(), exchange);
		Object value = result.block(Duration.ofSeconds(5));

		if (value != null) {
			assertTrue("Unexpected parameter type: " + value,
					param.getParameterType().isAssignableFrom(value.getClass()));
		}

		//no inspection unchecked
		return (T) value;
	}

	private Predicate<RequestBody> required() {
		return RequestBody::required;
	}

	private Predicate<RequestBody> notRequired() {
		return a -> !a.required();
	}


	@SuppressWarnings("unused")
	void handle(
			@RequestBody String string,
			@RequestBody Mono<String> mono,
			@RequestBody Flux<String> flux,
			@RequestBody Single<String> single,
			@RequestBody io.reactivex.Single<String> rxJava2Single,
			@RequestBody Maybe<String> rxJava2Maybe,
			@RequestBody Observable<String> obs,
			@RequestBody io.reactivex.Observable<String> rxjava2Obs,
			@RequestBody CompletableFuture<String> future,
			@RequestBody(required = false) String stringNotRequired,
			@RequestBody(required = false) Mono<String> monoNotRequired,
			@RequestBody(required = false) Flux<String> fluxNotRequired,
			@RequestBody(required = false) Single<String> singleNotRequired,
			@RequestBody(required = false) io.reactivex.Single<String> rxJava2SingleNotRequired,
			@RequestBody(required = false) Maybe<String> rxJava2MaybeNotRequired,
			@RequestBody(required = false) Observable<String> obsNotRequired,
			@RequestBody(required = false) io.reactivex.Observable<String> rxjava2ObsNotRequired,
			@RequestBody(required = false) CompletableFuture<String> futureNotRequired,
			String notAnnotated) {}

}
