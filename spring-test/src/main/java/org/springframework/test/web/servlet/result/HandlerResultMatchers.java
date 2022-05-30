/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.MethodInvocationInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * Factory for assertions on the selected handler or handler method.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#handler}.
 *
 * <p><strong>Note:</strong> Expectations that assert the controller method
 * used to process the request work only for requests processed with
 * {@link RequestMappingHandlerMapping} and {@link RequestMappingHandlerAdapter}
 * which is used by default with the Spring MVC Java config and XML namespace.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
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
	public ResultMatcher handlerType(Class<?> type) {
		return result -> {
			Object handler = result.getHandler();
			assertNotNull("No handler", handler);
			Class<?> actual = handler.getClass();
			if (handler instanceof HandlerMethod) {
				actual = ((HandlerMethod) handler).getBeanType();
			}
			assertEquals("Handler type", type, ClassUtils.getUserClass(actual));
		};
	}

	/**
	 * Assert the controller method used to process the request.
	 * <p>The expected method is specified through a "mock" controller method
	 * invocation similar to {@link MvcUriComponentsBuilder#fromMethodCall(Object)}.
	 * <p>For example, given this controller:
	 * <pre class="code">
	 * &#064;RestController
	 * public class SimpleController {
	 *
	 *     &#064;RequestMapping("/")
	 *     public ResponseEntity&lt;Void&gt; handle() {
	 *         return ResponseEntity.ok().build();
	 *     }
	 * }
	 * </pre>
	 * <p>A test that has statically imported {@link MvcUriComponentsBuilder#on}
	 * can be performed as follows:
	 * <pre class="code">
	 * mockMvc.perform(get("/"))
	 *     .andExpect(handler().methodCall(on(SimpleController.class).handle()));
	 * </pre>
	 * @param obj either the value returned from a "mock" controller invocation
	 * or the "mock" controller itself after an invocation
	 */
	public ResultMatcher methodCall(Object obj) {
		return result -> {
			if (!(obj instanceof MethodInvocationInfo)) {
				fail(String.format("The supplied object [%s] is not an instance of %s. " +
						"Ensure that you invoke the handler method via MvcUriComponentsBuilder.on().",
						obj, MethodInvocationInfo.class.getName()));
			}
			MethodInvocationInfo invocationInfo = (MethodInvocationInfo) obj;
			Method expected = invocationInfo.getControllerMethod();
			Method actual = getHandlerMethod(result).getMethod();
			assertEquals("Handler method", expected, actual);
		};
	}

	/**
	 * Assert the name of the controller method used to process the request
	 * using the given Hamcrest {@link Matcher}.
	 */
	public ResultMatcher methodName(Matcher<? super String> matcher) {
		return result -> {
			HandlerMethod handlerMethod = getHandlerMethod(result);
			assertThat("Handler method", handlerMethod.getMethod().getName(), matcher);
		};
	}

	/**
	 * Assert the name of the controller method used to process the request.
	 */
	public ResultMatcher methodName(String name) {
		return result -> {
			HandlerMethod handlerMethod = getHandlerMethod(result);
			assertEquals("Handler method", name, handlerMethod.getMethod().getName());
		};
	}

	/**
	 * Assert the controller method used to process the request.
	 */
	public ResultMatcher method(Method method) {
		return result -> {
			HandlerMethod handlerMethod = getHandlerMethod(result);
			assertEquals("Handler method", method, handlerMethod.getMethod());
		};
	}


	private static HandlerMethod getHandlerMethod(MvcResult result) {
		Object handler = result.getHandler();
		assertTrue("Not a HandlerMethod: " + handler, handler instanceof HandlerMethod);
		return (HandlerMethod) handler;
	}

}
