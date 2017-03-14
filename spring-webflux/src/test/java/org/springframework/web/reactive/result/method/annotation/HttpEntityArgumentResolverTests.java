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
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.ObjectUtils;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.core.ResolvableType.forClassWithGenerics;

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

	private final ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Before
	public void setup() throws Exception {
		this.request = MockServerHttpRequest.post("/path").build();
	}

	private HttpEntityArgumentResolver createResolver() {
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		readers.add(new DecoderHttpMessageReader<>(new StringDecoder()));
		return new HttpEntityArgumentResolver(readers, new ReactiveAdapterRegistry());
	}


	@Test
	public void supports() throws Exception {
		testSupports(this.testMethod.arg(httpEntityType(String.class)));
		testSupports(this.testMethod.arg(httpEntityType(Mono.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(Single.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(io.reactivex.Single.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(Maybe.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(CompletableFuture.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(Flux.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(Observable.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(io.reactivex.Observable.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(Flowable.class, String.class)));
		testSupports(this.testMethod.arg(forClassWithGenerics(RequestEntity.class, String.class)));
	}

	private void testSupports(MethodParameter parameter) {
		assertTrue(this.resolver.supportsParameter(parameter));
	}

	@Test
	public void doesNotSupport() throws Exception {
		assertFalse(this.resolver.supportsParameter(this.testMethod.arg(Mono.class, String.class)));
		assertFalse(this.resolver.supportsParameter(this.testMethod.arg(String.class)));
		try {
			this.resolver.supportsParameter(this.testMethod.arg(Mono.class, httpEntityType(String.class)));
			fail();
		}
		catch (IllegalStateException ex) {
			assertTrue("Unexpected error message:\n" + ex.getMessage(),
					ex.getMessage().startsWith(
							"HttpEntityArgumentResolver doesn't support reactive type wrapper"));
		}
	}

	@Test
	public void emptyBodyWithString() throws Exception {
		ResolvableType type = httpEntityType(String.class);
		HttpEntity<Object> entity = resolveValueWithEmptyBody(type);

		assertNull(entity.getBody());
	}

	@Test
	public void emptyBodyWithMono() throws Exception {
		ResolvableType type = httpEntityType(Mono.class, String.class);
		HttpEntity<Mono<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody()).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void emptyBodyWithFlux() throws Exception {
		ResolvableType type = httpEntityType(Flux.class, String.class);
		HttpEntity<Flux<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody()).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void emptyBodyWithSingle() throws Exception {
		ResolvableType type = httpEntityType(Single.class, String.class);
		HttpEntity<Single<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(RxReactiveStreams.toPublisher(entity.getBody()))
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	public void emptyBodyWithRxJava2Single() throws Exception {
		ResolvableType type = httpEntityType(io.reactivex.Single.class, String.class);
		HttpEntity<io.reactivex.Single<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody().toFlowable())
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	public void emptyBodyWithRxJava2Maybe() throws Exception {
		ResolvableType type = httpEntityType(Maybe.class, String.class);
		HttpEntity<Maybe<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody().toFlowable())
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithObservable() throws Exception {
		ResolvableType type = httpEntityType(Observable.class, String.class);
		HttpEntity<Observable<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(RxReactiveStreams.toPublisher(entity.getBody()))
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithRxJava2Observable() throws Exception {
		ResolvableType type = httpEntityType(io.reactivex.Observable.class, String.class);
		HttpEntity<io.reactivex.Observable<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody().toFlowable(BackpressureStrategy.BUFFER))
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithFlowable() throws Exception {
		ResolvableType type = httpEntityType(Flowable.class, String.class);
		HttpEntity<Flowable<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody())
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithCompletableFuture() throws Exception {
		ResolvableType type = httpEntityType(CompletableFuture.class, String.class);
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
		ResolvableType type = httpEntityType(Mono.class, String.class);
		HttpEntity<Mono<String>> httpEntity = resolveValue(type, body);

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		assertEquals("line1", httpEntity.getBody().block());
	}

	@Test
	public void httpEntityWithSingleBody() throws Exception {
		String body = "line1";
		ResolvableType type = httpEntityType(Single.class, String.class);
		HttpEntity<Single<String>> httpEntity = resolveValue(type, body);

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		assertEquals("line1", httpEntity.getBody().toBlocking().value());
	}

	@Test
	public void httpEntityWithRxJava2SingleBody() throws Exception {
		String body = "line1";
		ResolvableType type = httpEntityType(io.reactivex.Single.class, String.class);
		HttpEntity<io.reactivex.Single<String>> httpEntity = resolveValue(type, body);

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		assertEquals("line1", httpEntity.getBody().blockingGet());
	}

	@Test
	public void httpEntityWithRxJava2MaybeBody() throws Exception {
		String body = "line1";
		ResolvableType type = httpEntityType(Maybe.class, String.class);
		HttpEntity<Maybe<String>> httpEntity = resolveValue(type, body);

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		assertEquals("line1", httpEntity.getBody().blockingGet());
	}

	@Test
	public void httpEntityWithCompletableFutureBody() throws Exception {
		String body = "line1";
		ResolvableType type = httpEntityType(CompletableFuture.class, String.class);
		HttpEntity<CompletableFuture<String>> httpEntity = resolveValue(type, body);

		assertEquals(this.request.getHeaders(), httpEntity.getHeaders());
		assertEquals("line1", httpEntity.getBody().get());
	}

	@Test
	public void httpEntityWithFluxBody() throws Exception {
		String body = "line1\nline2\nline3\n";
		ResolvableType type = httpEntityType(Flux.class, String.class);
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


	private ResolvableType httpEntityType(Class<?> bodyType, Class<?>... generics) {
		return ResolvableType.forClassWithGenerics(HttpEntity.class,
				ObjectUtils.isEmpty(generics) ?
						ResolvableType.forClass(bodyType) :
						ResolvableType.forClassWithGenerics(bodyType, generics));
	}


	@SuppressWarnings("unchecked")
	private <T> T resolveValue(ResolvableType type, String body) {
		this.request = MockServerHttpRequest.post("/path").header("foo", "bar")
				.contentType(MediaType.TEXT_PLAIN)
				.body(body);
		ServerWebExchange exchange = new DefaultServerWebExchange(this.request, new MockServerHttpResponse());

		MethodParameter param = this.testMethod.arg(type);
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
		MethodParameter param = this.testMethod.arg(type);
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
			RequestEntity<String> requestEntity,
			Mono<HttpEntity<String>> httpEntityMono) {}

}
