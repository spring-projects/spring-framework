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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.tests.TestSubscriber;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link AbstractHandlerMethodMapping}.
 * @author Rossen Stoyanchev
 */
public class HandlerMethodMappingTests {

	private AbstractHandlerMethodMapping<String> mapping;

	private MyHandler handler;

	private Method method1;

	private Method method2;


	@Before
	public void setUp() throws Exception {
		this.mapping = new MyHandlerMethodMapping();
		this.handler = new MyHandler();
		this.method1 = handler.getClass().getMethod("handlerMethod1");
		this.method2 = handler.getClass().getMethod("handlerMethod2");
	}


	@Test(expected = IllegalStateException.class)
	public void registerDuplicates() {
		this.mapping.registerMapping("foo", this.handler, this.method1);
		this.mapping.registerMapping("foo", this.handler, this.method2);
	}

	@Test
	public void directMatch() throws Exception {
		String key = "foo";
		this.mapping.registerMapping(key, this.handler, this.method1);
		Mono<Object> result = this.mapping.getHandler(createExchange(HttpMethod.GET, key));

		assertEquals(this.method1, ((HandlerMethod) result.block()).getMethod());
	}

	@Test
	public void patternMatch() throws Exception {
		this.mapping.registerMapping("/fo*", this.handler, this.method1);
		this.mapping.registerMapping("/f*", this.handler, this.method2);

		Mono<Object> result = this.mapping.getHandler(createExchange(HttpMethod.GET, "/foo"));
		assertEquals(this.method1, ((HandlerMethod) result.block()).getMethod());
	}

	@Test
	public void ambiguousMatch() throws Exception {
		this.mapping.registerMapping("/f?o", this.handler, this.method1);
		this.mapping.registerMapping("/fo?", this.handler, this.method2);
		Mono<Object> result = this.mapping.getHandler(createExchange(HttpMethod.GET, "/foo"));

		TestSubscriber.subscribe(result).assertError(IllegalStateException.class);
	}

	@Test
	public void registerMapping() throws Exception {
		String key1 = "/foo";
		String key2 = "/foo*";
		this.mapping.registerMapping(key1, this.handler, this.method1);
		this.mapping.registerMapping(key2, this.handler, this.method2);

		List directUrlMatches = this.mapping.getMappingRegistry().getMappingsByUrl(key1);

		assertNotNull(directUrlMatches);
		assertEquals(1, directUrlMatches.size());
		assertEquals(key1, directUrlMatches.get(0));
	}

	@Test
	public void registerMappingWithSameMethodAndTwoHandlerInstances() throws Exception {
		String key1 = "foo";
		String key2 = "bar";
		MyHandler handler1 = new MyHandler();
		MyHandler handler2 = new MyHandler();
		this.mapping.registerMapping(key1, handler1, this.method1);
		this.mapping.registerMapping(key2, handler2, this.method1);

		List directUrlMatches = this.mapping.getMappingRegistry().getMappingsByUrl(key1);

		assertNotNull(directUrlMatches);
		assertEquals(1, directUrlMatches.size());
		assertEquals(key1, directUrlMatches.get(0));
	}

	@Test
	public void unregisterMapping() throws Exception {
		String key = "foo";
		this.mapping.registerMapping(key, this.handler, this.method1);
		Mono<Object> result = this.mapping.getHandler(createExchange(HttpMethod.GET, key));

		assertNotNull(result.block());

		this.mapping.unregisterMapping(key);
		result = this.mapping.getHandler(createExchange(HttpMethod.GET, key));

		assertNull(result.block());
		assertNull(this.mapping.getMappingRegistry().getMappingsByUrl(key));
	}


	private ServerWebExchange createExchange(HttpMethod httpMethod, String path) throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(httpMethod, new URI(path));
		WebSessionManager sessionManager = new MockWebSessionManager();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}


	private static class MyHandlerMethodMapping extends AbstractHandlerMethodMapping<String> {

		private PathMatcher pathMatcher = new AntPathMatcher();

		@Override
		protected boolean isHandler(Class<?> beanType) {
			return true;
		}

		@Override
		protected String getMappingForMethod(Method method, Class<?> handlerType) {
			String methodName = method.getName();
			return methodName.startsWith("handler") ? methodName : null;
		}

		@Override
		protected Set<String> getMappingPathPatterns(String key) {
			return (this.pathMatcher.isPattern(key) ? Collections.emptySet() : Collections.singleton(key));
		}

		@Override
		protected String getMatchingMapping(String pattern, ServerWebExchange exchange) {
			String lookupPath = exchange.getRequest().getURI().getPath();
			return (this.pathMatcher.match(pattern, lookupPath) ? pattern : null);
		}

		@Override
		protected Comparator<String> getMappingComparator(ServerWebExchange exchange) {
			String lookupPath = exchange.getRequest().getURI().getPath();
			return this.pathMatcher.getPatternComparator(lookupPath);
		}

	}

	@Controller
	private static class MyHandler {

		@RequestMapping @SuppressWarnings("unused")
		public void handlerMethod1() {
		}

		@RequestMapping @SuppressWarnings("unused")
		public void handlerMethod2() {
		}
	}
}