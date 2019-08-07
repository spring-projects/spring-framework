/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.method.support;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Test fixture for {@link InvocableHandlerMethod} unit tests.
 *
 * @author Rossen Stoyanchev
 */
public class InvocableHandlerMethodTests {

	private InvocableHandlerMethod handlerMethod;

	private NativeWebRequest webRequest;


	@Before
	public void setUp() throws Exception {
		Method method = Handler.class.getDeclaredMethod("handle", Integer.class, String.class);
		this.handlerMethod = new InvocableHandlerMethod(new Handler(), method);
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
	}


	@Test
	public void resolveArg() throws Exception {
		StubArgumentResolver intResolver = new StubArgumentResolver(Integer.class, 99);
		StubArgumentResolver stringResolver = new StubArgumentResolver(String.class, "value");

		HandlerMethodArgumentResolverComposite composite = new HandlerMethodArgumentResolverComposite();
		composite.addResolver(intResolver);
		composite.addResolver(stringResolver);
		handlerMethod.setHandlerMethodArgumentResolvers(composite);

		Object returnValue = handlerMethod.invokeForRequest(webRequest, null);
		assertEquals(1, intResolver.getResolvedParameters().size());
		assertEquals(1, stringResolver.getResolvedParameters().size());
		assertEquals("99-value", returnValue);
		assertEquals("intArg", intResolver.getResolvedParameters().get(0).getParameterName());
		assertEquals("stringArg", stringResolver.getResolvedParameters().get(0).getParameterName());
	}

	@Test
	public void resolveNullArg() throws Exception {
		StubArgumentResolver intResolver = new StubArgumentResolver(Integer.class, null);
		StubArgumentResolver stringResolver = new StubArgumentResolver(String.class, null);

		HandlerMethodArgumentResolverComposite composite = new HandlerMethodArgumentResolverComposite();
		composite.addResolver(intResolver);
		composite.addResolver(stringResolver);
		handlerMethod.setHandlerMethodArgumentResolvers(composite);

		Object returnValue = handlerMethod.invokeForRequest(webRequest, null);
		assertEquals(1, intResolver.getResolvedParameters().size());
		assertEquals(1, stringResolver.getResolvedParameters().size());
		assertEquals("null-null", returnValue);
	}

