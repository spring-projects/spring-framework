/*
 * Copyright 2002-2011 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Comparator;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.UrlPathHelper;

import static org.junit.Assert.*;

/**
 * Test for {@link AbstractHandlerMethodMapping}.
 *
 * @author Arjen Poutsma
 */
public class HandlerMethodMappingTests {

	private AbstractHandlerMethodMapping<String> mapping;

	private HandlerMethod handlerMethod1;

	private HandlerMethod handlerMethod2;

	@Before
	public void setUp() throws Exception {
		mapping = new MyHandlerMethodMapping();
		MyHandler handler = new MyHandler();
		handlerMethod1 = new HandlerMethod(handler, "handlerMethod1");
		handlerMethod2 = new HandlerMethod(handler, "handlerMethod2");
	}

	@Test(expected = IllegalStateException.class)
	public void registerDuplicates() {
		mapping.registerHandlerMethod("foo", handlerMethod1);
		mapping.registerHandlerMethod("foo", handlerMethod2);
	}

	@Test
	public void directMatch() throws Exception {
		String key = "foo";
		mapping.registerHandlerMethod(key, handlerMethod1);

		HandlerMethod result = mapping.getHandlerInternal(new MockHttpServletRequest("GET", key));
		assertEquals(handlerMethod1, result);
	}

	@Test
	public void patternMatch() throws Exception {
		mapping.registerHandlerMethod("/fo*", handlerMethod1);
		mapping.registerHandlerMethod("/f*", handlerMethod1);

		HandlerMethod result = mapping.getHandlerInternal(new MockHttpServletRequest("GET", "/foo"));
		assertEquals(handlerMethod1, result);
	}
	
	@Test(expected = IllegalStateException.class)
	public void ambiguousMatch() throws Exception {
		mapping.registerHandlerMethod("/f?o", handlerMethod1);
		mapping.registerHandlerMethod("/fo?", handlerMethod2);

		mapping.getHandlerInternal(new MockHttpServletRequest("GET", "/foo"));
	}

	private static class MyHandlerMethodMapping extends AbstractHandlerMethodMapping<String> {

		private UrlPathHelper urlPathHelper = new UrlPathHelper();

		private PathMatcher pathMatcher = new AntPathMatcher();

		@Override
		protected String getKeyForRequest(HttpServletRequest request) throws Exception {
			return urlPathHelper.getLookupPathForRequest(request);
		}

		@Override
		protected String getMatchingKey(String pattern, HttpServletRequest request) {
			String lookupPath = urlPathHelper.getLookupPathForRequest(request);

			return pathMatcher.match(pattern, lookupPath) ? pattern : null;
		}

		@Override
		protected String getKeyForMethod(String beanName, Method method) {
			String methodName = method.getName();
			return methodName.startsWith("handler") ? methodName : null;
		}

		@Override
		protected Comparator<String> getKeyComparator(HttpServletRequest request) {
			String lookupPath = urlPathHelper.getLookupPathForRequest(request);

			return pathMatcher.getPatternComparator(lookupPath);
		}

		@Override
		protected boolean isHandler(String beanName) {
			return true;
		}
	}

	private static class MyHandler {

		@SuppressWarnings("unused")
		public void handlerMethod1() {
		}

		@SuppressWarnings("unused")
		public void handlerMethod2() {
		}
	}
}