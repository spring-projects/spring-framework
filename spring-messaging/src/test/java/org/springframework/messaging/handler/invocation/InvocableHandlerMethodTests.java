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

package org.springframework.messaging.handler.invocation;

import java.lang.reflect.Method;

import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.util.ClassUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InvocableHandlerMethod}.
 *
 * @author Rossen Stoyanchev
 */
public class InvocableHandlerMethodTests {

	private final Message<?> message = mock(Message.class);

	private final HandlerMethodArgumentResolverComposite composite = new HandlerMethodArgumentResolverComposite();


	@Test
	public void resolveArg() throws Exception {
		this.composite.addResolver(new StubArgumentResolver(99));
		this.composite.addResolver(new StubArgumentResolver("value"));

		Object value = getInvocable("handle", Integer.class, String.class).invoke(this.message);

		assertEquals(1, getStubResolver(0).getResolvedParameters().size());
		assertEquals(1, getStubResolver(1).getResolvedParameters().size());
		assertEquals("99-value", value);
		assertEquals("intArg", getStubResolver(0).getResolvedParameters().get(0).getParameterName());
		assertEquals("stringArg", getStubResolver(1).getResolvedParameters().get(0).getParameterName());
	}

	@Test
	public void resolveNoArgValue() throws Exception {
		this.composite.addResolver(new StubArgumentResolver(Integer.class));
		this.composite.addResolver(new StubArgumentResolver(String.class));

		Object returnValue = getInvocable("handle", Integer.class, String.class).invoke(this.message);

		assertEquals(1, getStubResolver(0).getResolvedParameters().size());
		assertEquals(1, getStubResolver(1).getResolvedParameters().size());
		assertEquals("null-null", returnValue);
	}

	@Test
	public void cannotResolveArg() throws Exception {
		try {
			getInvocable("handle", Integer.class, String.class).invoke(this.message);
			fail("Expected exception");
		}
		catch (MethodArgumentResolutionException ex) {
			assertNotNull(ex.getMessage());
			assertTrue(ex.getMessage().contains("Could not resolve parameter [0]"));
		}
	}

	@Test
	public void resolveProvidedArg() throws Exception {
		Object value = getInvocable("handle", Integer.class, String.class).invoke(this.message, 99, "value");

		assertNotNull(value);
		assertEquals(String.class, value.getClass());
		assertEquals("99-value", value);
	}

	@Test
	public void resolveProvidedArgFirst() throws Exception {
		this.composite.addResolver(new StubArgumentResolver(1));
		this.composite.addResolver(new StubArgumentResolver("value1"));
		Object value = getInvocable("handle", Integer.class, String.class).invoke(this.message, 2, "value2");

		assertEquals("2-value2", value);
	}

	@Test
	public void exceptionInResolvingArg() throws Exception {
		this.composite.addResolver(new ExceptionRaisingArgumentResolver());
		try {
			getInvocable("handle", Integer.class, String.class).invoke(this.message);
			fail("Expected exception");
		}
		catch (IllegalArgumentException ex) {
			// expected -  allow HandlerMethodArgumentResolver exceptions to propagate
		}
	}

	@Test
	public void illegalArgumentException() throws Exception {
		this.composite.addResolver(new StubArgumentResolver(Integer.class, "__not_an_int__"));
		this.composite.addResolver(new StubArgumentResolver("value"));
		try {
			getInvocable("handle", Integer.class, String.class).invoke(this.message);
			fail("Expected exception");
		}
		catch (IllegalStateException ex) {
			assertNotNull("Exception not wrapped", ex.getCause());
			assertTrue(ex.getCause() instanceof IllegalArgumentException);
			assertTrue(ex.getMessage().contains("Endpoint ["));
			assertTrue(ex.getMessage().contains("Method ["));
			assertTrue(ex.getMessage().contains("with argument values:"));
			assertTrue(ex.getMessage().contains("[0] [type=java.lang.String] [value=__not_an_int__]"));
			assertTrue(ex.getMessage().contains("[1] [type=java.lang.String] [value=value"));
		}
	}

	@Test
	public void invocationTargetException() throws Exception {
		Throwable expected = null;
		try {
			expected = new RuntimeException("error");
			getInvocable("handleWithException", Throwable.class).invoke(this.message, expected);
			fail("Expected exception");
		}
		catch (RuntimeException actual) {
			assertSame(expected, actual);
		}
		try {
			expected = new Error("error");
			getInvocable("handleWithException", Throwable.class).invoke(this.message, expected);
			fail("Expected exception");
		}
		catch (Error actual) {
			assertSame(expected, actual);
		}
		try {
			expected = new Exception("error");
			getInvocable("handleWithException", Throwable.class).invoke(this.message, expected);
			fail("Expected exception");
		}
		catch (Exception actual) {
			assertSame(expected, actual);
		}
		try {
			expected = new Throwable("error");
			getInvocable("handleWithException", Throwable.class).invoke(this.message, expected);
			fail("Expected exception");
		}
		catch (IllegalStateException actual) {
			assertNotNull(actual.getCause());
			assertSame(expected, actual.getCause());
			assertTrue(actual.getMessage().contains("Invocation failure"));
		}
	}

	@Test  // Based on SPR-13917 (spring-web)
	public void invocationErrorMessage() throws Exception {
		this.composite.addResolver(new StubArgumentResolver(double.class));
		try {
			getInvocable("handle", double.class).invoke(this.message);
			fail();
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(), containsString("Illegal argument"));
		}
	}

	public InvocableHandlerMethod getInvocable(String methodName, Class<?>... argTypes) {
		Method method = ClassUtils.getMethod(Handler.class, methodName, argTypes);
		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(new Handler(), method);
		handlerMethod.setMessageMethodArgumentResolvers(this.composite);
		return handlerMethod;
	}

	private StubArgumentResolver getStubResolver(int index) {
		return (StubArgumentResolver) this.composite.getResolvers().get(index);
	}



	@SuppressWarnings("unused")
	private static class Handler {

		public String handle(Integer intArg, String stringArg) {
			return intArg + "-" + stringArg;
		}

		public void handle(double amount) {
		}

		public void handleWithException(Throwable ex) throws Throwable {
			throw ex;
		}
	}


	private static class ExceptionRaisingArgumentResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return true;
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, Message<?> message) {
			throw new IllegalArgumentException("oops, can't read");
		}
	}

}
