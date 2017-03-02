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

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RequestParamMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestParamMethodArgumentResolverTests {

	private RequestParamMethodArgumentResolver resolver;

	private MethodParameter paramNamedDefaultValueString;
	private MethodParameter paramNamedStringArray;
	private MethodParameter paramNamedMap;
	private MethodParameter paramMap;
	private MethodParameter paramStringNotAnnot;
	private MethodParameter paramRequired;
	private MethodParameter paramNotRequired;
	private MethodParameter paramOptional;

	private BindingContext bindContext;


	@Before
	public void setup() throws Exception {
		this.resolver = new RequestParamMethodArgumentResolver(null, true);

		ParameterNameDiscoverer paramNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
		Method method = ReflectionUtils.findMethod(getClass(), "handle", (Class<?>[]) null);

		this.paramNamedDefaultValueString = new SynthesizingMethodParameter(method, 0);
		this.paramNamedStringArray = new SynthesizingMethodParameter(method, 1);
		this.paramNamedMap = new SynthesizingMethodParameter(method, 2);
		this.paramMap = new SynthesizingMethodParameter(method, 3);
		this.paramStringNotAnnot = new SynthesizingMethodParameter(method, 4);
		this.paramStringNotAnnot.initParameterNameDiscovery(paramNameDiscoverer);
		this.paramRequired = new SynthesizingMethodParameter(method, 5);
		this.paramNotRequired = new SynthesizingMethodParameter(method, 6);
		this.paramOptional = new SynthesizingMethodParameter(method, 7);

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultFormattingConversionService());
		this.bindContext = new BindingContext(initializer);
	}


	@Test
	public void supportsParameter() {
		this.resolver = new RequestParamMethodArgumentResolver(null, true);
		assertTrue(this.resolver.supportsParameter(this.paramNamedDefaultValueString));
		assertTrue(this.resolver.supportsParameter(this.paramNamedStringArray));
		assertTrue(this.resolver.supportsParameter(this.paramNamedMap));
		assertFalse(this.resolver.supportsParameter(this.paramMap));
		assertTrue(this.resolver.supportsParameter(this.paramStringNotAnnot));
		assertTrue(this.resolver.supportsParameter(this.paramRequired));
		assertTrue(this.resolver.supportsParameter(this.paramNotRequired));
		assertTrue(this.resolver.supportsParameter(this.paramOptional));

		this.resolver = new RequestParamMethodArgumentResolver(null, false);
		assertFalse(this.resolver.supportsParameter(this.paramStringNotAnnot));
	}

	@Test
	public void resolveWithQueryString() throws Exception {
		assertEquals("foo", resolve(this.paramNamedDefaultValueString, exchangeWithQuery("name=foo")));
	}

	@Test
	public void resolveWithFormData() throws Exception {
		assertEquals("foo", resolve(this.paramNamedDefaultValueString, exchangeWithFormData("name=foo")));
	}

	@Test
	public void resolveStringArray() throws Exception {
		Object result = resolve(this.paramNamedStringArray, exchangeWithQuery("name=foo&name=bar"));
		assertTrue(result instanceof String[]);
		assertArrayEquals(new String[] {"foo", "bar"}, (String[]) result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		Object result = resolve(this.paramNamedDefaultValueString, exchange());
		assertEquals("bar", result);
	}

	@Test
	public void missingRequestParam() throws Exception {

		Mono<Object> mono = this.resolver.resolveArgument(
				this.paramNamedStringArray, this.bindContext, exchange());

		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	public void resolveSimpleTypeParam() throws Exception {
		ServerWebExchange exchange = exchangeWithQuery("stringNotAnnot=plainValue");
		Object result = resolve(this.paramStringNotAnnot, exchange);
		assertEquals("plainValue", result);
	}

	@Test  // SPR-8561
	public void resolveSimpleTypeParamToNull() throws Exception {
		assertNull(resolve(this.paramStringNotAnnot, exchange()));
	}

	@Test  // SPR-10180
	public void resolveEmptyValueToDefault() throws Exception {
		ServerWebExchange exchange = exchangeWithQuery("name=");
		Object result = resolve(this.paramNamedDefaultValueString, exchange);
		assertEquals("bar", result);
	}

	@Test
	public void resolveEmptyValueWithoutDefault() throws Exception {
		assertEquals("", resolve(this.paramStringNotAnnot, exchangeWithQuery("stringNotAnnot=")));
	}

	@Test
	public void resolveEmptyValueRequiredWithoutDefault() throws Exception {
		assertEquals("", resolve(this.paramRequired, exchangeWithQuery("name=")));
	}

	@Test
	public void resolveOptionalParamValue() throws Exception {
		ServerWebExchange exchange = exchange();
		Object result = resolve(this.paramOptional, exchange);
		assertEquals(Optional.empty(), result);

		exchange = exchangeWithQuery("name=123");
		result = resolve(this.paramOptional, exchange);

		assertEquals(Optional.class, result.getClass());
		Optional<?> value = (Optional<?>) result;
		assertTrue(value.isPresent());
		assertEquals(123, value.get());
	}


	private ServerWebExchange exchangeWithQuery(String query) throws URISyntaxException {
		MockServerHttpRequest request = MockServerHttpRequest.get("/path?" + query).build();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse());
	}

	private ServerWebExchange exchangeWithFormData(String formData) throws URISyntaxException {
		MockServerHttpRequest request = MockServerHttpRequest.post("/path")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formData);
		return new DefaultServerWebExchange(request, new MockServerHttpResponse());
	}

	private ServerWebExchange exchange() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse());
	}

	private Object resolve(MethodParameter parameter, ServerWebExchange exchange) {
		return this.resolver.resolveArgument(parameter, this.bindContext, exchange).blockMillis(0);
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
			@RequestParam("name") Optional<Integer> paramOptional,
			@RequestParam Mono<String> paramMono) {
	}

}
