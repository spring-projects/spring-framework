/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.web.ServletTestExecutionListener.POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE;
import static org.springframework.test.context.web.ServletTestExecutionListener.RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE;

/**
 * Tests for {@link ServletTestExecutionListener}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.2.6
 */
class ServletTestExecutionListenerTests {

	private static final String SET_UP_OUTSIDE_OF_STEL = "setUpOutsideOfStel";

	private final WebApplicationContext wac = mock();
	private final MockServletContext mockServletContext = new MockServletContext();
	private final TestContext testContext = mock();
	private final ServletTestExecutionListener listener = new ServletTestExecutionListener();


	@BeforeEach
	void setUp() {
		given(wac.getServletContext()).willReturn(mockServletContext);
		given(testContext.getApplicationContext()).willReturn(wac);

		MockHttpServletRequest request = new MockHttpServletRequest(mockServletContext);
		MockHttpServletResponse response = new MockHttpServletResponse();
		ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);

		request.setAttribute(SET_UP_OUTSIDE_OF_STEL, "true");

		RequestContextHolder.setRequestAttributes(servletWebRequest);
		assertSetUpOutsideOfStelAttributeExists();
	}

	@Test
	void standardApplicationContext() throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(getClass());
		given(testContext.getApplicationContext()).willReturn(mock());

		listener.beforeTestClass(testContext);
		assertSetUpOutsideOfStelAttributeExists();

		listener.prepareTestInstance(testContext);
		assertSetUpOutsideOfStelAttributeExists();

		listener.beforeTestMethod(testContext);
		assertSetUpOutsideOfStelAttributeExists();

		listener.afterTestMethod(testContext);
		assertSetUpOutsideOfStelAttributeExists();
	}

	@Test
	void legacyWebTestCaseWithoutExistingRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(LegacyWebTestCase.class);

		RequestContextHolder.resetRequestAttributes();
		assertRequestAttributesDoNotExist();

		listener.beforeTestClass(testContext);

		listener.prepareTestInstance(testContext);
		assertRequestAttributesDoNotExist();
		verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		given(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(null);

		listener.beforeTestMethod(testContext);
		assertRequestAttributesDoNotExist();
		verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

		listener.afterTestMethod(testContext);
		verify(testContext, times(1)).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		assertRequestAttributesDoNotExist();
	}

	@Test
	void legacyWebTestCaseWithPresetRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(LegacyWebTestCase.class);

		listener.beforeTestClass(testContext);
		assertSetUpOutsideOfStelAttributeExists();

		listener.prepareTestInstance(testContext);
		assertSetUpOutsideOfStelAttributeExists();
		verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		given(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(null);

		listener.beforeTestMethod(testContext);
		assertSetUpOutsideOfStelAttributeExists();
		verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		given(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(null);

		listener.afterTestMethod(testContext);
		verify(testContext, times(1)).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		assertSetUpOutsideOfStelAttributeExists();
	}

	@Test
	void atWebAppConfigTestCaseWithoutExistingRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(AtWebAppConfigWebTestCase.class);

		RequestContextHolder.resetRequestAttributes();
		listener.beforeTestClass(testContext);
		assertRequestAttributesDoNotExist();

		assertWebAppConfigTestCase();
	}

	@Test
	void atWebAppConfigTestCaseWithPresetRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(AtWebAppConfigWebTestCase.class);

		listener.beforeTestClass(testContext);
		assertRequestAttributesExist();

		assertWebAppConfigTestCase();
	}

	/**
	 * @since 4.3
	 */
	@Test
	void activateListenerWithoutExistingRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(NoAtWebAppConfigWebTestCase.class);
		given(testContext.getAttribute(ServletTestExecutionListener.ACTIVATE_LISTENER)).willReturn(true);

		RequestContextHolder.resetRequestAttributes();
		listener.beforeTestClass(testContext);
		assertRequestAttributesDoNotExist();

		assertWebAppConfigTestCase();
	}


	private RequestAttributes assertRequestAttributesExist() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		assertThat(requestAttributes).as("request attributes should exist").isNotNull();
		return requestAttributes;
	}

	private void assertRequestAttributesDoNotExist() {
		assertThat(RequestContextHolder.getRequestAttributes()).as("request attributes should not exist").isNull();
	}

	private void assertSetUpOutsideOfStelAttributeExists() {
		RequestAttributes requestAttributes = assertRequestAttributesExist();
		Object setUpOutsideOfStel = requestAttributes.getAttribute(SET_UP_OUTSIDE_OF_STEL,
			RequestAttributes.SCOPE_REQUEST);
		assertThat(setUpOutsideOfStel).as(SET_UP_OUTSIDE_OF_STEL + " should exist as a request attribute").isNotNull();
	}

	private void assertSetUpOutsideOfStelAttributeDoesNotExist() {
		RequestAttributes requestAttributes = assertRequestAttributesExist();
		Object setUpOutsideOfStel = requestAttributes.getAttribute(SET_UP_OUTSIDE_OF_STEL,
			RequestAttributes.SCOPE_REQUEST);
		assertThat(setUpOutsideOfStel).as(SET_UP_OUTSIDE_OF_STEL + " should NOT exist as a request attribute").isNull();
	}

	private void assertWebAppConfigTestCase() throws Exception {
		listener.prepareTestInstance(testContext);
		assertRequestAttributesExist();
		assertSetUpOutsideOfStelAttributeDoesNotExist();
		verify(testContext, times(1)).setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		verify(testContext, times(1)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		given(testContext.getAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(Boolean.TRUE);
		given(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(Boolean.TRUE);

		listener.beforeTestMethod(testContext);
		assertRequestAttributesExist();
		assertSetUpOutsideOfStelAttributeDoesNotExist();
		verify(testContext, times(1)).setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		verify(testContext, times(1)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

		listener.afterTestMethod(testContext);
		verify(testContext).removeAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		verify(testContext).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		assertRequestAttributesDoNotExist();
	}


	static class LegacyWebTestCase {
	}

	@WebAppConfiguration
	static class AtWebAppConfigWebTestCase {
	}

	static class NoAtWebAppConfigWebTestCase {
	}

}
