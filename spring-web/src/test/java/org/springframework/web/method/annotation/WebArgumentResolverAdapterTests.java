/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.method.annotation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Test fixture with {@link WebArgumentResolverAdapterTests}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class WebArgumentResolverAdapterTests {

	private TestWebArgumentResolverAdapter adapter;

	private WebArgumentResolver adaptee;

	private MethodParameter parameter;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		adaptee = mock(WebArgumentResolver.class);
		adapter = new TestWebArgumentResolverAdapter(adaptee);
		parameter = new MethodParameter(getClass().getMethod("handle", Integer.TYPE), 0);
		webRequest = new ServletWebRequest(new MockHttpServletRequest());

		// Expose request to the current thread (for SpEL expressions)
		RequestContextHolder.setRequestAttributes(webRequest);
	}

	@After
	public void resetRequestContextHolder() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	public void supportsParameter() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willReturn(42);

		assertTrue("Parameter not supported", adapter.supportsParameter(parameter));

		verify(adaptee).resolveArgument(parameter, webRequest);
	}

	@Test
	public void supportsParameterUnresolved() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willReturn(WebArgumentResolver.UNRESOLVED);

		assertFalse("Parameter supported", adapter.supportsParameter(parameter));

		verify(adaptee).resolveArgument(parameter, webRequest);
	}

	@Test
	public void supportsParameterWrongType() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willReturn("Foo");

		assertFalse("Parameter supported", adapter.supportsParameter(parameter));

		verify(adaptee).resolveArgument(parameter, webRequest);
	}

	@Test
	public void supportsParameterThrowsException() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willThrow(new Exception());

		assertFalse("Parameter supported", adapter.supportsParameter(parameter));

		verify(adaptee).resolveArgument(parameter, webRequest);
	}

	@Test
	public void resolveArgument() throws Exception {
		int expected = 42;
		given(adaptee.resolveArgument(parameter, webRequest)).willReturn(expected);

		Object result = adapter.resolveArgument(parameter, null, webRequest, null);
		assertEquals("Invalid result", expected, result);
	}

	@Test(expected = IllegalStateException.class)
	public void resolveArgumentUnresolved() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willReturn(WebArgumentResolver.UNRESOLVED);

		adapter.resolveArgument(parameter, null, webRequest, null);
	}

	@Test(expected = IllegalStateException.class)
	public void resolveArgumentWrongType() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willReturn("Foo");

		adapter.resolveArgument(parameter, null, webRequest, null);
	}

	@Test(expected = Exception.class)
	public void resolveArgumentThrowsException() throws Exception {
		given(adaptee.resolveArgument(parameter, webRequest)).willThrow(new Exception());

		adapter.resolveArgument(parameter, null, webRequest, null);
	}

	public void handle(int param) {
	}

	private class TestWebArgumentResolverAdapter extends AbstractWebArgumentResolverAdapter {

		public TestWebArgumentResolverAdapter(WebArgumentResolver adaptee) {
			super(adaptee);
		}

		@Override
		protected NativeWebRequest getWebRequest() {
			return WebArgumentResolverAdapterTests.this.webRequest;
		}
	}

}
