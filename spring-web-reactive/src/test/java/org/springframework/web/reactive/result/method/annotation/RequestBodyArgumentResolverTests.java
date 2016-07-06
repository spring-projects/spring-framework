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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.test.TestSubscriber;
import rx.Observable;
import rx.Single;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.convert.support.MonoToCompletableFutureConverter;
import org.springframework.core.convert.support.ReactorToRxJava1Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.result.ResolvableMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.core.ResolvableType.forClassWithGenerics;

/**
 * Unit tests for {@link RequestBodyArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class RequestBodyArgumentResolverTests {

	private RequestBodyArgumentResolver resolver = resolver(new JacksonJsonDecoder());

	private ServerWebExchange exchange;

	private MockServerHttpRequest request;

	private ResolvableMethod testMethod = ResolvableMethod.on(this.getClass()).name("handle");


	@Before
	public void setUp() throws Exception {
		this.request = new MockServerHttpRequest(HttpMethod.GET, new URI("/path"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(this.request, response, new MockWebSessionManager());
	}


	@Test
	public void supports() throws Exception {
		RequestBodyArgumentResolver resolver = resolver(new StringDecoder());

		ResolvableType type = forClassWithGenerics(Mono.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		assertTrue(resolver.supportsParameter(param));

		MethodParameter parameter = this.testMethod.resolveParam(p -> !p.hasParameterAnnotations());
		assertFalse(resolver.supportsParameter(parameter));
	}

	@Test
	public void missingContentType() throws Exception {
		String body = "{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}";
		this.request.writeWith(Flux.just(dataBuffer(body)));
		ResolvableType type = forClassWithGenerics(Mono.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Mono<Object> result = this.resolver.resolveArgument(param, new ExtendedModelMap(), this.exchange);

		TestSubscriber.subscribe(result)
				.assertError(UnsupportedMediaTypeStatusException.class);
	}

	@Test @SuppressWarnings("unchecked")
	public void monoTestBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		ResolvableType type = forClassWithGenerics(Mono.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Mono<TestBean> mono = (Mono<TestBean>) resolveValue(param, Mono.class, body);

		assertEquals(new TestBean("f1", "b1"), mono.block());
	}

	@Test @SuppressWarnings("unchecked")
	public void fluxTestBean() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(Flux.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Flux<TestBean> flux = (Flux<TestBean>) resolveValue(param, Flux.class, body);

		assertEquals(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")),
				flux.collectList().block());
	}

	@Test @SuppressWarnings("unchecked")
	public void singleTestBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		ResolvableType type = forClassWithGenerics(Single.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Single<TestBean> single = (Single<TestBean>) resolveValue(param, Single.class, body);

		assertEquals(new TestBean("f1", "b1"), single.toBlocking().value());
	}

	@Test @SuppressWarnings("unchecked")
	public void observableTestBean() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(Observable.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Observable<?> observable = (Observable<?>) resolveValue(param, Observable.class, body);

		assertEquals(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")),
				observable.toList().toBlocking().first());
	}

	@Test @SuppressWarnings("unchecked")
	public void futureTestBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		ResolvableType type = forClassWithGenerics(CompletableFuture.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		CompletableFuture future = resolveValue(param, CompletableFuture.class, body);

		assertEquals(new TestBean("f1", "b1"), future.get());
	}

	@Test
	public void testBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		MethodParameter param = this.testMethod.resolveParam(
				forClass(TestBean.class), p -> p.hasParameterAnnotation(RequestBody.class));
		TestBean value = resolveValue(param, TestBean.class, body);

		assertEquals(new TestBean("f1", "b1"), value);
	}

	@Test
	public void map() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		Map<String, String> map = new HashMap<>();
		map.put("foo", "f1");
		map.put("bar", "b1");
		ResolvableType type = forClassWithGenerics(Map.class, String.class, String.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Map actual = resolveValue(param, Map.class, body);

		assertEquals(map, actual);
	}

	@Test
	public void list() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(List.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		List<?> list = resolveValue(param, List.class, body);

		assertEquals(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")), list);
	}

	@Test
	public void array() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClass(TestBean[].class);
		MethodParameter param = this.testMethod.resolveParam(type);
		TestBean[] value = resolveValue(param, TestBean[].class, body);

		assertArrayEquals(new TestBean[] {new TestBean("f1", "b1"), new TestBean("f2", "b2")}, value);
	}

	@Test @SuppressWarnings("unchecked")
	public void validateMonoTestBean() throws Exception {
		String body = "{\"bar\":\"b1\"}";
		ResolvableType type = forClassWithGenerics(Mono.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Mono<TestBean> mono = resolveValue(param, Mono.class, body);

		TestSubscriber.subscribe(mono)
				.assertNoValues()
				.assertError(ServerWebInputException.class);
	}

	@Test @SuppressWarnings("unchecked")
	public void validateFluxTestBean() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\"}]";
		ResolvableType type = forClassWithGenerics(Flux.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Flux<TestBean> flux = resolveValue(param, Flux.class, body);

		TestSubscriber.subscribe(flux)
				.assertValues(new TestBean("f1", "b1"))
				.assertError(ServerWebInputException.class);
	}


	@SuppressWarnings("unchecked")
	private <T> T resolveValue(MethodParameter param, Class<T> valueType, String body) {

		this.request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		this.request.writeWith(Flux.just(dataBuffer(body)));

		Mono<Object> result = this.resolver.resolveArgument(param, new ExtendedModelMap(), this.exchange);
		Object value = result.block(Duration.ofSeconds(5));

		assertNotNull(value);
		assertTrue("Actual type: " + value.getClass(), valueType.isAssignableFrom(value.getClass()));

		return (T) value;
	}

	@SuppressWarnings("Convert2MethodRef")
	private RequestBodyArgumentResolver resolver(Decoder<?>... decoders) {

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		Arrays.asList(decoders).forEach(decoder -> converters.add(new CodecHttpMessageConverter<>(decoder)));

		FormattingConversionService service = new DefaultFormattingConversionService();
		service.addConverter(new MonoToCompletableFutureConverter());
		service.addConverter(new ReactorToRxJava1Converter());

		return new RequestBodyArgumentResolver(converters, service, new TestBeanValidator());
	}

	private DataBuffer dataBuffer(String body) {
		byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		return new DefaultDataBufferFactory().wrap(byteBuffer);
	}


	@SuppressWarnings("unused")
	void handle(
			@Validated @RequestBody Mono<TestBean> monoTestBean,
			@Validated @RequestBody Flux<TestBean> fluxTestBean,
			@RequestBody Single<TestBean> singleTestBean,
			@RequestBody Observable<TestBean> observableTestBean,
			@RequestBody CompletableFuture<TestBean> futureTestBean,
			@RequestBody TestBean testBean,
			@RequestBody Map<String, String> map,
			@RequestBody List<TestBean> list,
			@RequestBody Set<TestBean> set,
			@RequestBody TestBean[] array,
			TestBean paramWithoutAnnotation) {
	}


	@XmlRootElement
	private static class TestBean {

		private String foo;

		private String bar;

		@SuppressWarnings("unused")
		public TestBean() {
		}

		TestBean(String foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof TestBean) {
				TestBean other = (TestBean) o;
				return this.foo.equals(other.foo) && this.bar.equals(other.bar);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return 31 * foo.hashCode() + bar.hashCode();
		}

		@Override
		public String toString() {
			return "TestBean[foo='" + this.foo + "\'" + ", bar='" + this.bar + "\']";
		}
	}

	private static class TestBeanValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.equals(TestBean.class);
		}

		@Override
		public void validate(Object target, Errors errors) {
			TestBean testBean = (TestBean) target;
			if (testBean.getFoo() == null) {
				errors.rejectValue("foo", "nullValue");
			}
		}
	}
}
