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

package org.springframework.web.method.annotation;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

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
		adaptee = createMock(WebArgumentResolver.class);
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
		expect(adaptee.resolveArgument(parameter, webRequest)).andReturn(42);
		replay(adaptee);

		assertTrue("Parameter not supported", adapter.supportsParameter(parameter));

		verify(adaptee);
	}

	@Test
	public void supportsParameterUnresolved() throws Exception {
		expect(adaptee.resolveArgument(parameter, webRequest)).andReturn(WebArgumentResolver.UNRESOLVED);
		replay(adaptee);

		assertFalse("Parameter supported", adapter.supportsParameter(parameter));

		verify(adaptee);
	}

	@Test
	public void supportsParameterWrongType() throws Exception {
		expect(adaptee.resolveArgument(parameter, webRequest)).andReturn("Foo");
		replay(adaptee);

		assertFalse("Parameter supported", adapter.supportsParameter(parameter));

		verify(adaptee);
	}

	@Test
	public void supportsParameterThrowsException() throws Exception {
		expect(adaptee.resolveArgument(parameter, webRequest)).andThrow(new Exception());
		replay(adaptee);

		assertFalse("Parameter supported", adapter.supportsParameter(parameter));

		verify(adaptee);
	}

	@Test
	public void resolveArgument() throws Exception {
		int expected = 42;
		expect(adaptee.resolveArgument(parameter, webRequest)).andReturn(expected);
		replay(adaptee);

		Object result = adapter.resolveArgument(parameter, null, webRequest, null);
		assertEquals("Invalid result", expected, result);

		verify(adaptee);

	}

	@Test(expected = IllegalStateException.class)
	public void resolveArgumentUnresolved() throws Exception {
		expect(adaptee.resolveArgument(parameter, webRequest)).andReturn(WebArgumentResolver.UNRESOLVED);
		replay(adaptee);

		adapter.resolveArgument(parameter, null, webRequest, null);

		verify(adaptee);
	}

	@Test(expected = IllegalStateException.class)
	public void resolveArgumentWrongType() throws Exception {
		expect(adaptee.resolveArgument(parameter, webRequest)).andReturn("Foo");
		replay(adaptee);

		adapter.resolveArgument(parameter, null, webRequest, null);

		verify(adaptee);
	}

	@Test(expected = Exception.class)
	public void resolveArgumentThrowsException() throws Exception {
		expect(adaptee.resolveArgument(parameter, webRequest)).andThrow(new Exception());
		replay(adaptee);

		adapter.resolveArgument(parameter, null, webRequest, null);

		verify(adaptee);
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
