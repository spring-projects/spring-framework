/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.web.servlet.htmlunit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.Assert;

/**
 * {@code MockMvcWebConnection} enables {@link MockMvc} to transform a
 * {@link WebRequest} into a {@link WebResponse}.
 * <p>This is the core integration with <a href="http://htmlunit.sourceforge.net/">HtmlUnit</a>.
 * <p>Example usage can be seen below.
 *
 * <pre class="code">
 * WebClient webClient = new WebClient();
 * MockMvc mockMvc = ...
 * MockMvcWebConnection webConnection = new MockMvcWebConnection(mockMvc);
 * mockConnection.setWebClient(webClient);
 * webClient.setWebConnection(webConnection);
 *
 * // Use webClient as normal ...
 * </pre>
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 * @see org.springframework.test.web.servlet.htmlunit.webdriver.WebConnectionHtmlUnitDriver
 */
public final class MockMvcWebConnection implements WebConnection {

	private final Map<String, MockHttpSession> sessions = new HashMap<String, MockHttpSession>();

	private final MockMvc mockMvc;

	private final String contextPath;

	private WebClient webClient;


	/**
	 * Create a new instance that assumes the context path of the application
	 * is {@code ""} (i.e., the root context).
	 * <p>For example, the URL {@code http://localhost/test/this} would use
	 * {@code ""} as the context path.
	 * @param mockMvc the {@code MockMvc} instance to use; never {@code null}
	 */
	public MockMvcWebConnection(MockMvc mockMvc) {
		this(mockMvc, "");
	}

	/**
	 * Create a new instance with the specified context path.
	 * <p>The path may be {@code null} in which case the first path segment
	 * of the URL is turned into the contextPath. Otherwise it must conform
	 * to {@link javax.servlet.http.HttpServletRequest#getContextPath()}
	 * which states that it can be an empty string and otherwise must start
	 * with a "/" character and not end with a "/" character.
	 * @param mockMvc the {@code MockMvc} instance to use; never {@code null}
	 * @param contextPath the contextPath to use
	 */
	public MockMvcWebConnection(MockMvc mockMvc, String contextPath) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		validateContextPath(contextPath);

		this.webClient = new WebClient();
		this.mockMvc = mockMvc;
		this.contextPath = contextPath;
	}


	public void setWebClient(WebClient webClient) {
		Assert.notNull(webClient, "WebClient must not be null");
		this.webClient = webClient;
	}


	public WebResponse getResponse(WebRequest webRequest) throws IOException {
		long startTime = System.currentTimeMillis();
		HtmlUnitRequestBuilder requestBuilder = new HtmlUnitRequestBuilder(this.sessions, this.webClient, webRequest);
		requestBuilder.setContextPath(this.contextPath);

		MockHttpServletResponse httpServletResponse = getResponse(requestBuilder);
		String forwardedUrl = httpServletResponse.getForwardedUrl();
		while (forwardedUrl != null) {
			requestBuilder.setForwardPostProcessor(new ForwardRequestPostProcessor(forwardedUrl));
			httpServletResponse = getResponse(requestBuilder);
			forwardedUrl = httpServletResponse.getForwardedUrl();
		}

		return new MockWebResponseBuilder(startTime, webRequest, httpServletResponse).build();
	}

	private MockHttpServletResponse getResponse(RequestBuilder requestBuilder) throws IOException {
		ResultActions resultActions;
		try {
			resultActions = this.mockMvc.perform(requestBuilder);
		}
		catch (Exception ex) {
			throw new IOException(ex);
		}

		return resultActions.andReturn().getResponse();
	}

	@Override
	public void close() {
	}


	/**
	 * Validate the supplied {@code contextPath}.
	 * <p>If the value is not {@code null}, it must conform to
	 * {@link javax.servlet.http.HttpServletRequest#getContextPath()} which
	 * states that it can be an empty string and otherwise must start with
	 * a "/" character and not end with a "/" character.
	 * @param contextPath the path to validate
	 */
	static void validateContextPath(String contextPath) {
		if (contextPath == null || "".equals(contextPath)) {
			return;
		}
		if (!contextPath.startsWith("/")) {
			throw new IllegalArgumentException("contextPath '" + contextPath + "' must start with '/'.");
		}
		if (contextPath.endsWith("/")) {
			throw new IllegalArgumentException("contextPath '" + contextPath + "' must not end with '/'.");
		}
	}

}
