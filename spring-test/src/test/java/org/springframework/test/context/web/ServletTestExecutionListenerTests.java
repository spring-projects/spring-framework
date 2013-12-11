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

package org.springframework.test.context.web;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.context.web.ServletTestExecutionListener.*;

/**
 * Unit tests for {@link ServletTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 3.2.6
 */
public class ServletTestExecutionListenerTests {

	private static final String SET_UP_OUTSIDE_OF_STEL = "SET_UP_OUTSIDE_OF_STEL";

	private final WebApplicationContext wac = mock(WebApplicationContext.class);
	private final MockServletContext mockServletContext = new MockServletContext();
	private final TestContext testContext = mock(TestContext.class);
	private final ServletTestExecutionListener listener = new ServletTestExecutionListener();


	private void assertAttributesAvailable() {
		assertNotNull("request attributes should be available", RequestContextHolder.getRequestAttributes());
	}

	private void assertAttributesNotAvailable() {
		assertNull("request attributes should not be available", RequestContextHolder.getRequestAttributes());
	}

	private void assertAttributeExists() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		assertNotNull("request attributes should exist", requestAttributes);
		Object setUpOutsideOfStel = requestAttributes.getAttribute(SET_UP_OUTSIDE_OF_STEL,
			RequestAttributes.SCOPE_REQUEST);
		assertNotNull(SET_UP_OUTSIDE_OF_STEL + " should exist as a request attribute", setUpOutsideOfStel);
	}

	private void assertAttributeDoesNotExist() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		assertNotNull("request attributes should exist", requestAttributes);
		Object setUpOutsideOfStel = requestAttributes.getAttribute(SET_UP_OUTSIDE_OF_STEL,
			RequestAttributes.SCOPE_REQUEST);
		assertNull(SET_UP_OUTSIDE_OF_STEL + " should NOT exist as a request attribute", setUpOutsideOfStel);
	}

	@Before
	public void setUp() {
		when(wac.getServletContext()).thenReturn(mockServletContext);
		when(testContext.getApplicationContext()).thenReturn(wac);

		MockHttpServletRequest request = new MockHttpServletRequest(mockServletContext);
		MockHttpServletResponse response = new MockHttpServletResponse();
		ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);

		request.setAttribute(SET_UP_OUTSIDE_OF_STEL, "true");

		RequestContextHolder.setRequestAttributes(servletWebRequest);
		assertAttributeExists();
	}

	@Test
	public void standardApplicationContext() throws Exception {
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(getClass());
		when(testContext.getApplicationContext()).thenReturn(mock(ApplicationContext.class));

		listener.beforeTestClass(testContext);
		assertAttributeExists();

		listener.prepareTestInstance(testContext);
		assertAttributeExists();

		listener.beforeTestMethod(testContext);
		assertAttributeExists();

		listener.afterTestMethod(testContext);
		assertAttributeExists();
	}

	@Test
	public void legacyWebTestCaseWithoutExistingRequestAttributes() throws Exception {
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(LegacyWebTestCase.class);

		RequestContextHolder.resetRequestAttributes();
		assertAttributesNotAvailable();

		listener.beforeTestClass(testContext);

		listener.prepareTestInstance(testContext);
		assertAttributesNotAvailable();
		verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		when(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).thenReturn(null);

		listener.beforeTestMethod(testContext);
		assertAttributesNotAvailable();
		verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

		listener.afterTestMethod(testContext);
		verify(testContext, times(1)).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		assertAttributesNotAvailable();
	}

	@Test
	public void legacyWebTestCaseWithPresetRequestAttributes() throws Exception {
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(LegacyWebTestCase.class);

		listener.beforeTestClass(testContext);
		assertAttributeExists();

		listener.prepareTestInstance(testContext);
		assertAttributeExists();
		verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		when(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).thenReturn(null);

		listener.beforeTestMethod(testContext);
		assertAttributeExists();
		verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		when(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).thenReturn(null);

		listener.afterTestMethod(testContext);
		verify(testContext, times(1)).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		assertAttributeExists();
	}

	@Test
	public void atWebAppConfigTestCaseWithoutExistingRequestAttributes() throws Exception {
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(AtWebAppConfigWebTestCase.class);

		RequestContextHolder.resetRequestAttributes();
		listener.beforeTestClass(testContext);
		assertAttributesNotAvailable();

		assertWebAppConfigTestCase();
	}

	@Test
	public void atWebAppConfigTestCaseWithPresetRequestAttributes() throws Exception {
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(AtWebAppConfigWebTestCase.class);

		listener.beforeTestClass(testContext);
		assertAttributesAvailable();

		assertWebAppConfigTestCase();
	}

	private void assertWebAppConfigTestCase() throws Exception {
		listener.prepareTestInstance(testContext);
		assertAttributeDoesNotExist();
		verify(testContext, times(1)).setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		verify(testContext, times(1)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		when(testContext.getAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).thenReturn(Boolean.TRUE);
		when(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).thenReturn(Boolean.TRUE);

		listener.beforeTestMethod(testContext);
		assertAttributeDoesNotExist();
		verify(testContext, times(1)).setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		verify(testContext, times(1)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

		listener.afterTestMethod(testContext);
		verify(testContext).removeAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		verify(testContext).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		assertAttributesNotAvailable();
	}


	static class LegacyWebTestCase {
	}

	@WebAppConfiguration
	static class AtWebAppConfigWebTestCase {
	}

}
