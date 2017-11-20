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
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.core.ResolvableType.forClassWithGenerics;
import static org.springframework.web.method.MvcAnnotationPredicates.requestParam;

/**
 * Unit tests for {@link RequestParamMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestParamMethodArgumentResolverTests {

	private RequestParamMethodArgumentResolver resolver;

	private BindingContext bindContext;

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Before
	public void setup() throws Exception {

		ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		this.resolver = new RequestParamMethodArgumentResolver(null, adapterRegistry, true);

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultFormattingConversionService());
		this.bindContext = new BindingContext(initializer);
	}


	@Test
	public void supportsParameter() {

		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annot(requestParam().name("name")).arg(Map.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annot(requestParam().name("")).arg(Map.class);
		assertFalse(this.resolver.supportsParameter(param));

		param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annot(requestParam()).arg(String.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annot(requestParam().notRequired()).arg(String.class);
		assertTrue(this.resolver.supportsParameter(param));

	}

	@Test
	public void doesNotSupportParameterWithDefaultResolutionTurnedOff() {
		ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		this.resolver = new RequestParamMethodArgumentResolver(null, adapterRegistry, false);

		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		assertFalse(this.resolver.supportsParameter(param));
	}

	@Test
	public void doesNotSupportReactiveWrapper() {
		MethodParameter param;
		try {
			param = this.testMethod.annot(requestParam()).arg(Mono.class, String.class);
			this.resolver.supportsParameter(param);
			fail();
		}
		catch (IllegalStateException ex) {
			assertTrue("Unexpected error message:\n" + ex.getMessage(),
					ex.getMessage().startsWith(
							"RequestParamMethodArgumentResolver doesn't support reactive type wrapper"));
		}
		try {
			param = this.testMethod.annotNotPresent(RequestParam.class).arg(Mono.class, String.class);
			this.resolver.supportsParameter(param);
			fail();
		}
		catch (IllegalStateException ex) {
			assertTrue("Unexpected error message:\n" + ex.getMessage(),
					ex.getMessage().startsWith(
							"RequestParamMethodArgumentResolver doesn't support reactive type wrapper"));
		}
	}

	@Test
	public void resolveWithQueryString() throws Exception {
		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?name=foo"));
		assertEquals("foo", resolve(param, exchange));
	}

	@Test
	public void resolveStringArray() throws Exception {
		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
		MockServerHttpRequest request = MockServerHttpRequest.get("/path?name=foo&name=bar").build();
		Object result = resolve(param, MockServerWebExchange.from(request));
		assertTrue(result instanceof String[]);
		assertArrayEquals(new String[] {"foo", "bar"}, (String[]) result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		assertEquals("bar", resolve(param, MockServerWebExchange.from(MockServerHttpRequest.get("/"))));
	}

	@Test
	public void missingRequestParam() throws Exception {

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
		Mono<Object> mono = this.resolver.resolveArgument(param, this.bindContext, exchange);

		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	public void resolveSimpleTypeParam() throws Exception {
		MockServerHttpRequest request = MockServerHttpRequest.get("/path?stringNotAnnot=plainValue").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		Object result = resolve(param, exchange);
		assertEquals("plainValue", result);
	}

	@Test  // SPR-8561
	public void resolveSimpleTypeParamToNull() throws Exception {
		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		assertNull(resolve(param, MockServerWebExchange.from(MockServerHttpRequest.get("/"))));
	}

	@Test  // SPR-10180
	public void resolveEmptyValueToDefault() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?name="));
		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		Object result = resolve(param, exchange);
		assertEquals("bar", result);
	}

	@Test
	public void resolveEmptyValueWithoutDefault() throws Exception {
		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		MockServerHttpRequest request = MockServerHttpRequest.get("/path?stringNotAnnot=").build();
		assertEquals("", resolve(param, MockServerWebExchange.from(request)));
	}

	@Test
	public void resolveEmptyValueRequiredWithoutDefault() throws Exception {
		MethodParameter param = this.testMethod.annot(requestParam()).arg(String.class);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?name="));
		assertEquals("", resolve(param, exchange));
	}

	@Test
	public void resolveOptionalParamValue() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		MethodParameter param = this.testMethod.arg(forClassWithGenerics(Optional.class, Integer.class));
		Object result = resolve(param, exchange);
		assertEquals(Optional.empty(), result);

		exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?name=123"));
		result = resolve(param, exchange);

		assertEquals(Optional.class, result.getClass());
		Optional<?> value = (Optional<?>) result;
		assertTrue(value.isPresent());
		assertEquals(123, value.get());
	}


	private Object resolve(MethodParameter parameter, ServerWebExchange exchange) {
		return this.resolver.resolveArgument(parameter, this.bindContext, exchange).block(Duration.ZERO);
	}


	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	public void handle(
			@RequestParam(name = "name", defaultValue = "bar") String param1,
			@RequestParam("name") String[] param2,
			@RequestParam("name") Map<?, ?> param3,
			@RequestParam Map<?, ?> param4,
			String stringNotAnnot,
			Mono<String> monoStringNotAnnot,
			@RequestParam("name") String paramRequired,
			@RequestParam(name = "name", required = false) String paramNotRequired,
			@RequestParam("name") Optional<Integer> paramOptional,
			@RequestParam Mono<String> paramMono) {
	}

}
