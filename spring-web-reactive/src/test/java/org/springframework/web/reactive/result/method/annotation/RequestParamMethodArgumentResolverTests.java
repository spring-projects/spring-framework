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
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.core.test.TestSubscriber;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link RequestParamMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestParamMethodArgumentResolverTests {

	private RequestParamMethodArgumentResolver resolver;

	private ServerWebExchange exchange;

	private MethodParameter paramNamedDefaultValueString;
	private MethodParameter paramNamedStringArray;
	private MethodParameter paramNamedMap;
	private MethodParameter paramMap;
	private MethodParameter paramStringNotAnnot;
	private MethodParameter paramRequired;
	private MethodParameter paramNotRequired;
	private MethodParameter paramOptional;


	@Before @SuppressWarnings("ConfusingArgumentToVarargsMethod")
	public void setUp() throws Exception {
		ConversionService conversionService = new DefaultConversionService();
		this.resolver = new RequestParamMethodArgumentResolver(conversionService, null, true);

		ParameterNameDiscoverer paramNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
		Method method = ReflectionUtils.findMethod(getClass(), "handle", (Class<?>[]) null);

		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		WebSessionManager sessionManager = mock(WebSessionManager.class);
		this.exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);

		this.paramNamedDefaultValueString = new SynthesizingMethodParameter(method, 0);
		this.paramNamedStringArray = new SynthesizingMethodParameter(method, 1);
		this.paramNamedMap = new SynthesizingMethodParameter(method, 2);
		this.paramMap = new SynthesizingMethodParameter(method, 3);
		this.paramStringNotAnnot = new SynthesizingMethodParameter(method, 4);
		this.paramStringNotAnnot.initParameterNameDiscovery(paramNameDiscoverer);
		this.paramRequired = new SynthesizingMethodParameter(method, 5);
		this.paramNotRequired = new SynthesizingMethodParameter(method, 6);
		this.paramOptional = new SynthesizingMethodParameter(method, 7);
	}


	@Test
	public void supportsParameter() {
		this.resolver = new RequestParamMethodArgumentResolver(new GenericConversionService(), null, true);
		assertTrue(this.resolver.supportsParameter(this.paramNamedDefaultValueString));
		assertTrue(this.resolver.supportsParameter(this.paramNamedStringArray));
		assertTrue(this.resolver.supportsParameter(this.paramNamedMap));
		assertFalse(this.resolver.supportsParameter(this.paramMap));
		assertTrue(this.resolver.supportsParameter(this.paramStringNotAnnot));
		assertTrue(this.resolver.supportsParameter(this.paramRequired));
		assertTrue(this.resolver.supportsParameter(this.paramNotRequired));
		assertTrue(this.resolver.supportsParameter(this.paramOptional));

		this.resolver = new RequestParamMethodArgumentResolver(new GenericConversionService(), null, false);
		assertFalse(this.resolver.supportsParameter(this.paramStringNotAnnot));
	}

	@Test
	public void resolveString() throws Exception {
		String expected = "foo";
		this.exchange.getRequest().getQueryParams().set("name", expected);

		Mono<Object> mono = this.resolver.resolveArgument(this.paramNamedDefaultValueString, null, this.exchange);
		Object result = mono.block();

		assertTrue(result instanceof String);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveStringArray() throws Exception {
		String[] expected = {"foo", "bar"};
		this.exchange.getRequest().getQueryParams().put("name", Arrays.asList(expected));

		Mono<Object> mono = this.resolver.resolveArgument(this.paramNamedStringArray, null, this.exchange);
		Object result = mono.block();

		assertTrue(result instanceof String[]);
		assertArrayEquals(expected, (String[]) result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		Mono<Object> mono = this.resolver.resolveArgument(paramNamedDefaultValueString, null, this.exchange);
		Object result = mono.block();

		assertTrue(result instanceof String);
		assertEquals("Invalid result", "bar", result);
	}

	@Test
	public void missingRequestParam() throws Exception {
		Mono<Object> mono = this.resolver.resolveArgument(paramNamedStringArray, null, this.exchange);
		TestSubscriber
				.subscribe(mono)
				.assertError(ServerWebInputException.class);
	}

	@Test
	public void resolveSimpleTypeParam() throws Exception {
		this.exchange.getRequest().getQueryParams().set("stringNotAnnot", "plainValue");
		Mono<Object> mono = this.resolver.resolveArgument(paramStringNotAnnot, null, this.exchange);
		Object result = mono.block();

		assertTrue(result instanceof String);
		assertEquals("plainValue", result);
	}

	@Test  // SPR-8561
	public void resolveSimpleTypeParamToNull() throws Exception {
		Mono<Object> mono = this.resolver.resolveArgument(paramStringNotAnnot, null, this.exchange);
		Object result = mono.block();

		assertNull(result);
	}

	@Test  // SPR-10180
	public void resolveEmptyValueToDefault() throws Exception {
		this.exchange.getRequest().getQueryParams().set("name", "");
		Mono<Object> mono = this.resolver.resolveArgument(paramNamedDefaultValueString, null, this.exchange);
		Object result = mono.block();

		assertEquals("bar", result);
	}

	@Test
	public void resolveEmptyValueWithoutDefault() throws Exception {
		this.exchange.getRequest().getQueryParams().set("stringNotAnnot", "");
		Mono<Object> mono = this.resolver.resolveArgument(paramStringNotAnnot, null, this.exchange);
		Object result = mono.block();

		assertEquals("", result);
	}

	@Test
	public void resolveEmptyValueRequiredWithoutDefault() throws Exception {
		this.exchange.getRequest().getQueryParams().set("name", "");
		Mono<Object> mono = this.resolver.resolveArgument(paramRequired, null, this.exchange);
		Object result = mono.block();

		assertEquals("", result);
	}

	@Test
	public void resolveOptionalParamValue() throws Exception {
		Mono<Object> mono = this.resolver.resolveArgument(paramOptional, null, this.exchange);
		Object result = mono.block();

		assertEquals(Optional.empty(), result);

		this.exchange.getRequest().getQueryParams().set("name", "123");
		mono = resolver.resolveArgument(paramOptional, null, this.exchange);
		result = mono.block();

		assertEquals(Optional.class, result.getClass());
		Optional<?> value = (Optional<?>) result;
		assertTrue(value.isPresent());
		assertEquals(123, value.get());
	}


	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	public void handle(
			@RequestParam(name = "name", defaultValue = "bar") String param1,
			@RequestParam("name") String[] param2,
			@RequestParam("name") Map<?, ?> param3,
			@RequestParam Map<?, ?> param4,
			String stringNotAnnot,
			@RequestParam("name") String paramRequired,
			@RequestParam(name = "name", required = false) String paramNotRequired,
			@RequestParam("name") Optional<Integer> paramOptional) {
	}

}
