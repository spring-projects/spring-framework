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

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Conventions;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * {@code TestExecutionListener} which provides mock Servlet API support to
 * {@link WebApplicationContext WebApplicationContexts} loaded by the <em>Spring
 * TestContext Framework</em>.
 *
 * <p>Specifically, {@code ServletTestExecutionListener} sets up thread-local
 * state via Spring Web's {@link RequestContextHolder} during {@linkplain
 * #prepareTestInstance(TestContext) test instance preparation} and {@linkplain
 * #beforeTestMethod(TestContext) before each test method} and creates a {@link
 * MockHttpServletRequest}, {@link MockHttpServletResponse}, and
 * {@link ServletWebRequest} based on the {@link MockServletContext} present in
 * the {@code WebApplicationContext}. This listener also ensures that the
 * {@code MockHttpServletResponse} and {@code ServletWebRequest} can be injected
 * into the test instance, and once the test is complete this listener {@linkplain
 * #afterTestMethod(TestContext) cleans up} thread-local state.
 *
 * <p>Note that {@code ServletTestExecutionListener} is enabled by default but
 * generally takes no action if the {@linkplain TestContext#getTestClass() test
 * class} is not annotated with {@link WebAppConfiguration @WebAppConfiguration}.
 * See the Javadoc for individual methods in this class for details.
 *
 * @author Sam Brannen
 * @since 3.2
 */
public class ServletTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Attribute name for a {@link TestContext} attribute which indicates
	 * whether or not the {@code ServletTestExecutionListener} should {@linkplain
	 * RequestContextHolder#resetRequestAttributes() reset} Spring Web's
	 * {@code RequestContextHolder} in {@link #afterTestMethod(TestContext)}.
	 *
	 * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
	 */
	public static final String RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(
		ServletTestExecutionListener.class, "resetRequestContextHolder");

	/**
	 * Attribute name for a {@link TestContext} attribute which indicates that
	 * {@code ServletTestExecutionListener} has already populated Spring Web's
	 * {@code RequestContextHolder}.
	 *
	 * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
	 */
	public static final String POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(
		ServletTestExecutionListener.class, "populatedRequestContextHolder");

	private static final Log logger = LogFactory.getLog(ServletTestExecutionListener.class);


	/**
	 * Sets up thread-local state during the <em>test instance preparation</em>
	 * callback phase via Spring Web's {@link RequestContextHolder}, but only if
	 * the {@linkplain TestContext#getTestClass() test class} is annotated with
	 * {@link WebAppConfiguration @WebAppConfiguration}.
	 *
	 * @see TestExecutionListener#prepareTestInstance(TestContext)
	 * @see #setUpRequestContextIfNecessary(TestContext)
	 */
	@SuppressWarnings("javadoc")
	public void prepareTestInstance(TestContext testContext) throws Exception {
		setUpRequestContextIfNecessary(testContext);
	}

	/**
	 * Sets up thread-local state before each test method via Spring Web's
	 * {@link RequestContextHolder}, but only if the
	 * {@linkplain TestContext#getTestClass() test class} is annotated with
	 * {@link WebAppConfiguration @WebAppConfiguration}.
	 *
	 * @see TestExecutionListener#beforeTestMethod(TestContext)
	 * @see #setUpRequestContextIfNecessary(TestContext)
	 */
	@SuppressWarnings("javadoc")
	public void beforeTestMethod(TestContext testContext) throws Exception {
		setUpRequestContextIfNecessary(testContext);
	}

	/**
	 * Cleans up thread-local state after each test method by {@linkplain
	 * RequestContextHolder#resetRequestAttributes() resetting} Spring Web's
	 * {@code RequestContextHolder}, but only if the {@link
	 * #RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} in the supplied {@code TestContext}
	 * has a value of {@link Boolean#TRUE}.
	 *
	 * <p>The {@link #RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} and
	 * {@link #POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} will be subsequently
	 * removed from the test context, regardless of their values.
	 *
	 * @see TestExecutionListener#afterTestMethod(TestContext)
	 */
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE))) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Resetting RequestContextHolder for test context %s.", testContext));
			}
			RequestContextHolder.resetRequestAttributes();
		}
		testContext.removeAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		testContext.removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
	}

	private boolean notAnnotatedWithWebAppConfiguration(TestContext testContext) {
		return AnnotationUtils.findAnnotation(testContext.getTestClass(), WebAppConfiguration.class) == null;
	}

	private boolean alreadyPopulatedRequestContextHolder(TestContext testContext) {
		return Boolean.TRUE.equals(testContext.getAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE));
	}

	private void setUpRequestContextIfNecessary(TestContext testContext) {
		if (notAnnotatedWithWebAppConfiguration(testContext) || alreadyPopulatedRequestContextHolder(testContext)) {
			return;
		}

		ApplicationContext context = testContext.getApplicationContext();

		if (context instanceof WebApplicationContext) {
			WebApplicationContext wac = (WebApplicationContext) context;
			ServletContext servletContext = wac.getServletContext();
			Assert.state(servletContext instanceof MockServletContext, String.format(
				"The WebApplicationContext for test context %s must be configured with a MockServletContext.",
				testContext));

			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
					"Setting up MockHttpServletRequest, MockHttpServletResponse, ServletWebRequest, and RequestContextHolder for test context %s.",
					testContext));
			}

			MockServletContext mockServletContext = (MockServletContext) servletContext;
			MockHttpServletRequest request = new MockHttpServletRequest(mockServletContext);
			MockHttpServletResponse response = new MockHttpServletResponse();
			ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);

			RequestContextHolder.setRequestAttributes(servletWebRequest);
			testContext.setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
			testContext.setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

			if (wac instanceof ConfigurableApplicationContext) {
				@SuppressWarnings("resource")
				ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) wac;
				ConfigurableListableBeanFactory bf = configurableApplicationContext.getBeanFactory();
				bf.registerResolvableDependency(MockHttpServletResponse.class, response);
				bf.registerResolvableDependency(ServletWebRequest.class, servletWebRequest);
			}
		}
	}

}
