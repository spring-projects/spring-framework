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
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link RequestParamMapMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestParamMapMethodArgumentResolverTests {

	private RequestParamMapMethodArgumentResolver resolver;

	private ServerWebExchange exchange;

	private MethodParameter paramMap;
	private MethodParameter paramMultiValueMap;
	private MethodParameter paramNamedMap;
	private MethodParameter paramMapWithoutAnnot;



	@Before
	public void setUp() throws Exception {
		this.resolver = new RequestParamMapMethodArgumentResolver();

		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		WebSessionManager sessionManager = mock(WebSessionManager.class);
		this.exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);

		Method method = getClass().getMethod("params", Map.class, MultiValueMap.class, Map.class, Map.class);
		this.paramMap = new SynthesizingMethodParameter(method, 0);
		this.paramMultiValueMap = new SynthesizingMethodParameter(method, 1);
		this.paramNamedMap = new SynthesizingMethodParameter(method, 2);
		this.paramMapWithoutAnnot = new SynthesizingMethodParameter(method, 3);
	}


	@Test
	public void supportsParameter() {
		assertTrue(this.resolver.supportsParameter(this.paramMap));
		assertTrue(this.resolver.supportsParameter(this.paramMultiValueMap));
		assertFalse(this.resolver.supportsParameter(this.paramNamedMap));
		assertFalse(this.resolver.supportsParameter(this.paramMapWithoutAnnot));
	}

	@Test
	public void resolveMapArgument() throws Exception {
		String name = "foo";
		String value = "bar";
		this.exchange.getRequest().getQueryParams().set(name, value);
		Map<String, String> expected = Collections.singletonMap(name, value);

		Mono<Object> mono = resolver.resolveArgument(paramMap, null, exchange);
		Object result = mono.block();

		assertTrue(result instanceof Map);
		assertEquals(expected, result);
	}

	@Test
	public void resolveMultiValueMapArgument() throws Exception {
		String name = "foo";
		String value1 = "bar";
		String value2 = "baz";
		this.exchange.getRequest().getQueryParams().put(name, Arrays.asList(value1, value2));

		MultiValueMap<String, String> expected = new LinkedMultiValueMap<>(1);
		expected.add(name, value1);
		expected.add(name, value2);

		Mono<Object> mono = this.resolver.resolveArgument(this.paramMultiValueMap, null, this.exchange);
		Object result = mono.block();

		assertTrue(result instanceof MultiValueMap);
		assertEquals(expected, result);
	}


	@SuppressWarnings("unused")
	public void params(@RequestParam Map<?, ?> param1,
					   @RequestParam MultiValueMap<?, ?> param2,
					   @RequestParam("name") Map<?, ?> param3,
					   Map<?, ?> param4) {
	}

}
