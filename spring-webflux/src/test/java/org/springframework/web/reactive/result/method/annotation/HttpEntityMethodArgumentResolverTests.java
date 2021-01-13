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
import java.util.concurrent.CompletableFuture;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.core.ResolvableType.forClassWithGenerics;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.post;

/**
 * Unit tests for {@link HttpEntityMethodArgumentResolver}.When adding a test also
 * consider whether the logic under test is in a parent class, then see:
 * {@link MessageReaderArgumentResolverTests}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class HttpEntityMethodArgumentResolverTests {

	private final HttpEntityMethodArgumentResolver resolver = createResolver();

	private final ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	private HttpEntityMethodArgumentResolver createResolver() {
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		readers.add(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		return new HttpEntityMethodArgumentResolver(readers, ReactiveAdapterRegistry.getSharedInstance());
	}


	@Test
	public void supports() throws Exception {
		testSupports(this.testMethod.arg(httpEntityType(String.class)));
		testSupports(this.testMethod.arg(httpEntityType(Mono.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(Single.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(Maybe.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(CompletableFuture.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(Flux.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(Observable.class, String.class)));
		testSupports(this.testMethod.arg(httpEntityType(Flowable.class, String.class)));
		testSupports(this.testMethod.arg(forClassWithGenerics(RequestEntity.class, String.class)));
	}

	private void testSupports(MethodParameter parameter) {
		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
	}

	@Test
	public void doesNotSupport() {
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(Mono.class, String.class))).isFalse();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(String.class))).isFalse();
		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.supportsParameter(this.testMethod.arg(Mono.class, httpEntityType(String.class))))
			.withMessageStartingWith("HttpEntityMethodArgumentResolver does not support reactive type wrapper");
	}

	@Test
	public void emptyBodyWithString() {
		ResolvableType type = httpEntityType(String.class);
		HttpEntity<Object> entity = resolveValueWithEmptyBody(type);

		assertThat(entity.getBody()).isNull();
	}

	@Test
	public void emptyBodyWithMono() {
		ResolvableType type = httpEntityType(Mono.class, String.class);
		HttpEntity<Mono<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody()).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void emptyBodyWithFlux() {
		ResolvableType type = httpEntityType(Flux.class, String.class);
		HttpEntity<Flux<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody()).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void emptyBodyWithSingle() {
		ResolvableType type = httpEntityType(Single.class, String.class);
		HttpEntity<Single<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody().toFlowable())
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	public void emptyBodyWithMaybe() {
		ResolvableType type = httpEntityType(Maybe.class, String.class);
		HttpEntity<Maybe<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody().toFlowable())
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithObservable() {
		ResolvableType type = httpEntityType(Observable.class, String.class);
		HttpEntity<Observable<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody().toFlowable(BackpressureStrategy.BUFFER))
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithFlowable() {
		ResolvableType type = httpEntityType(Flowable.class, String.class);
		HttpEntity<Flowable<String>> entity = resolveValueWithEmptyBody(type);

		StepVerifier.create(entity.getBody())
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void emptyBodyWithCompletableFuture() {
		ResolvableType type = httpEntityType(CompletableFuture.class, String.class);
		HttpEntity<CompletableFuture<String>> entity = resolveValueWithEmptyBody(type);

		entity.getBody().whenComplete((body, ex) -> {
			assertThat(body).isNull();
			assertThat(ex).isNull();
		});
	}

	@Test
	public void httpEntityWithStringBody() {
		ServerWebExchange exchange = postExchange("line1");
		ResolvableType type = httpEntityType(String.class);
		HttpEntity<String> httpEntity = resolveValue(exchange, type);

		assertThat(httpEntity.getHeaders()).isEqualTo(exchange.getRequest().getHeaders());
		assertThat(httpEntity.getBody()).isEqualTo("line1");
	}

	@Test
	public void httpEntityWithMonoBody() {
		ServerWebExchange exchange = postExchange("line1");
		ResolvableType type = httpEntityType(Mono.class, String.class);
		HttpEntity<Mono<String>> httpEntity = resolveValue(exchange, type);

		assertThat(httpEntity.getHeaders()).isEqualTo(exchange.getRequest().getHeaders());
		assertThat(httpEntity.getBody().block()).isEqualTo("line1");
	}

	@Test
	public void httpEntityWithSingleBody() {
		ServerWebExchange exchange = postExchange("line1");
		ResolvableType type = httpEntityType(Single.class, String.class);
		HttpEntity<Single<String>> httpEntity = resolveValue(exchange, type);

		assertThat(httpEntity.getHeaders()).isEqualTo(exchange.getRequest().getHeaders());
		assertThat(httpEntity.getBody().blockingGet()).isEqualTo("line1");
	}

	@Test
	public void httpEntityWithMaybeBody() {
		ServerWebExchange exchange = postExchange("line1");
		ResolvableType type = httpEntityType(Maybe.class, String.class);
		HttpEntity<Maybe<String>> httpEntity = resolveValue(exchange, type);

		assertThat(httpEntity.getHeaders()).isEqualTo(exchange.getRequest().getHeaders());
		assertThat(httpEntity.getBody().blockingGet()).isEqualTo("line1");
	}

	@Test
	public void httpEntityWithCompletableFutureBody() throws Exception {
		ServerWebExchange exchange = postExchange("line1");
		ResolvableType type = httpEntityType(CompletableFuture.class, String.class);
		HttpEntity<CompletableFuture<String>> httpEntity = resolveValue(exchange, type);

		assertThat(httpEntity.getHeaders()).isEqualTo(exchange.getRequest().getHeaders());
		assertThat(httpEntity.getBody().get()).isEqualTo("line1");
	}

	@Test
	public void httpEntityWithFluxBody() {
		ServerWebExchange exchange = postExchange("line1\nline2\nline3\n");
		ResolvableType type = httpEntityType(Flux.class, String.class);
		HttpEntity<Flux<String>> httpEntity = resolveValue(exchange, type);

		assertThat(httpEntity.getHeaders()).isEqualTo(exchange.getRequest().getHeaders());
		StepVerifier.create(httpEntity.getBody())
				.expectNext("line1")
				.expectNext("line2")
				.expectNext("line3")
				.expectComplete()
				.verify();
	}

	@Test
	public void requestEntity() {
		ServerWebExchange exchange = postExchange("line1");
		ResolvableType type = forClassWithGenerics(RequestEntity.class, String.class);
		RequestEntity<String> requestEntity = resolveValue(exchange, type);

		assertThat(requestEntity.getMethod()).isEqualTo(exchange.getRequest().getMethod());
		assertThat(requestEntity.getUrl()).isEqualTo(exchange.getRequest().getURI());
		assertThat(requestEntity.getHeaders()).isEqualTo(exchange.getRequest().getHeaders());
		assertThat(requestEntity.getBody()).isEqualTo("line1");
	}


	private MockServerWebExchange postExchange(String body) {
		return MockServerWebExchange.from(post("/path").header("foo", "bar").contentType(TEXT_PLAIN).body(body));
	}

	private ResolvableType httpEntityType(Class<?> bodyType, Class<?>... generics) {
		return ResolvableType.forClassWithGenerics(HttpEntity.class,
				ObjectUtils.isEmpty(generics) ?
						ResolvableType.forClass(bodyType) :
						ResolvableType.forClassWithGenerics(bodyType, generics));
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveValue(ServerWebExchange exchange, ResolvableType type) {
		MethodParameter param = this.testMethod.arg(type);
		Mono<Object> result = this.resolver.resolveArgument(param, new BindingContext(), exchange);
		Object value = result.block(Duration.ofSeconds(5));

		assertThat(value).isNotNull();
		assertThat(param.getParameterType().isAssignableFrom(value.getClass())).as("Unexpected return value type: " + value.getClass()).isTrue();

		return (T) value;
	}

	@SuppressWarnings("unchecked")
	private <T> HttpEntity<T> resolveValueWithEmptyBody(ResolvableType type) {
		ServerWebExchange exchange = MockServerWebExchange.from(post("/path"));
		MethodParameter param = this.testMethod.arg(type);
		Mono<Object> result = this.resolver.resolveArgument(param, new BindingContext(), exchange);
		HttpEntity<String> httpEntity = (HttpEntity<String>) result.block(Duration.ofSeconds(5));

		assertThat(httpEntity.getHeaders()).isEqualTo(exchange.getRequest().getHeaders());
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
			HttpEntity<Maybe<String>> maybeBody,
			HttpEntity<Observable<String>> observableBody,
			HttpEntity<Flowable<String>> flowableBody,
			HttpEntity<CompletableFuture<String>> completableFutureBody,
			RequestEntity<String> requestEntity,
			Mono<HttpEntity<String>> httpEntityMono) {}

}
