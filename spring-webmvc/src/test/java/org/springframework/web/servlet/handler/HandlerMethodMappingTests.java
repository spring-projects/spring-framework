/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.handler;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.UrlPathHelper;

/**
 * Test for {@link AbstractHandlerMethodMapping}.
 *
 * @author Arjen Poutsma
 */
public class HandlerMethodMappingTests {

	private AbstractHandlerMethodMapping<String> mapping;

	private MyHandler handler;

	private Method method1;

	private Method method2;

	@Before
	public void setUp() throws Exception {
		mapping = new MyHandlerMethodMapping();
		handler = new MyHandler();
		method1 = handler.getClass().getMethod("handlerMethod1");
		method2 = handler.getClass().getMethod("handlerMethod2");
	}

	@Test(expected = IllegalStateException.class)
	public void registerDuplicates() {
		mapping.registerHandlerMethod(handler, method1, "foo");
		mapping.registerHandlerMethod(handler, method2, "foo");
	}

	@Test
	public void directMatch() throws Exception {
		String key = "foo";
		mapping.registerHandlerMethod(handler, method1, key);

		HandlerMethod result = mapping.getHandlerInternal(new MockHttpServletRequest("GET", key));
		assertEquals(method1, result.getMethod());
	}

	@Test
	public void patternMatch() throws Exception {
		mapping.registerHandlerMethod(handler, method1, "/fo*");
		mapping.registerHandlerMethod(handler, method1, "/f*");

		HandlerMethod result = mapping.getHandlerInternal(new MockHttpServletRequest("GET", "/foo"));
		assertEquals(method1, result.getMethod());
	}

	@Test(expected = IllegalStateException.class)
	public void ambiguousMatch() throws Exception {
		mapping.registerHandlerMethod(handler, method1, "/f?o");
		mapping.registerHandlerMethod(handler, method2, "/fo?");

		mapping.getHandlerInternal(new MockHttpServletRequest("GET", "/foo"));
	}

	@Test
	public void testDetectHandlerMethodsInAncestorContexts() {
		StaticApplicationContext cxt = new StaticApplicationContext();
		cxt.registerSingleton("myHandler", MyHandler.class);

		AbstractHandlerMethodMapping<String> mapping1 = new MyHandlerMethodMapping();
		mapping1.setApplicationContext(new StaticApplicationContext(cxt));
		mapping1.afterPropertiesSet();

		assertEquals(0, mapping1.getHandlerMethods().size());

		AbstractHandlerMethodMapping<String> mapping2 = new MyHandlerMethodMapping();
		mapping2.setDetectHandlerMethodsInAncestorContexts(true);
		mapping2.setApplicationContext(new StaticApplicationContext(cxt));
		mapping2.afterPropertiesSet();

		assertEquals(2, mapping2.getHandlerMethods().size());
	}


	private static class MyHandlerMethodMapping extends AbstractHandlerMethodMapping<String> {

		private UrlPathHelper pathHelper = new UrlPathHelper();

		private PathMatcher pathMatcher = new AntPathMatcher();

		@Override
		protected String getMatchingMapping(String pattern, HttpServletRequest request) {
			String lookupPath = pathHelper.getLookupPathForRequest(request);
			return pathMatcher.match(pattern, lookupPath) ? pattern : null;
		}

		@Override
		protected String getMappingForMethod(Method method, Class<?> handlerType) {
			String methodName = method.getName();
			return methodName.startsWith("handler") ? methodName : null;
		}

		@Override
		protected Comparator<String> getMappingComparator(HttpServletRequest request) {
			String lookupPath = pathHelper.getLookupPathForRequest(request);
			return pathMatcher.getPatternComparator(lookupPath);
		}

		@Override
		protected boolean isHandler(Class<?> beanType) {
			return true;
		}

		@Override
		protected Set<String> getMappingPathPatterns(String key) {
			return new HashSet<String>();
		}
	}

	@Controller
	static class MyHandler {

		@RequestMapping
		public void handlerMethod1() {
		}

		@RequestMapping
		public void handlerMethod2() {
		}
	}
}