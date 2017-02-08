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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;
import org.springframework.web.util.patterns.PathPattern;
import org.springframework.web.util.patterns.PathPatternParser;
import org.springframework.web.util.patterns.PatternComparatorConsideringPath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link AbstractHandlerMethodMapping}.
 * @author Rossen Stoyanchev
 */
public class HandlerMethodMappingTests {

	private PathPatternParser patternParser = new PathPatternParser();

	private AbstractHandlerMethodMapping<PathPattern> mapping;

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
		this.mapping.registerMapping(this.patternParser.parse("/foo"), this.handler, this.method1);
		this.mapping.registerMapping(this.patternParser.parse("/foo"), this.handler, this.method2);
	}

	@Test
	public void directMatch() throws Exception {
		String key = "foo";
		this.mapping.registerMapping(this.patternParser.parse(key), this.handler, this.method1);
		Mono<Object> result = this.mapping.getHandler(createExchange(HttpMethod.GET, key));

		assertEquals(this.method1, ((HandlerMethod) result.block()).getMethod());
	}

	@Test
	public void patternMatch() throws Exception {
		this.mapping.registerMapping(this.patternParser.parse("/fo*"), this.handler, this.method1);
		this.mapping.registerMapping(this.patternParser.parse("/f*"), this.handler, this.method2);

		Mono<Object> result = this.mapping.getHandler(createExchange(HttpMethod.GET, "/foo"));
		assertEquals(this.method1, ((HandlerMethod) result.block()).getMethod());
	}

	@Test
	public void ambiguousMatch() throws Exception {
		this.mapping.registerMapping(this.patternParser.parse("/f?o"), this.handler, this.method1);
		this.mapping.registerMapping(this.patternParser.parse("/fo?"), this.handler, this.method2);
		Mono<Object> result = this.mapping.getHandler(createExchange(HttpMethod.GET, "/foo"));

		StepVerifier.create(result).expectError(IllegalStateException.class).verify();
	}

	@Test
	public void registerMapping() throws Exception {
		PathPattern key1 = this.patternParser.parse("/foo");
		PathPattern key2 = this.patternParser.parse("/foo*");
		this.mapping.registerMapping(key1, this.handler, this.method1);
		this.mapping.registerMapping(key2, this.handler, this.method2);

		HandlerMethod match = this.mapping.getMappingRegistry().getMappings().get(key1);
		assertNotNull(match);
	}

	@Test
	public void registerMappingWithSameMethodAndTwoHandlerInstances() throws Exception {
		PathPattern key1 = this.patternParser.parse("/foo");
		PathPattern key2 = this.patternParser.parse("/bar");
		MyHandler handler1 = new MyHandler();
		MyHandler handler2 = new MyHandler();
		this.mapping.registerMapping(key1, handler1, this.method1);
		this.mapping.registerMapping(key2, handler2, this.method1);

		HandlerMethod match = this.mapping.getMappingRegistry().getMappings().get(key1);
		assertNotNull(match);
	}

	@Test
	public void unregisterMapping() throws Exception {
		String key = "foo";
		this.mapping.registerMapping(this.patternParser.parse(key), this.handler, this.method1);
		Mono<Object> result = this.mapping.getHandler(createExchange(HttpMethod.GET, key));

		assertNotNull(result.block());

		this.mapping.unregisterMapping(this.patternParser.parse(key));
		result = this.mapping.getHandler(createExchange(HttpMethod.GET, key));

		assertNull(result.block());
		assertNull(this.mapping.getMappingRegistry().getMappings().get(key));
	}


	private ServerWebExchange createExchange(HttpMethod httpMethod, String path) throws URISyntaxException {
		ServerHttpRequest request = MockServerHttpRequest.method(httpMethod, path).build();
		WebSessionManager sessionManager = new MockWebSessionManager();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}


	private static class MyHandlerMethodMapping extends AbstractHandlerMethodMapping<PathPattern> {

		private PathPatternParser patternParser = new PathPatternParser();

		@Override
		protected boolean isHandler(Class<?> beanType) {
			return true;
		}

		@Override
		protected PathPattern getMappingForMethod(Method method, Class<?> handlerType) {
			String methodName = method.getName();
			return methodName.startsWith("handler") ? this.patternParser.parse(methodName) : null;
		}

		@Override
		protected SortedSet<PathPattern> getMappingPathPatterns(PathPattern key) {
			TreeSet<PathPattern> patterns = new TreeSet<>();
			patterns.add(key);
			return patterns;
		}

		@Override
		protected PathPattern getMatchingMapping(PathPattern pattern, ServerWebExchange exchange) {
			String lookupPath = exchange.getRequest().getURI().getPath();
			return (pattern.matches(lookupPath) ? pattern : null);
		}

		@Override
		protected Comparator<PathPattern> getMappingComparator(ServerWebExchange exchange) {
			String lookupPath = exchange.getRequest().getURI().getPath();
			return new PatternComparatorConsideringPath(lookupPath);
		}

	}

	@Controller
	private static class MyHandler {

		@RequestMapping
		@SuppressWarnings("unused")
		public void handlerMethod1() {
		}

		@RequestMapping
		@SuppressWarnings("unused")
		public void handlerMethod2() {
		}
	}
}