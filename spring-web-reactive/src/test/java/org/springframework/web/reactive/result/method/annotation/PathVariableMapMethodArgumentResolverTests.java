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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

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


	@Before
	public void setUp() throws Exception {
		this.resolver = new PathVariableMapMethodArgumentResolver();

		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		WebSessionManager sessionManager = mock(WebSessionManager.class);
		this.exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);

		Method method = getClass().getMethod("handle", Map.class, Map.class, Map.class);
		this.paramMap = new MethodParameter(method, 0);
		this.paramNamedMap = new MethodParameter(method, 1);
		this.paramMapNoAnnot = new MethodParameter(method, 2);
	}

	@Test
	public void supportsParameter() {
		assertTrue(resolver.supportsParameter(paramMap));
		assertFalse(resolver.supportsParameter(paramNamedMap));
		assertFalse(resolver.supportsParameter(paramMapNoAnnot));
	}

	@Test
	public void resolveArgument() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name1", "value1");
		uriTemplateVars.put("name2", "value2");
		this.exchange.getAttributes().put(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		Mono<Object> mono = this.resolver.resolveArgument(this.paramMap, new ModelMap(), this.exchange);
		Object result = mono.block();

		assertEquals(uriTemplateVars, result);
	}

	@Test
	public void resolveArgumentNoUriVars() throws Exception {
		Mono<Object> mono = this.resolver.resolveArgument(this.paramMap, new ModelMap(), this.exchange);
		Object result = mono.block();

		assertEquals(Collections.emptyMap(), result);
	}


	@SuppressWarnings("unused")
	public void handle(
			@PathVariable Map<String, String> map,
			@PathVariable(value = "name") Map<String, String> namedMap,
			Map<String, String> mapWithoutAnnotat) {
	}

}