	@Test
	public void cannotResolveArg() throws Exception {
		try {
			handlerMethod.invokeForRequest(webRequest, null);
			fail("Expected exception");
		}
		catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("No suitable resolver for argument 0 of type 'java.lang.Integer'"));
		}
	}

	@Test
	public void resolveProvidedArg() throws Exception {
		Object returnValue = handlerMethod.invokeForRequest(webRequest, null, 99, "value");

		assertEquals(String.class, returnValue.getClass());
		assertEquals("99-value", returnValue);
	}

	@Test
	public void resolveProvidedArgFirst() throws Exception {
		StubArgumentResolver intResolver = new StubArgumentResolver(Integer.class, 1);
		StubArgumentResolver stringResolver = new StubArgumentResolver(String.class, "value1");

		HandlerMethodArgumentResolverComposite composite = new HandlerMethodArgumentResolverComposite();
		composite.addResolver(intResolver);
		composite.addResolver(stringResolver);
		handlerMethod.setHandlerMethodArgumentResolvers(composite);

		Object returnValue = handlerMethod.invokeForRequest(webRequest, null, 2, "value2");
		assertEquals("2-value2", returnValue);
	}

	@Test
	public void exceptionInResolvingArg() throws Exception {
		HandlerMethodArgumentResolverComposite composite = new HandlerMethodArgumentResolverComposite();
		composite.addResolver(new ExceptionRaisingArgumentResolver());
		handlerMethod.setHandlerMethodArgumentResolvers(composite);

		try {
			handlerMethod.invokeForRequest(webRequest, null);
			fail("Expected exception");
		}
		catch (HttpMessageNotReadableException ex) {
			// expected -  allow HandlerMethodArgumentResolver exceptions to propagate
		}
	}

	@Test
	public void illegalArgumentException() throws Exception {
		StubArgumentResolver intResolver = new StubArgumentResolver(Integer.class, "__invalid__");
		StubArgumentResolver stringResolver = new StubArgumentResolver(String.class, "value");

		HandlerMethodArgumentResolverComposite composite = new HandlerMethodArgumentResolverComposite();
		composite.addResolver(intResolver);
		composite.addResolver(stringResolver);
		handlerMethod.setHandlerMethodArgumentResolvers(composite);

		try {
			handlerMethod.invokeForRequest(webRequest, null);
			fail("Expected exception");
		}
		catch (IllegalStateException ex) {
			assertNotNull("Exception not wrapped", ex.getCause());
			assertTrue(ex.getCause() instanceof IllegalArgumentException);
			assertTrue(ex.getMessage().contains("Controller ["));
			assertTrue(ex.getMessage().contains("Method ["));
			assertTrue(ex.getMessage().contains("Resolved arguments: "));
			assertTrue(ex.getMessage().contains("[0] [type=java.lang.String] [value=__invalid__]"));
			assertTrue(ex.getMessage().contains("[1] [type=java.lang.String] [value=value"));
		}
	}

	@Test
	public void invocationTargetException() throws Exception {
		Throwable expected = new RuntimeException("error");
		try {
			invokeExceptionRaisingHandler(expected);
		}
		catch (RuntimeException actual) {
			assertSame(expected, actual);
		}

		expected = new Error("error");
		try {
			invokeExceptionRaisingHandler(expected);
		}
		catch (Error actual) {
			assertSame(expected, actual);
		}

		expected = new Exception("error");
		try {
			invokeExceptionRaisingHandler(expected);
		}
		catch (Exception actual) {
			assertSame(expected, actual);
		}

		expected = new Throwable("error");
		try {
			invokeExceptionRaisingHandler(expected);
		}
		catch (IllegalStateException actual) {
			assertNotNull(actual.getCause());
			assertSame(expected, actual.getCause());
			assertTrue(actual.getMessage().contains("Failed to invoke handler method"));
		}
	}

	@Test  // SPR-13917
	public void invocationErrorMessage() throws Exception {
		HandlerMethodArgumentResolverComposite composite = new HandlerMethodArgumentResolverComposite();
		composite.addResolver(new StubArgumentResolver(double.class, null));

		Method method = Handler.class.getDeclaredMethod("handle", double.class);
		Object handler = new Handler();
		InvocableHandlerMethod hm = new InvocableHandlerMethod(handler, method);
		hm.setHandlerMethodArgumentResolvers(composite);

		try {
			hm.invokeForRequest(this.webRequest, new ModelAndViewContainer());
			fail();
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(), containsString("Illegal argument"));
		}
	}


	private void invokeExceptionRaisingHandler(Throwable expected) throws Exception {
		Method method = ExceptionRaisingHandler.class.getDeclaredMethod("raiseException");
		Object handler = new ExceptionRaisingHandler(expected);
		new InvocableHandlerMethod(handler, method).invokeForRequest(webRequest, null);
		fail("Expected exception");
	}


	@SuppressWarnings("unused")
	private static class Handler {

		public String handle(Integer intArg, String stringArg) {
			return intArg + "-" + stringArg;
		}

		public void handle(double amount) {
		}
	}


	@SuppressWarnings("unused")
	private static class ExceptionRaisingHandler {

		private final Throwable t;

		public ExceptionRaisingHandler(Throwable t) {
			this.t = t;
		}

		public void raiseException() throws Throwable {
			throw t;
		}
	}


	private static class ExceptionRaisingArgumentResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return true;
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
			throw new HttpMessageNotReadableException("oops, can't read");
		}
	}

}
