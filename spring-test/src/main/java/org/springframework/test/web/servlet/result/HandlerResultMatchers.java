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

package org.springframework.test.web.servlet.result;

import java.lang.reflect.Method;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Factory for assertions on the selected handler or handler method.
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#handler}.
 *
 * <p><strong>Note:</strong> Expectations that assert the controller method
 * used to process the request work only for requests processed with
 * {@link RequestMappingHandlerMapping} and {@link RequestMappingHandlerAdapter}
 * which is used by default with the Spring MVC Java config and XML namespace.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class HandlerResultMatchers {


	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#handler()}.
	 */
	protected HandlerResultMatchers() {
	}


	/**
	 * Assert the type of the handler that processed the request.
	 */
	public ResultMatcher handlerType(final Class<?> type) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				Object handler = result.getHandler();
				assertTrue("No handler: ", handler != null);
				Class<?> actual = handler.getClass();
				if (HandlerMethod.class.isInstance(handler)) {
					actual = ((HandlerMethod) handler).getBeanType();
				}
				assertEquals("Handler type", type, ClassUtils.getUserClass(actual));
			}
		};
	}

	/**
	 * Assert the name of the controller method used to process the request
	 * using the given Hamcrest {@link Matcher}.
	 */
	public ResultMatcher methodName(final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				HandlerMethod handlerMethod = getHandlerMethod(result);
				assertThat("HandlerMethod", handlerMethod.getMethod().getName(), matcher);
			}
		};
	}

	/**
	 * Assert the name of the controller method used to process the request.
	 */
	public ResultMatcher methodName(final String name) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				HandlerMethod handlerMethod = getHandlerMethod(result);
				assertEquals("HandlerMethod", name, handlerMethod.getMethod().getName());
			}
		};
	}

	/**
	 * Assert the controller method used to process the request.
	 */
	public ResultMatcher method(final Method method) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				HandlerMethod handlerMethod = getHandlerMethod(result);
				assertEquals("HandlerMethod", method, handlerMethod.getMethod());
			}
		};
	}

	private static HandlerMethod getHandlerMethod(MvcResult result) {
		Object handler = result.getHandler();
		assertTrue("No handler: ", handler != null);
		assertTrue("Not a HandlerMethod: " + handler, HandlerMethod.class.isInstance(handler));
		return (HandlerMethod) handler;
	}

}
