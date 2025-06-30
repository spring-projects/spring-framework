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

package org.springframework.test.web.servlet.htmlunit;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.htmlunit.CookieManager;
import org.htmlunit.WebClient;
import org.htmlunit.WebConnection;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.util.Cookie;
import org.jspecify.annotations.Nullable;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.Assert;

/**
 * {@code MockMvcWebConnection} enables {@link MockMvc} to transform a
 * {@link WebRequest} into a {@link WebResponse}.
 * <p>This is the core integration with <a href="https://htmlunit.sourceforge.io/">HtmlUnit</a>.
 * <p>Example usage can be seen below.
 *
 * <pre class="code">
 * WebClient webClient = new WebClient();
 * MockMvc mockMvc = ...
 * MockMvcWebConnection webConnection = new MockMvcWebConnection(mockMvc, webClient);
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

	private final Map<String, MockHttpSession> sessions = new HashMap<>();

	private final MockMvc mockMvc;

	private final @Nullable String contextPath;

	private WebClient webClient;

	private static final int MAX_FORWARDS = 100;


	/**
	 * Create a new instance that assumes the context path of the application
	 * is {@code ""} (i.e., the root context).
	 * <p>For example, the URL {@code http://localhost/test/this} would use
	 * {@code ""} as the context path.
	 * @param mockMvc the {@code MockMvc} instance to use; never {@code null}
	 * @param webClient the {@link WebClient} to use. never {@code null}
	 */
	public MockMvcWebConnection(MockMvc mockMvc, WebClient webClient) {
		this(mockMvc, webClient, "");
	}

	/**
	 * Create a new instance with the specified context path.
	 * <p>The path may be {@code null} in which case the first path segment
	 * of the URL is turned into the contextPath. Otherwise it must conform
	 * to {@link jakarta.servlet.http.HttpServletRequest#getContextPath()}
	 * which states that it can be an empty string and otherwise must start
	 * with a "/" character and not end with a "/" character.
	 * @param mockMvc the {@code MockMvc} instance to use (never {@code null})
	 * @param webClient the {@link WebClient} to use (never {@code null})
	 * @param contextPath the contextPath to use
	 */
	public MockMvcWebConnection(MockMvc mockMvc, WebClient webClient, @Nullable String contextPath) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		Assert.notNull(webClient, "WebClient must not be null");
		validateContextPath(contextPath);

		this.webClient = webClient;
		this.mockMvc = mockMvc;
		this.contextPath = contextPath;
	}

	/**
	 * Validate the supplied {@code contextPath}.
	 * <p>If the value is not {@code null}, it must conform to
	 * {@link jakarta.servlet.http.HttpServletRequest#getContextPath()} which
	 * states that it can be an empty string and otherwise must start with
	 * a "/" character and not end with a "/" character.
	 * @param contextPath the path to validate
	 */
	static void validateContextPath(@Nullable String contextPath) {
		if (contextPath == null || contextPath.isEmpty()) {
			return;
		}
		Assert.isTrue(contextPath.startsWith("/"), () -> "contextPath '" + contextPath + "' must start with '/'.");
		Assert.isTrue(!contextPath.endsWith("/"), () -> "contextPath '" + contextPath + "' must not end with '/'.");
	}


	public void setWebClient(WebClient webClient) {
		Assert.notNull(webClient, "WebClient must not be null");
		this.webClient = webClient;
	}


	@Override
	public WebResponse getResponse(WebRequest webRequest) throws IOException {
		long startTime = System.currentTimeMillis();
		HtmlUnitRequestBuilder requestBuilder = new HtmlUnitRequestBuilder(this.sessions, this.webClient, webRequest);
		requestBuilder.setContextPath(this.contextPath);

		MockHttpServletResponse httpServletResponse = getResponse(requestBuilder);
		String forwardedUrl = httpServletResponse.getForwardedUrl();
		int forwards = 0;
		while (forwardedUrl != null && forwards < MAX_FORWARDS) {
			requestBuilder.setForwardPostProcessor(new ForwardRequestPostProcessor(forwardedUrl));
			httpServletResponse = getResponse(requestBuilder);
			forwardedUrl = httpServletResponse.getForwardedUrl();
			forwards += 1;
		}
		if (forwards == MAX_FORWARDS) {
			throw new IllegalStateException("Forwarded " + forwards + " times in a row, potential infinite forward loop");
		}
		storeCookies(webRequest, httpServletResponse.getCookies());

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

	private void storeCookies(WebRequest webRequest, jakarta.servlet.http.Cookie[] cookies) {
		Date now = new Date();
		CookieManager cookieManager = this.webClient.getCookieManager();
		for (jakarta.servlet.http.Cookie cookie : cookies) {
			if (cookie.getDomain() == null) {
				cookie.setDomain(webRequest.getUrl().getHost());
			}
			Cookie toManage = createCookie(cookie);
			Date expires = toManage.getExpires();
			if (expires == null || expires.after(now)) {
				cookieManager.addCookie(toManage);
			}
			else {
				cookieManager.removeCookie(toManage);
			}
		}
	}

	@SuppressWarnings("removal")
	private static Cookie createCookie(jakarta.servlet.http.Cookie cookie) {
		Date expires = null;
		if (cookie.getMaxAge() > -1) {
			expires = new Date(System.currentTimeMillis() + cookie.getMaxAge() * 1000);
		}
		BasicClientCookie result = new BasicClientCookie(cookie.getName(), cookie.getValue());
		result.setDomain(cookie.getDomain());
		result.setComment(cookie.getComment());
		result.setExpiryDate(expires);
		result.setPath(cookie.getPath());
		result.setSecure(cookie.getSecure());
		if (cookie.isHttpOnly()) {
			result.setAttribute("httponly", "true");
		}
		return new Cookie(result);
	}

	@Override
	public void close() {
	}

}
