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

import java.lang.reflect.Method;
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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.test.TestSubscriber;
import rx.Observable;
import rx.Single;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.support.JacksonJsonDecoder;
import org.springframework.core.codec.support.JsonObjectDecoder;
import org.springframework.core.codec.support.Pojo;
import org.springframework.core.codec.support.StringDecoder;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.convert.support.ReactiveStreamsToCompletableFutureConverter;
import org.springframework.core.convert.support.ReactiveStreamsToRxJava1Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RequestBodyArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class RequestBodyArgumentResolverTests {

	private RequestBodyArgumentResolver resolver;

	private ServerWebExchange exchange;

	private MockServerHttpRequest request;

	private ModelMap model;


	@Before
	public void setUp() throws Exception {
		this.resolver = resolver(new JacksonJsonDecoder(new JsonObjectDecoder()));
		this.request = new MockServerHttpRequest(HttpMethod.GET, new URI("/path"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
		this.exchange = new DefaultServerWebExchange(this.request, response, sessionManager);
		this.model = new ExtendedModelMap();
	}


	@Test
	public void supports() throws Exception {
		RequestBodyArgumentResolver resolver = resolver(new StringDecoder());

		assertTrue(resolver.supportsParameter(parameter("monoPojo")));
		assertFalse(resolver.supportsParameter(parameter("paramWithoutAnnotation")));
	}

	@Test
	public void missingContentType() throws Exception {
		String body = "{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}";
		this.request.writeWith(Flux.just(dataBuffer(body)));
		Mono<Object> result = this.resolver.resolveArgument(parameter("monoPojo"), this.model, this.exchange);

		TestSubscriber.subscribe(result)
				.assertError(UnsupportedMediaTypeStatusException.class);
	}

	@Test @SuppressWarnings("unchecked")
	public void monoPojo() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		Mono<Pojo> mono = (Mono<Pojo>) resolve("monoPojo", Mono.class, body);
		assertEquals(new Pojo("f1", "b1"), mono.block());
	}

	@Test @SuppressWarnings("unchecked")
	public void fluxPojo() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		Flux<Pojo> flux = (Flux<Pojo>) resolve("fluxPojo", Flux.class, body);
		assertEquals(Arrays.asList(new Pojo("f1", "b1"), new Pojo("f2", "b2")), flux.collectList().block());
	}

	@Test @SuppressWarnings("unchecked")
	public void singlePojo() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		Single<Pojo> single = (Single<Pojo>) resolve("singlePojo", Single.class, body);
		assertEquals(new Pojo("f1", "b1"), single.toBlocking().value());
	}

	@Test @SuppressWarnings("unchecked")
	public void observablePojo() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		Observable<?> observable = (Observable<?>) resolve("observablePojo", Observable.class, body);
		assertEquals(Arrays.asList(new Pojo("f1", "b1"), new Pojo("f2", "b2")),
				observable.toList().toBlocking().first());
	}

	@Test @SuppressWarnings("unchecked")
	public void futurePojo() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		assertEquals(new Pojo("f1", "b1"), resolve("futurePojo", CompletableFuture.class, body).get());
	}

	@Test
	public void pojo() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		assertEquals(new Pojo("f1", "b1"), resolve("pojo", Pojo.class, body));
	}

	@Test
	public void map() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		Map<String, String> map = new HashMap<>();
		map.put("foo", "f1");
		map.put("bar", "b1");
		assertEquals(map, resolve("map", Map.class, body));
	}

	// TODO: @Ignore

	@Test
	@Ignore
	public void list() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		assertEquals(Arrays.asList(new Pojo("f1", "b1"), new Pojo("f2", "b2")),
				resolve("list", List.class, body));
	}

	@Test
	@Ignore
	public void array() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		assertArrayEquals(new Pojo[] {new Pojo("f1", "b1"), new Pojo("f2", "b2")},
				resolve("array", Pojo[].class, body));
	}


	@SuppressWarnings("unchecked")
	private <T> T resolve(String paramName, Class<T> valueType, String body) {
		this.request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		this.request.writeWith(Flux.just(dataBuffer(body)));
		Mono<Object> result = this.resolver.resolveArgument(parameter(paramName), this.model, this.exchange);
		Object value = result.block(Duration.ofSeconds(5));
		assertNotNull(value);
		assertTrue("Actual type: " + value.getClass(), valueType.isAssignableFrom(value.getClass()));
		return (T) value;
	}

	@SuppressWarnings("Convert2MethodRef")
	private RequestBodyArgumentResolver resolver(Decoder<?>... decoders) {
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		Arrays.asList(decoders).forEach(decoder -> converters.add(new CodecHttpMessageConverter<>(decoder)));
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new ReactiveStreamsToCompletableFutureConverter());
		service.addConverter(new ReactiveStreamsToRxJava1Converter());
		return new RequestBodyArgumentResolver(converters, service);
	}

	@SuppressWarnings("ConfusingArgumentToVarargsMethod")
	private MethodParameter parameter(String name) {
		ParameterNameDiscoverer nameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
		Method method = ReflectionUtils.findMethod(getClass(), "handle", (Class<?>[]) null);
		String[] names = nameDiscoverer.getParameterNames(method);
		for (int i=0; i < names.length; i++) {
			if (name.equals(names[i])) {
				return new SynthesizingMethodParameter(method, i);
			}
		}
		throw new IllegalArgumentException("Invalid parameter name '" + name + "'. Actual parameters: " +
				Arrays.toString(names));
	}

	private DataBuffer dataBuffer(String body) {
		byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		return new DefaultDataBufferFactory().wrap(byteBuffer);
	}


	@SuppressWarnings("unused")
	void handle(
			@RequestBody Mono<Pojo> monoPojo,
			@RequestBody Flux<Pojo> fluxPojo,
			@RequestBody Single<Pojo> singlePojo,
			@RequestBody Observable<Pojo> observablePojo,
			@RequestBody CompletableFuture<Pojo> futurePojo,
			@RequestBody Pojo pojo,
			@RequestBody Map<String, String> map,
			@RequestBody List<Pojo> list,
			@RequestBody Set<Pojo> set,
			@RequestBody Pojo[] array,
			Pojo paramWithoutAnnotation) {
	}

}
