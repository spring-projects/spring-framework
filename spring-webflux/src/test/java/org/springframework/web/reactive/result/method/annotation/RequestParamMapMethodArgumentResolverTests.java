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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.post;
import static org.springframework.web.method.MvcAnnotationPredicates.requestParam;

/**
 * Unit tests for {@link RequestParamMapMethodArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class RequestParamMapMethodArgumentResolverTests {

	private RequestParamMapMethodArgumentResolver resolver = new RequestParamMapMethodArgumentResolver(new ReactiveAdapterRegistry());

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	public void supportsParameter() {
		MethodParameter param = this.testMethod.annot(requestParam().name("")).arg(Map.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annotPresent(RequestParam.class).arg(MultiValueMap.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annot(requestParam().name("name")).arg(Map.class);
		assertFalse(this.resolver.supportsParameter(param));

		param = this.testMethod.annotNotPresent(RequestParam.class).arg(Map.class);
		assertFalse(this.resolver.supportsParameter(param));

		try {
			param = this.testMethod.annot(requestParam()).arg(Mono.class, Map.class);
			this.resolver.supportsParameter(param);
			fail();
		}
		catch (IllegalStateException ex) {
			assertTrue("Unexpected error message:\n" + ex.getMessage(),
					ex.getMessage().startsWith(
							"RequestParamMapMethodArgumentResolver doesn't support reactive type wrapper"));
		}
	}

	@Test
	public void resolveMapArgumentWithQueryString() throws Exception {
		MethodParameter param = this.testMethod.annot(requestParam().name("")).arg(Map.class);
		Object result= resolve(param, MockServerHttpRequest.get("/path?foo=bar").toExchange());
		assertTrue(result instanceof Map);
		assertEquals(Collections.singletonMap("foo", "bar"), result);
	}

	@Test
	public void resolveMapArgumentWithFormData() throws Exception {
		MethodParameter param = this.testMethod.annot(requestParam().name("")).arg(Map.class);
		ServerWebExchange exchange = post("/").contentType(APPLICATION_FORM_URLENCODED).body("foo=bar").toExchange();
		Object result= resolve(param, exchange);
		assertTrue(result instanceof Map);
		assertEquals(Collections.singletonMap("foo", "bar"), result);
	}

	@Test
	public void resolveMultiValueMapArgument() throws Exception {
		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultiValueMap.class);
		ServerWebExchange exchange = MockServerHttpRequest.get("/path?foo=bar&foo=baz").toExchange();
		Object result= resolve(param, exchange);

		assertTrue(result instanceof MultiValueMap);
		assertEquals(Collections.singletonMap("foo", Arrays.asList("bar", "baz")), result);
	}


	private Object resolve(MethodParameter parameter, ServerWebExchange exchange) {
		return this.resolver.resolveArgument(parameter, null, exchange).block(Duration.ofMillis(0));
	}


	public void handle(
			@RequestParam Map<?, ?> param1,
			@RequestParam MultiValueMap<?, ?> param2,
			@RequestParam("name") Map<?, ?> param3,
			Map<?, ?> param4,
			@RequestParam Mono<Map<?, ?>> paramMono) {
	}

}