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
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RequestParamMapMethodArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class RequestParamMapMethodArgumentResolverTests {

	private RequestParamMapMethodArgumentResolver resolver;

	private MethodParameter paramMap;
	private MethodParameter paramMultiValueMap;
	private MethodParameter paramNamedMap;
	private MethodParameter paramMapWithoutAnnot;



	@Before
	public void setUp() throws Exception {
		this.resolver = new RequestParamMapMethodArgumentResolver();

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
	public void resolveMapArgumentWithQueryString() throws Exception {
		Object result= resolve(this.paramMap, exchangeWithQuery("foo=bar"));
		assertTrue(result instanceof Map);
		assertEquals(Collections.singletonMap("foo", "bar"), result);
	}

	@Test
	public void resolveMapArgumentWithFormData() throws Exception {
		Object result= resolve(this.paramMap, exchangeWithFormData("foo=bar"));
		assertTrue(result instanceof Map);
		assertEquals(Collections.singletonMap("foo", "bar"), result);
	}

	@Test
	public void resolveMultiValueMapArgument() throws Exception {
		ServerWebExchange exchange = exchangeWithQuery("foo=bar&foo=baz");
		Object result= resolve(this.paramMultiValueMap, exchange);

		assertTrue(result instanceof MultiValueMap);
		assertEquals(Collections.singletonMap("foo", Arrays.asList("bar", "baz")), result);
	}


	private ServerWebExchange exchangeWithQuery(String query) throws URISyntaxException {
		MockServerHttpRequest request = MockServerHttpRequest.get("/path?" + query).build();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse());
	}

	private ServerWebExchange exchangeWithFormData(String formData) throws URISyntaxException {
		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formData);
		return new DefaultServerWebExchange(request, new MockServerHttpResponse());
	}

	private Object resolve(MethodParameter parameter, ServerWebExchange exchange) {
		return this.resolver.resolveArgument(parameter, null, exchange).blockMillis(0);
	}


	@SuppressWarnings("unused")
	public void params(@RequestParam Map<?, ?> param1,
					   @RequestParam MultiValueMap<?, ?> param2,
					   @RequestParam("name") Map<?, ?> param3,
					   Map<?, ?> param4) {
	}

}
