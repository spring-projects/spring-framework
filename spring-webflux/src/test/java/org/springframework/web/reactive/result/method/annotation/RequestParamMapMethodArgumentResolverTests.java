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

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.ResolvableMethod;
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

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Before
	public void setup() throws Exception {
		this.resolver = new RequestParamMapMethodArgumentResolver();
	}


	@Test
	public void supportsParameter() {
		MethodParameter param = this.testMethod.annotated(RequestParam.class, name("")).arg(Map.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class).arg(MultiValueMap.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class, name("name")).arg(Map.class);
		assertFalse(this.resolver.supportsParameter(param));

		param = this.testMethod.notAnnotated(RequestParam.class).arg(Map.class);
		assertFalse(this.resolver.supportsParameter(param));
	}

	@Test
	public void resolveMapArgumentWithQueryString() throws Exception {
		MethodParameter param = this.testMethod.annotated(RequestParam.class, name("")).arg(Map.class);
		Object result= resolve(param, exchangeWithQuery("foo=bar"));
		assertTrue(result instanceof Map);
		assertEquals(Collections.singletonMap("foo", "bar"), result);
	}

	@Test
	public void resolveMapArgumentWithFormData() throws Exception {
		MethodParameter param = this.testMethod.annotated(RequestParam.class, name("")).arg(Map.class);
		Object result= resolve(param, exchangeWithFormData("foo=bar"));
		assertTrue(result instanceof Map);
		assertEquals(Collections.singletonMap("foo", "bar"), result);
	}

	@Test
	public void resolveMultiValueMapArgument() throws Exception {
		MethodParameter param = this.testMethod.annotated(RequestParam.class).arg(MultiValueMap.class);
		ServerWebExchange exchange = exchangeWithQuery("foo=bar&foo=baz");
		Object result= resolve(param, exchange);

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

	private Predicate<RequestParam> name(String name) {
		return a -> name.equals(a.name());
	}


	@SuppressWarnings("unused")
	public void handle(
			@RequestParam Map<?, ?> param1,
			@RequestParam MultiValueMap<?, ?> param2,
			@RequestParam("name") Map<?, ?> param3,
			Map<?, ?> param4) {
	}

}