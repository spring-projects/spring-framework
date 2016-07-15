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

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import reactor.adapter.RxJava1Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.TestSubscriber;
import rx.Observable;
import rx.Single;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.convert.support.MonoToCompletableFutureConverter;
import org.springframework.core.convert.support.ReactorToRxJava1Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.result.ResolvableMethod;
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
 * {@link MessageConverterArgumentResolverTests}.
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
		this.request = new MockServerHttpRequest(HttpMethod.POST, new URI("/path"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(this.request, response, new MockWebSessionManager());
	}

	private RequestBodyArgumentResolver resolver() {
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new CodecHttpMessageConverter<>(new StringDecoder()));

		FormattingConversionService service = new DefaultFormattingConversionService();
		service.addConverter(new MonoToCompletableFutureConverter());
		service.addConverter(new ReactorToRxJava1Converter());

		return new RequestBodyArgumentResolver(converters, service);
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
	public void emptyBodyWithMono() throws Exception {
		ResolvableType type = forClassWithGenerics(Mono.class, String.class);

		TestSubscriber.subscribe(resolveValueWithEmptyBody(type, true))
				.assertNoValues()
				.assertError(ServerWebInputException.class);

		TestSubscriber.subscribe(resolveValueWithEmptyBody(type, false))
				.assertNoValues()
				.assertComplete();
	}

	@Test
	public void emptyBodyWithFlux() throws Exception {
		ResolvableType type = forClassWithGenerics(Flux.class, String.class);

		TestSubscriber.subscribe(resolveValueWithEmptyBody(type, true))
				.assertNoValues()
				.assertError(ServerWebInputException.class);

		TestSubscriber.subscribe(resolveValueWithEmptyBody(type, false))
				.assertNoValues()
				.assertComplete();
	}

	@Test
	public void emptyBodyWithSingle() throws Exception {
		ResolvableType type = forClassWithGenerics(Single.class, String.class);

		Single<String> single = resolveValueWithEmptyBody(type, true);
		TestSubscriber.subscribe(RxJava1Adapter.singleToMono(single))
				.assertNoValues()
				.assertError(ServerWebInputException.class);

		single = resolveValueWithEmptyBody(type, false);
		TestSubscriber.subscribe(RxJava1Adapter.singleToMono(single))
				.assertNoValues()
				.assertError(ServerWebInputException.class);
	}

	@Test
	public void emptyBodyWithObservable() throws Exception {
		ResolvableType type = forClassWithGenerics(Observable.class, String.class);

		Observable<String> observable = resolveValueWithEmptyBody(type, true);
		TestSubscriber.subscribe(RxJava1Adapter.observableToFlux(observable))
				.assertNoValues()
				.assertError(ServerWebInputException.class);

		observable = resolveValueWithEmptyBody(type, false);
		TestSubscriber.subscribe(RxJava1Adapter.observableToFlux(observable))
				.assertNoValues()
				.assertComplete();
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
		this.request.writeWith(Flux.just(dataBuffer(body)));
		Mono<Object> result = this.resolver.readBody(param, true, this.exchange);
		Object value = result.block(Duration.ofSeconds(5));

		assertNotNull(value);
		assertTrue("Unexpected return value type: " + value,
				param.getParameterType().isAssignableFrom(value.getClass()));

		//no inspection unchecked
		return (T) value;
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveValueWithEmptyBody(ResolvableType bodyType, boolean isRequired) {
		this.request.writeWith(Flux.empty());
		MethodParameter param = this.testMethod.resolveParam(bodyType, requestBody(isRequired));
		Mono<Object> result = this.resolver.resolveArgument(param, new ExtendedModelMap(), this.exchange);
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

	private DataBuffer dataBuffer(String body) {
		byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		return new DefaultDataBufferFactory().wrap(byteBuffer);
	}


	void handle(
			@RequestBody String string,
			@RequestBody Mono<String> mono,
			@RequestBody Flux<String> flux,
			@RequestBody Single<String> single,
			@RequestBody Observable<String> obs,
			@RequestBody CompletableFuture<String> future,
			@RequestBody(required = false) String stringNotRequired,
			@RequestBody(required = false) Mono<String> monoNotRequired,
			@RequestBody(required = false) Flux<String> fluxNotRequired,
			@RequestBody(required = false) Single<String> singleNotRequired,
			@RequestBody(required = false) Observable<String> obsNotRequired,
			@RequestBody(required = false) CompletableFuture<String> futureNotRequired,
			String notAnnotated) {}

}
