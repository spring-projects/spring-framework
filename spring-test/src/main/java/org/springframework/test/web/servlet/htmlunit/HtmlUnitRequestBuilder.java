/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FormEncodingType;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import org.springframework.beans.Mergeable;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.SmartRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Internal class used to transform a {@link WebRequest} into a
 * {@link MockHttpServletRequest} using Spring MVC Test's {@link RequestBuilder}.
 *
 * <p>By default the first path segment of the URL is used as the context path.
 * To override this default see {@link #setContextPath(String)}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 * @see MockMvcWebConnection
 */
final class HtmlUnitRequestBuilder implements RequestBuilder, Mergeable {

	private final Map<String, MockHttpSession> sessions;

	private final WebClient webClient;

	private final WebRequest webRequest;

	@Nullable
	private String contextPath;

	@Nullable
	private RequestBuilder parentBuilder;

	@Nullable
	private SmartRequestBuilder parentPostProcessor;

	@Nullable
	private RequestPostProcessor forwardPostProcessor;


	/**
	 * Construct a new {@code HtmlUnitRequestBuilder}.
	 * @param sessions a {@link Map} from session {@linkplain HttpSession#getId() IDs}
	 * to currently managed {@link HttpSession} objects; never {@code null}
	 * @param webClient the WebClient for retrieving cookies
	 * @param webRequest the {@link WebRequest} to transform into a
	 * {@link MockHttpServletRequest}; never {@code null}
	 */
	public HtmlUnitRequestBuilder(Map<String, MockHttpSession> sessions, WebClient webClient, WebRequest webRequest) {
		Assert.notNull(sessions, "Sessions Map must not be null");
		Assert.notNull(webClient, "WebClient must not be null");
		Assert.notNull(webRequest, "WebRequest must not be null");
		this.sessions = sessions;
		this.webClient = webClient;
		this.webRequest = webRequest;
	}


	public MockHttpServletRequest buildRequest(ServletContext servletContext) {
		Charset charset = getCharset();
		String httpMethod = this.webRequest.getHttpMethod().name();
		UriComponents uriComponents = uriComponents();
		String path = uriComponents.getPath();

		MockHttpServletRequest request = new HtmlUnitMockHttpServletRequest(
				servletContext, httpMethod, (path != null ? path : ""));
		parent(request, this.parentBuilder);
		String host = uriComponents.getHost();
		request.setServerName(host != null ? host : "");  // needs to be first for additional headers
		authType(request);
		request.setCharacterEncoding(charset.name());
		content(request, charset);
		contextPath(request, uriComponents);
		contentType(request);
		cookies(request);
		headers(request);
		locales(request);
		servletPath(uriComponents, request);
		params(request, uriComponents);
		ports(uriComponents, request);
		request.setProtocol("HTTP/1.1");
		request.setQueryString(uriComponents.getQuery());
		String scheme = uriComponents.getScheme();
		request.setScheme(scheme != null ? scheme : "");
		request.setPathInfo(null);

		return postProcess(request);
	}

	private Charset getCharset() {
		Charset charset = this.webRequest.getCharset();
		return (charset != null ? charset : StandardCharsets.ISO_8859_1);
	}

	private MockHttpServletRequest postProcess(MockHttpServletRequest request) {
		if (this.parentPostProcessor != null) {
			request = this.parentPostProcessor.postProcessRequest(request);
		}
		if (this.forwardPostProcessor != null) {
			request = this.forwardPostProcessor.postProcessRequest(request);
		}
		return request;
	}

	private void parent(MockHttpServletRequest request, @Nullable RequestBuilder parent) {
		if (parent == null) {
			return;
		}

		MockHttpServletRequest parentRequest = parent.buildRequest(request.getServletContext());

		// session
		HttpSession parentSession = parentRequest.getSession(false);
		if (parentSession != null) {
			HttpSession localSession = request.getSession();
			Assert.state(localSession != null, "No local HttpSession");
			Enumeration<String> attrNames = parentSession.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = attrNames.nextElement();
				Object attrValue = parentSession.getAttribute(attrName);
				localSession.setAttribute(attrName, attrValue);
			}
		}

		// header
		Enumeration<String> headerNames = parentRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String attrName = headerNames.nextElement();
			Enumeration<String> attrValues = parentRequest.getHeaders(attrName);
			while (attrValues.hasMoreElements()) {
				String attrValue = attrValues.nextElement();
				request.addHeader(attrName, attrValue);
			}
		}

		// parameter
		Map<String, String[]> parentParams = parentRequest.getParameterMap();
		parentParams.forEach(request::addParameter);

		// cookie
		Cookie[] parentCookies = parentRequest.getCookies();
		if (!ObjectUtils.isEmpty(parentCookies)) {
			request.setCookies(parentCookies);
		}

		// request attribute
		Enumeration<String> parentAttrNames = parentRequest.getAttributeNames();
		while (parentAttrNames.hasMoreElements()) {
			String parentAttrName = parentAttrNames.nextElement();
			request.setAttribute(parentAttrName, parentRequest.getAttribute(parentAttrName));
		}
	}

	/**
	 * Set the contextPath to be used.
	 * <p>The value may be null in which case the first path segment of the
	 * URL is turned into the contextPath. Otherwise it must conform to
	 * {@link HttpServletRequest#getContextPath()} which states it can be
	 * an empty string, or it must start with a "/" and not end with a "/".
	 * @param contextPath a valid contextPath
	 * @throws IllegalArgumentException if the contextPath is not a valid
	 * {@link HttpServletRequest#getContextPath()}
	 */
	public void setContextPath(@Nullable String contextPath) {
		MockMvcWebConnection.validateContextPath(contextPath);
		this.contextPath = contextPath;
	}

	public void setForwardPostProcessor(RequestPostProcessor forwardPostProcessor) {
		this.forwardPostProcessor = forwardPostProcessor;
	}

	private void authType(MockHttpServletRequest request) {
		String authorization = header("Authorization");
		String[] authSplit = StringUtils.split(authorization, ": ");
		if (authSplit != null) {
			request.setAuthType(authSplit[0]);
		}
	}

	private void content(MockHttpServletRequest request, Charset charset) {
		String requestBody = this.webRequest.getRequestBody();
		if (requestBody == null) {
			return;
		}
		request.setContent(requestBody.getBytes(charset));
	}

	private void contentType(MockHttpServletRequest request) {
		String contentType = header("Content-Type");
		if (contentType == null) {
			FormEncodingType encodingType = this.webRequest.getEncodingType();
			if (encodingType != null) {
				contentType = encodingType.getName();
			}
		}
		request.setContentType(contentType != null ? contentType : MediaType.ALL_VALUE);
	}

	private void contextPath(MockHttpServletRequest request, UriComponents uriComponents) {
		if (this.contextPath == null) {
			List<String> pathSegments = uriComponents.getPathSegments();
			if (pathSegments.isEmpty()) {
				request.setContextPath("");
			}
			else {
				request.setContextPath("/" + pathSegments.get(0));
			}
		}
		else {
			String path = uriComponents.getPath();
			Assert.isTrue(path != null && path.startsWith(this.contextPath),
					() -> "\"" + uriComponents.getPath() +
							"\" should start with context path \"" + this.contextPath + "\"");
			request.setContextPath(this.contextPath);
		}
	}

	private void cookies(MockHttpServletRequest request) {
		List<Cookie> cookies = new ArrayList<>();

		String cookieHeaderValue = header("Cookie");
		if (cookieHeaderValue != null) {
			StringTokenizer tokens = new StringTokenizer(cookieHeaderValue, "=;");
			while (tokens.hasMoreTokens()) {
				String cookieName = tokens.nextToken().trim();
				Assert.isTrue(tokens.hasMoreTokens(),
						() -> "Expected value for cookie name '" + cookieName +
								"': full cookie header was [" + cookieHeaderValue + "]");
				String cookieValue = tokens.nextToken().trim();
				processCookie(request, cookies, new Cookie(cookieName, cookieValue));
			}
		}

		Set<com.gargoylesoftware.htmlunit.util.Cookie> managedCookies = this.webClient.getCookies(this.webRequest.getUrl());
		for (com.gargoylesoftware.htmlunit.util.Cookie cookie : managedCookies) {
			processCookie(request, cookies, new Cookie(cookie.getName(), cookie.getValue()));
		}

		Cookie[] parentCookies = request.getCookies();
		if (parentCookies != null) {
			for (Cookie cookie : parentCookies) {
				cookies.add(cookie);
			}
		}

		if (!ObjectUtils.isEmpty(cookies)) {
			request.setCookies(cookies.toArray(new Cookie[0]));
		}
	}

	private void processCookie(MockHttpServletRequest request, List<Cookie> cookies, Cookie cookie) {
		cookies.add(cookie);
		if ("JSESSIONID".equals(cookie.getName())) {
			request.setRequestedSessionId(cookie.getValue());
			request.setSession(httpSession(request, cookie.getValue()));
		}
	}

	@Nullable
	private String header(String headerName) {
		return this.webRequest.getAdditionalHeaders().get(headerName);
	}

	private void headers(MockHttpServletRequest request) {
		this.webRequest.getAdditionalHeaders().forEach(request::addHeader);
	}

	private MockHttpSession httpSession(MockHttpServletRequest request, final String sessionid) {
		MockHttpSession session;
		synchronized (this.sessions) {
			session = this.sessions.get(sessionid);
			if (session == null) {
				session = new HtmlUnitMockHttpSession(request, sessionid);
				session.setNew(true);
				synchronized (this.sessions) {
					this.sessions.put(sessionid, session);
				}
				addSessionCookie(request, sessionid);
			}
			else {
				session.setNew(false);
			}
		}
		return session;
	}

	private void addSessionCookie(MockHttpServletRequest request, String sessionid) {
		getCookieManager().addCookie(createCookie(request, sessionid));
	}

	private void removeSessionCookie(MockHttpServletRequest request, String sessionid) {
		getCookieManager().removeCookie(createCookie(request, sessionid));
	}

	private com.gargoylesoftware.htmlunit.util.Cookie createCookie(MockHttpServletRequest request, String sessionid) {
		return new com.gargoylesoftware.htmlunit.util.Cookie(request.getServerName(), "JSESSIONID", sessionid,
				request.getContextPath() + "/", null, request.isSecure(), true);
	}

	private void locales(MockHttpServletRequest request) {
		String locale = header("Accept-Language");
		if (locale == null) {
			request.addPreferredLocale(Locale.getDefault());
		}
	}

	private void params(MockHttpServletRequest request, UriComponents uriComponents) {
		uriComponents.getQueryParams().forEach((name, values) -> {
			String urlDecodedName = urlDecode(name);
			values.forEach(value -> {
				value = (value != null ? urlDecode(value) : "");
				request.addParameter(urlDecodedName, value);
			});
		});
		for (NameValuePair param : this.webRequest.getRequestParameters()) {
			request.addParameter(param.getName(), param.getValue());
		}
	}

	private String urlDecode(String value) {
		try {
			return URLDecoder.decode(value, "UTF-8");
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void servletPath(MockHttpServletRequest request, String requestPath) {
		String servletPath = requestPath.substring(request.getContextPath().length());
		request.setServletPath(servletPath);
	}

	private void servletPath(UriComponents uriComponents, MockHttpServletRequest request) {
		if ("".equals(request.getPathInfo())) {
			request.setPathInfo(null);
		}
		String path = uriComponents.getPath();
		servletPath(request, (path != null ? path : ""));
	}

	private void ports(UriComponents uriComponents, MockHttpServletRequest request) {
		int serverPort = uriComponents.getPort();
		request.setServerPort(serverPort);
		if (serverPort == -1) {
			int portConnection = this.webRequest.getUrl().getDefaultPort();
			request.setLocalPort(serverPort);
			request.setRemotePort(portConnection);
		}
		else {
			request.setRemotePort(serverPort);
		}
	}

	private UriComponents uriComponents() {
		URL url = this.webRequest.getUrl();
		return UriComponentsBuilder.fromUriString(url.toExternalForm()).build();
	}

	@Override
	public boolean isMergeEnabled() {
		return true;
	}

	@Override
	public Object merge(@Nullable Object parent) {
		if (parent instanceof RequestBuilder) {
			if (parent instanceof MockHttpServletRequestBuilder) {
				MockHttpServletRequestBuilder copiedParent = MockMvcRequestBuilders.get("/");
				copiedParent.merge(parent);
				this.parentBuilder = copiedParent;
			}
			else {
				this.parentBuilder = (RequestBuilder) parent;
			}
			if (parent instanceof SmartRequestBuilder) {
				this.parentPostProcessor = (SmartRequestBuilder) parent;
			}
		}
		return this;
	}

	private CookieManager getCookieManager() {
		return this.webClient.getCookieManager();
	}


	/**
	 * An extension to {@link MockHttpServletRequest} that ensures that when a
	 * new {@link HttpSession} is created, it is added to the managed sessions.
	 */
	private final class HtmlUnitMockHttpServletRequest extends MockHttpServletRequest {

		public HtmlUnitMockHttpServletRequest(ServletContext servletContext, String method, String requestURI) {
			super(servletContext, method, requestURI);
		}

		@Override
		public HttpSession getSession(boolean create) {
			HttpSession session = super.getSession(false);
			if (session == null && create) {
				HtmlUnitMockHttpSession newSession = new HtmlUnitMockHttpSession(this);
				setSession(newSession);
				newSession.setNew(true);
				String sessionid = newSession.getId();
				synchronized (HtmlUnitRequestBuilder.this.sessions) {
					HtmlUnitRequestBuilder.this.sessions.put(sessionid, newSession);
				}
				addSessionCookie(this, sessionid);
				session = newSession;
			}
			return session;
		}
	}


	/**
	 * An extension to {@link MockHttpSession} that ensures when
	 * {@link #invalidate()} is called that the {@link HttpSession}
	 * is removed from the managed sessions.
	 */
	private final class HtmlUnitMockHttpSession extends MockHttpSession {

		private final MockHttpServletRequest request;

		public HtmlUnitMockHttpSession(MockHttpServletRequest request) {
			super(request.getServletContext());
			this.request = request;
		}

		private HtmlUnitMockHttpSession(MockHttpServletRequest request, String id) {
			super(request.getServletContext(), id);
			this.request = request;
		}

		@Override
		public void invalidate() {
			super.invalidate();
			synchronized (HtmlUnitRequestBuilder.this.sessions) {
				HtmlUnitRequestBuilder.this.sessions.remove(getId());
			}
			removeSessionCookie(request, getId());
		}
	}

}
