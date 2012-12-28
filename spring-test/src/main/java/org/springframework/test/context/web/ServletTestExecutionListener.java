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

package org.springframework.test.context.web;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
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
 * takes no action if the {@link ApplicationContext} loaded for the current test
 * is not a {@link WebApplicationContext}.
 *
 * @author Sam Brannen
 * @since 3.2
 */
public class ServletTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(ServletTestExecutionListener.class);


	/**
	 * Sets up thread-local state during the <em>test instance preparation</em>
	 * callback phase via Spring Web's {@link RequestContextHolder}.
	 *
	 * @see TestExecutionListener#prepareTestInstance(TestContext)
	 * @see #setUpRequestContextIfNecessary(TestContext)
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		setUpRequestContextIfNecessary(testContext);
	}

	/**
	 * Sets up thread-local state before each test method via Spring Web's
	 * {@link RequestContextHolder}.
	 *
	 * @see TestExecutionListener#beforeTestMethod(TestContext)
	 * @see #setUpRequestContextIfNecessary(TestContext)
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		setUpRequestContextIfNecessary(testContext);
	}

	/**
	 * Cleans up thread-local state after each test method by {@linkplain
	 * RequestContextHolder#resetRequestAttributes() resetting} Spring Web's
	 * {@code RequestContextHolder}.
	 *
	 * @see TestExecutionListener#afterTestMethod(TestContext)
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Resetting RequestContextHolder for test context %s.", testContext));
		}
		RequestContextHolder.resetRequestAttributes();
	}

	private void setUpRequestContextIfNecessary(TestContext testContext) {

		ApplicationContext context = testContext.getApplicationContext();

		if (context instanceof WebApplicationContext) {
			WebApplicationContext wac = (WebApplicationContext) context;
			ServletContext servletContext = wac.getServletContext();
			if (!(servletContext instanceof MockServletContext)) {
				throw new IllegalStateException(String.format(
					"The WebApplicationContext for test context %s must be configured with a MockServletContext.",
					testContext));
			}

			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
					"Setting up MockHttpServletRequest, MockHttpServletResponse, ServletWebRequest, and RequestContextHolder for test context %s.",
					testContext));
			}

			if (RequestContextHolder.getRequestAttributes() == null) {
				MockServletContext mockServletContext = (MockServletContext) servletContext;
				MockHttpServletRequest request = new MockHttpServletRequest(mockServletContext);
				MockHttpServletResponse response = new MockHttpServletResponse();
				ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);

				RequestContextHolder.setRequestAttributes(servletWebRequest);

				if (wac instanceof ConfigurableApplicationContext) {
					ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) wac;
					ConfigurableListableBeanFactory bf = configurableApplicationContext.getBeanFactory();
					bf.registerResolvableDependency(MockHttpServletResponse.class, response);
					bf.registerResolvableDependency(ServletWebRequest.class, servletWebRequest);
				}
			}
		}
	}

}
