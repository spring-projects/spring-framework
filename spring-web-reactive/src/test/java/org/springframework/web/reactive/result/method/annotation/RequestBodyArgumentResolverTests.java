/*
 * Copyright 2002-2016 the original author or authors.
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
import reactor.test.subscriber.ScriptedSubscriber;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.result.ResolvableMethod;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.core.ResolvableType.forClassWithGenerics;

/**
 * Unit tests for {@link RequestBodyArgumentResolver}. When adding a test also
 * consider whether the logic under test is in a parent class, then see:
 * {@link MessageReaderArgumentResolverTests}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestBodyArgumentResolverTests {

	private RequestBodyArgumentResolver resolver = resolver();

	private ServerWebExchange exchange;

	private MockServerHttpRequest request;

	private ResolvableMethod testMethod = ResolvableMethod.onClass(this.getClass()).name("handle");


	@Before
	public void setUp() throws Exception {
		this.request = new MockServerHttpRequest(HttpMethod.POST, "/path");
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(this.request, response, new MockWebSessionManager());
	}

	private RequestBodyArgumentResolver resolver() {
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		readers.add(new DecoderHttpMessageReader<>(new StringDecoder()));
		return new RequestBodyArgumentResolver(readers);
	}


	@Test
	public void supports() throws Exception {
		ResolvableType type = forClassWithGenerics(Mono.class, String.class);
		MethodParameter param = this.testMethod.resolveParam(type, requestBody(true));
		assertTrue(this.resolver.supportsParameter(param));

		MethodParameter parameter = this.testMethod.resolveParam(p -> !p.hasParameterAnnotations());
		assertFalse(this.resolver.supportsParameter(parameter));
	}

	@Test
	public void stringBody() throws Exception {
		String body = "line1";
		ResolvableType type = forClass(String.class);
		MethodParameter param = this.testMethod.resolveParam(type, requestBody(true));
		String value = resolveValue(param, body);

		assertEquals(body, value);
	}

	@Test(expected = ServerWebInputException.class)
	public void emptyBodyWithString() throws Exception {
		resolveValueWithEmptyBody(forClass(String.class), true);
	}

	@Test
	public void emptyBodyWithStringNotRequired() throws Exception {
		ResolvableType type = forClass(String.class);
		String body = resolveValueWithEmptyBody(type, false);

		assertNull(body);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void emptyBodyWithMono() throws Exception {
		ResolvableType type = forClassWithGenerics(Mono.class, String.class);

		ScriptedSubscriber.<Void>create().expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify((Mono<Void>) resolveValueWithEmptyBody(type, true));

		ScriptedSubscriber.<Void>create().expectNextCount(0)
				.expectComplete()
				.verify((Mono<Void>) resolveValueWithEmptyBody(type, false));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void emptyBodyWithFlux() throws Exception {
		ResolvableType type = forClassWithGenerics(Flux.class, String.class);

		ScriptedSubscriber.<Void>create().expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify((Flux<Void>) resolveValueWithEmptyBody(type, true));

		ScriptedSubscriber.<Void>create().expectNextCount(0)
				.expectComplete()
				.verify((Flux<Void>) resolveValueWithEmptyBody(type, false));
	}

	@Test
	public void emptyBodyWithSingle() throws Exception {
		ResolvableType type = forClassWithGenerics(Single.class, String.class);

		Single<String> single = resolveValueWithEmptyBody(type, true);
		ScriptedSubscriber.<String>create().expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify(RxReactiveStreams.toPublisher(single));

		single = resolveValueWithEmptyBody(type, false);
		ScriptedSubscriber.<String>create().expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify(RxReactiveStreams.toPublisher(single));
	}

	@Test
	public void emptyBodyWithMaybe() throws Exception {
		ResolvableType type = forClassWithGenerics(Maybe.class, String.class);

		Maybe<String> maybe = resolveValueWithEmptyBody(type, true);
		ScriptedSubscriber.<String>create().expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify(maybe.toFlowable());

		maybe = resolveValueWithEmptyBody(type, false);
		ScriptedSubscriber.<String>create().expectNextCount(0)
				.expectComplete()
				.verify(maybe.toFlowable());
	}

	@Test
	public void emptyBodyWithObservable() throws Exception {
		ResolvableType type = forClassWithGenerics(Observable.class, String.class);

		Observable<String> observable = resolveValueWithEmptyBody(type, true);
		ScriptedSubscriber.<String>create().expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify(RxReactiveStreams.toPublisher(observable));

		observable = resolveValueWithEmptyBody(type, false);
		ScriptedSubscriber.<String>create().expectNextCount(0)
				.expectComplete()
				.verify(RxReactiveStreams.toPublisher(observable));
	}

	@Test
	public void emptyBodyWithCompletableFuture() throws Exception {
		ResolvableType type = forClassWithGenerics(CompletableFuture.class, String.class);

		CompletableFuture<String> future = resolveValueWithEmptyBody(type, true);
		future.whenComplete((text, ex) -> {
			assertNull(text);
			assertNotNull(ex);
		});

		future = resolveValueWithEmptyBody(type, false);
		future.whenComplete((text, ex) -> {
			assertNotNull(text);
			assertNull(ex);
		});
	}


	@SuppressWarnings("unchecked")
	private <T> T resolveValue(MethodParameter param, String body) {
		this.request.setBody(body);
		Mono<Object> result = this.resolver.readBody(param, true, new BindingContext(), this.exchange);
		Object value = result.block(Duration.ofSeconds(5));

		assertNotNull(value);
		assertTrue("Unexpected return value type: " + value,
				param.getParameterType().isAssignableFrom(value.getClass()));

		//no inspection unchecked
		return (T) value;
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveValueWithEmptyBody(ResolvableType bodyType, boolean isRequired) {
		MethodParameter param = this.testMethod.resolveParam(bodyType, requestBody(isRequired));
		Mono<Object> result = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		Object value = result.block(Duration.ofSeconds(5));

		if (value != null) {
			assertTrue("Unexpected return value type: " + value,
					param.getParameterType().isAssignableFrom(value.getClass()));
		}

		//no inspection unchecked
		return (T) value;
	}

	private Predicate<MethodParameter> requestBody(boolean required) {
		return p -> {
			RequestBody annotation = p.getParameterAnnotation(RequestBody.class);
			return annotation != null && annotation.required() == required;
		};
	}


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
