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

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.ResolvableMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.*;
import static org.springframework.core.ResolvableType.*;

/**
 * Unit tests for {@link HttpEntityArgumentResolver}.When adding a test also
 * consider whether the logic under test is in a parent class, then see:
 * {@link MessageReaderArgumentResolverTests}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class HttpEntityArgumentResolverTests {

	private HttpEntityArgumentResolver resolver = createResolver();

	private MockServerHttpRequest request;

	private ResolvableMethod testMethod = ResolvableMethod.onClass(getClass()).name("handle");


	@Before
	public void setup() throws Exception {
		this.request = MockServerHttpRequest.post("/path").build();
	}

	private HttpEntityArgumentResolver createResolver() {
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		readers.add(new DecoderHttpMessageReader<>(new StringDecoder()));
		return new HttpEntityArgumentResolver(readers);
	}


	@Test
	public void supports() throws Exception {
		testSupports(httpEntityType(String.class));
		testSupports(httpEntityType(forClassWithGenerics(Mono.class, String.class)));
		testSupports(httpEntityType(forClassWithGenerics(Single.class, String.class)));
		testSupports(httpEntityType(forClassWithGenerics(io.reactivex.Single.class, String.class)));
		testSupports(httpEntityType(forClassWithGenerics(Maybe.class, String.class)));
		testSupports(httpEntityType(forClassWithGenerics(CompletableFuture.class, String.class)));
		testSupports(httpEntityType(forClassWithGenerics(Flux.class, String.class)));
		testSupports(httpEntityType(forClassWithGenerics(Observable.class, String.class)));
		testSupports(httpEntityType(forClassWithGenerics(io.reactivex.Observable.class, String.class)));
		testSupports(httpEntityType(forClassWithGenerics(Flowable.class, String.class)));
		testSupports(forClassWithGenerics(RequestEntity.class, String.class));
	}

	@Test
	public void doesNotSupport() throws Exception {
		ResolvableType type = ResolvableType.forClassWithGenerics(Mono.class, String.class);
		assertFalse(this.resolver.supportsParameter(this.testMethod.resolveParam(type)));

		type = ResolvableType.forClass(String.class);
		assertFalse(this.resolver.supportsParameter(this.testMethod.resolveParam(type)));
	}

	@Test
	public void emptyBodyWithString() throws Exception {
		ResolvableType type = httpEntityType(String.class);
		HttpEntity<Object> entity = resolveValueWithEmptyBody(type);

		assertNull(entity.getBody());
	}

	@Test
	public void emptyBodyWithMono() throws Exception {
		ResolvableType type = httpEntityType(forClassWithGenerics(Mono.class, String.class));
		HttpEntity<Mono<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody()).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void emptyBodyWithFlux() throws Exception {
		ResolvableType type = httpEntityType(forClassWithGenerics(Flux.class, String.class));
		HttpEntity<Flux<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody()).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void emptyBodyWithSingle() throws Exception {
		ResolvableType type = httpEntityType(forClassWithGenerics(Single.class, String.class));
		HttpEntity<Single<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(RxReactiveStreams.toPublisher(entity.getBody()))
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	public void emptyBodyWithRxJava2Single() throws Exception {
		ResolvableType type = httpEntityType(forClassWithGenerics(io.reactivex.Single.class, String.class));
		HttpEntity<io.reactivex.Single<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody().toFlowable())
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	public void emptyBodyWithRxJava2Maybe() throws Exception {
		ResolvableType type = httpEntityType(forClassWithGenerics(Maybe.class, String.class));
		HttpEntity<Maybe<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody().toFlowable())
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithObservable() throws Exception {
		ResolvableType type = httpEntityType(forClassWithGenerics(Observable.class, String.class));
		HttpEntity<Observable<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(RxReactiveStreams.toPublisher(entity.getBody()))
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithRxJava2Observable() throws Exception {
		ResolvableType type = httpEntityType(forClassWithGenerics(io.reactivex.Observable.class, String.class));
		HttpEntity<io.reactivex.Observable<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody().toFlowable(BackpressureStrategy.BUFFER))
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithFlowable() throws Exception {
		ResolvableType type = httpEntityType(forClassWithGenerics(Flowable.class, String.class));
		HttpEntity<Flowable<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody())
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithCompletableFuture() throws Exception {
		ResolvableType type = httpEntityType(forClassWithGenerics(CompletableFuture.class, String.class));
		HttpEntity<CompletableFuture<String>> entity = resolveValueWithEmptyBody(type);

		entity.getBody().whenComplete((body, ex) -> {
			assertNull(body);
			assertNull(ex);
		});
	}

	@Test
	public void httpEntityWithStringBody() throws Exception {
		String body = "line1";
		ResolvableType type = httpEntityType(String.class);
		HttpEntity<String> httpEntity = resolveValue(type, body);

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		assertEquals("line1", httpEntity.getBody());
	}

	@Test
	public void httpEntityWithMonoBody() throws Exception {
		String body = "line1";
		ResolvableType type = httpEntityType(forClassWithGenerics(Mono.class, String.class));
		HttpEntity<Mono<String>> httpEntity = resolveValue(type, body);

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		assertEquals("line1", httpEntity.getBody().block());
	}

	@Test
	public void httpEntityWithSingleBody() throws Exception {
		String body = "line1";
		ResolvableType type = httpEntityType(forClassWithGenerics(Single.class, String.class));
		HttpEntity<Single<String>> httpEntity = resolveValue(type, body);

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		assertEquals("line1", httpEntity.getBody().toBlocking().value());
	}

	@Test
	public void httpEntityWithRxJava2SingleBody() throws Exception {
		String body = "line1";
		ResolvableType type = httpEntityType(forClassWithGenerics(io.reactivex.Single.class, String.class));
		HttpEntity<io.reactivex.Single<String>> httpEntity = resolveValue(type, body);

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		assertEquals("line1", httpEntity.getBody().blockingGet());
	}

	@Test
	public void httpEntityWithRxJava2MaybeBody() throws Exception {
		String body = "line1";
		ResolvableType type = httpEntityType(forClassWithGenerics(Maybe.class, String.class));
		HttpEntity<Maybe<String>> httpEntity = resolveValue(type, body);

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		assertEquals("line1", httpEntity.getBody().blockingGet());
	}

	@Test
	public void httpEntityWithCompletableFutureBody() throws Exception {
		String body = "line1";
		ResolvableType type = httpEntityType(forClassWithGenerics(CompletableFuture.class, String.class));
		HttpEntity<CompletableFuture<String>> httpEntity = resolveValue(type, body);

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		assertEquals("line1", httpEntity.getBody().get());
	}

	@Test
	public void httpEntityWithFluxBody() throws Exception {
		String body = "line1\nline2\nline3\n";
		ResolvableType type = httpEntityType(forClassWithGenerics(Flux.class, String.class));
		HttpEntity<Flux<String>> httpEntity = resolveValue(type, body);

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		StepVerifier.create(httpEntity.getBody())
				.expectNext("line1\n")
				.expectNext("line2\n")
				.expectNext("line3\n")
				.expectComplete()
				.verify();
	}

	@Test
	public void requestEntity() throws Exception {
		String body = "line1";
		ResolvableType type = forClassWithGenerics(RequestEntity.class, String.class);
		RequestEntity<String> requestEntity = resolveValue(type, body);

		assertEquals(this.request.getMethod(), requestEntity.getMethod());
		assertEquals(this.request.getURI(), requestEntity.getUrl());
		assertEquals(this.request.getHeaders(), requestEntity.getHeaders());
		assertEquals("line1", requestEntity.getBody());
	}


	private ResolvableType httpEntityType(Class<?> bodyType) {
		return httpEntityType(ResolvableType.forClass(bodyType));
	}

	private ResolvableType httpEntityType(ResolvableType type) {
		return forClassWithGenerics(HttpEntity.class, type);
	}

	private void testSupports(ResolvableType type) {
		MethodParameter parameter = this.testMethod.resolveParam(type);
		assertTrue(this.resolver.supportsParameter(parameter));
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveValue(ResolvableType type, String body) {
		this.request = MockServerHttpRequest.post("/path").header("foo", "bar")
				.contentType(MediaType.TEXT_PLAIN)
				.body(body);
		ServerWebExchange exchange = new DefaultServerWebExchange(this.request, new MockServerHttpResponse());

		MethodParameter param = this.testMethod.resolveParam(type);
		Mono<Object> result = this.resolver.resolveArgument(param, new BindingContext(), exchange);
		Object value = result.block(Duration.ofSeconds(5));

		assertNotNull(value);
		assertTrue("Unexpected return value type: " + value.getClass(),
				param.getParameterType().isAssignableFrom(value.getClass()));

		return (T) value;
	}

	@SuppressWarnings("unchecked")
	private <T> HttpEntity<T> resolveValueWithEmptyBody(ResolvableType type) {
		ServerWebExchange exchange = new DefaultServerWebExchange(this.request, new MockServerHttpResponse());
		MethodParameter param = this.testMethod.resolveParam(type);
		Mono<Object> result = this.resolver.resolveArgument(param, new BindingContext(), exchange);
		HttpEntity<String> httpEntity = (HttpEntity<String>) result.block(Duration.ofSeconds(5));

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		return (HttpEntity<T>) httpEntity;
	}


	@SuppressWarnings("unused")
	void handle(
			String string,
			Mono<String> monoString,
			HttpEntity<String> httpEntity,
			HttpEntity<Mono<String>> monoBody,
			HttpEntity<Flux<String>> fluxBody,
			HttpEntity<Single<String>> singleBody,
			HttpEntity<io.reactivex.Single<String>> rxJava2SingleBody,
			HttpEntity<Maybe<String>> rxJava2MaybeBody,
			HttpEntity<Observable<String>> observableBody,
			HttpEntity<io.reactivex.Observable<String>> rxJava2ObservableBody,
			HttpEntity<Flowable<String>> flowableBody,
			HttpEntity<CompletableFuture<String>> completableFutureBody,
			RequestEntity<String> requestEntity) {}

}
