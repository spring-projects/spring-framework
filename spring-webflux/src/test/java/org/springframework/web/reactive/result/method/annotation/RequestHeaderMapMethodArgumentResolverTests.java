/*
 * Copyright 2002-2017 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestHeader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link RequestHeaderMapMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestHeaderMapMethodArgumentResolverTests {

	private RequestHeaderMapMethodArgumentResolver resolver;

	private MethodParameter paramMap;
	private MethodParameter paramMultiValueMap;
	private MethodParameter paramHttpHeaders;
	private MethodParameter paramUnsupported;
	private MethodParameter paramAlsoUnsupported;


	@Before
	public void setup() throws Exception {
		resolver = new RequestHeaderMapMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

		Method method = ReflectionUtils.findMethod(getClass(), "params", (Class<?>[]) null);
		paramMap = new SynthesizingMethodParameter(method, 0);
		paramMultiValueMap = new SynthesizingMethodParameter(method, 1);
		paramHttpHeaders = new SynthesizingMethodParameter(method, 2);
		paramUnsupported = new SynthesizingMethodParameter(method, 3);
		paramUnsupported = new SynthesizingMethodParameter(method, 3);
		paramAlsoUnsupported = new SynthesizingMethodParameter(method, 4);
	}


	@Test
	public void supportsParameter() {
		assertTrue("Map parameter not supported", resolver.supportsParameter(paramMap));
		assertTrue("MultiValueMap parameter not supported", resolver.supportsParameter(paramMultiValueMap));
		assertTrue("HttpHeaders parameter not supported", resolver.supportsParameter(paramHttpHeaders));
		assertFalse("non-@RequestParam map supported", resolver.supportsParameter(paramUnsupported));
		try {
			this.resolver.supportsParameter(this.paramAlsoUnsupported);
			fail();
		}
		catch (IllegalStateException ex) {
			assertTrue("Unexpected error message:\n" + ex.getMessage(),
					ex.getMessage().startsWith(
							"RequestHeaderMapMethodArgumentResolver doesn't support reactive type wrapper"));
		}
	}

	@Test
	public void resolveMapArgument() throws Exception {
		String name = "foo";
		String value = "bar";
		Map<String, String> expected = Collections.singletonMap(name, value);
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header(name, value).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		Mono<Object> mono = resolver.resolveArgument(paramMap, null, exchange);
		Object result = mono.block();

		assertTrue(result instanceof Map);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveMultiValueMapArgument() throws Exception {
		String name = "foo";
		String value1 = "bar";
		String value2 = "baz";
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header(name, value1, value2).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		MultiValueMap<String, String> expected = new LinkedMultiValueMap<>(1);
		expected.add(name, value1);
		expected.add(name, value2);

		Mono<Object> mono = resolver.resolveArgument(paramMultiValueMap, null, exchange);
		Object result = mono.block();

		assertTrue(result instanceof MultiValueMap);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveHttpHeadersArgument() throws Exception {
		String name = "foo";
		String value1 = "bar";
		String value2 = "baz";
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header(name, value1, value2).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		HttpHeaders expected = new HttpHeaders();
		expected.add(name, value1);
		expected.add(name, value2);

		Mono<Object> mono = resolver.resolveArgument(paramHttpHeaders, null, exchange);
		Object result = mono.block();

		assertTrue(result instanceof HttpHeaders);
		assertEquals("Invalid result", expected, result);
	}


	@SuppressWarnings("unused")
	public void params(
			@RequestHeader Map<?, ?> param1,
			@RequestHeader MultiValueMap<?, ?> param2,
			@RequestHeader HttpHeaders param3,
			Map<?,?> unsupported,
			@RequestHeader Mono<Map<?, ?>> alsoUnsupported) {
	}

}
