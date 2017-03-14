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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link PathVariableMapMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class PathVariableMapMethodArgumentResolverTests {

	private PathVariableMapMethodArgumentResolver resolver;

	private ServerWebExchange exchange;

	private MethodParameter paramMap;
	private MethodParameter paramNamedMap;
	private MethodParameter paramMapNoAnnot;
	private MethodParameter paramMonoMap;


	@Before
	public void setup() throws Exception {
		this.resolver = new PathVariableMapMethodArgumentResolver(new ReactiveAdapterRegistry());

		ServerHttpRequest request = MockServerHttpRequest.get("/").build();
		this.exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse());

		Method method = ReflectionUtils.findMethod(getClass(), "handle", (Class<?>[]) null);
		this.paramMap = new MethodParameter(method, 0);
		this.paramNamedMap = new MethodParameter(method, 1);
		this.paramMapNoAnnot = new MethodParameter(method, 2);
		this.paramMonoMap = new MethodParameter(method, 3);
	}


	@Test
	public void supportsParameter() {
		assertTrue(resolver.supportsParameter(paramMap));
		assertFalse(resolver.supportsParameter(paramNamedMap));
		assertFalse(resolver.supportsParameter(paramMapNoAnnot));
		try {
			this.resolver.supportsParameter(this.paramMonoMap);
			fail();
		}
		catch (IllegalStateException ex) {
			assertTrue("Unexpected error message:\n" + ex.getMessage(),
					ex.getMessage().startsWith(
							"PathVariableMapMethodArgumentResolver doesn't support reactive type wrapper"));
		}
	}

	@Test
	public void resolveArgument() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name1", "value1");
		uriTemplateVars.put("name2", "value2");
		this.exchange.getAttributes().put(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		Mono<Object> mono = this.resolver.resolveArgument(this.paramMap, new BindingContext(), this.exchange);
		Object result = mono.block();

		assertEquals(uriTemplateVars, result);
	}

	@Test
	public void resolveArgumentNoUriVars() throws Exception {
		Mono<Object> mono = this.resolver.resolveArgument(this.paramMap, new BindingContext(), this.exchange);
		Object result = mono.block();

		assertEquals(Collections.emptyMap(), result);
	}


	@SuppressWarnings("unused")
	public void handle(
			@PathVariable Map<String, String> map,
			@PathVariable(value = "name") Map<String, String> namedMap,
			Map<String, String> mapWithoutAnnotat,
			@PathVariable Mono<Map<?, ?>> monoMap) {
	}

}
