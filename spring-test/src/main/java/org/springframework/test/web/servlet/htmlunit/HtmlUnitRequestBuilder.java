/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * <p>By default the first path segment of the URL is used as the contextPath.
 * To override this default see {@link #setContextPath(String)}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 * @see MockMvcWebConnection
 */
final class HtmlUnitRequestBuilder implements RequestBuilder, Mergeable {

	private static final Pattern LOCALE_PATTERN = Pattern.compile("^\\s*(\\w{2})(?:-(\\w{2}))?(?:;q=(\\d+\\.\\d+))?$");

	private final Map<String, MockHttpSession> sessions;

	private final WebClient webClient;

	private final WebRequest webRequest;

	private String contextPath;

	private RequestBuilder parentBuilder;

	private SmartRequestBuilder parentPostProcessor;

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
		String charset = getCharset();
		String httpMethod = this.webRequest.getHttpMethod().name();
		UriComponents uriComponents = uriComponents();

		MockHttpServletRequest request = new HtmlUnitMockHttpServletRequest(
				servletContext, httpMethod, uriComponents.getPath());
		parent(request, this.parentBuilder);
		request.setServerName(uriComponents.getHost()); // needs to be first for additional headers
		authType(request);
		request.setCharacterEncoding(charset);
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
		request.setScheme(uriComponents.getScheme());
		request.setPathInfo(null);

		return postProcess(request);
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

	private void parent(MockHttpServletRequest request, RequestBuilder parent) {
		if (parent == null) {
			return;
		}

		MockHttpServletRequest parentRequest = parent.buildRequest(request.getServletContext());

		// session
		HttpSession parentSession = parentRequest.getSession(false);
		if (parentSession != null) {
			Enumeration<String> attrNames = parentSession.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = attrNames.nextElement();
				Object attrValue = parentSession.getAttribute(attrName);
				request.getSession().setAttribute(attrName, attrValue);
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
		for (Map.Entry<String, String[]> parentParam : parentParams.entrySet()) {
			String paramName = parentParam.getKey();
			String[] paramValues = parentParam.getValue();
			request.addParameter(paramName, paramValues);
		}

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
	public void setContextPath(String contextPath) {
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

	private void content(MockHttpServletRequest request, String charset) {
		String requestBody = this.webRequest.getRequestBody();
		if (requestBody == null) {
			return;
		}
		try {
			request.setContent(requestBody.getBytes(charset));
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
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
			if (!uriComponents.getPath().startsWith(this.contextPath)) {
				throw new IllegalArgumentException(uriComponents.getPath() + " should start with contextPath " +
						this.contextPath);
			}
			request.setContextPath(this.contextPath);
		}
	}

	private void cookies(MockHttpServletRequest request) {
		List<Cookie> cookies = new ArrayList<Cookie>();

		String cookieHeaderValue = header("Cookie");
		if (cookieHeaderValue != null) {
			StringTokenizer tokens = new StringTokenizer(cookieHeaderValue, "=;");
			while (tokens.hasMoreTokens()) {
				String cookieName = tokens.nextToken().trim();
				if (!tokens.hasMoreTokens()) {
					throw new IllegalArgumentException("Expected value for cookie name '" + cookieName +
							"'. Full cookie was " + cookieHeaderValue);
				}
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
			request.setCookies(cookies.toArray(new Cookie[cookies.size()]));
		}
	}

	private void processCookie(MockHttpServletRequest request, List<Cookie> cookies, Cookie cookie) {
		cookies.add(cookie);
		if ("JSESSIONID".equals(cookie.getName())) {
			request.setRequestedSessionId(cookie.getValue());
			request.setSession(httpSession(request, cookie.getValue()));
		}
	}

	private String getCharset() {
		String charset = this.webRequest.getCharset();
		if (charset == null) {
			return "ISO-8859-1";
		}
		return charset;
	}

	private String header(String headerName) {
		return this.webRequest.getAdditionalHeaders().get(headerName);
	}

	private void headers(MockHttpServletRequest request) {
		for (Entry<String, String> header : this.webRequest.getAdditionalHeaders().entrySet()) {
			request.addHeader(header.getKey(), header.getValue());
		}
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
		else {
			String[] tokens = StringUtils.tokenizeToStringArray(locale, ",");
			for (int i = tokens.length - 1; i >= 0; i--) {
				request.addPreferredLocale(parseLocale(tokens[i]));
			}
		}
	}

	private void params(MockHttpServletRequest request, UriComponents uriComponents) {
		for (Entry<String, List<String>> entry : uriComponents.getQueryParams().entrySet()) {
			String name = entry.getKey();
			String urlDecodedName = urlDecode(name);
			for (String value : entry.getValue()) {
				value = (value != null ? urlDecode(value) : "");
				request.addParameter(urlDecodedName, value);
			}
		}
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

	private Locale parseLocale(String locale) {
		Matcher matcher = LOCALE_PATTERN.matcher(locale);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid locale " + locale);
		}
		String language = matcher.group(1);
		String country = matcher.group(2);
		if (country == null) {
			country = "";
		}
		String qualifier = matcher.group(3);
		if (qualifier == null) {
			qualifier = "";
		}
		return new Locale(language, country, qualifier);
	}

	private void servletPath(MockHttpServletRequest request, String requestPath) {
		String servletPath = requestPath.substring(request.getContextPath().length());
		if ("".equals(servletPath)) {
			servletPath = null;
		}
		request.setServletPath(servletPath);
	}

	private void servletPath(UriComponents uriComponents, MockHttpServletRequest request) {
		if ("".equals(request.getPathInfo())) {
			request.setPathInfo(null);
		}
		servletPath(request, uriComponents.getPath());
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
	public Object merge(Object parent) {
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

		public HttpSession getSession() {
			return super.getSession();
		}

		public void setSession(HttpSession session) {
			super.setSession(session);
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

		public void invalidate() {
			super.invalidate();
			synchronized (HtmlUnitRequestBuilder.this.sessions) {
				HtmlUnitRequestBuilder.this.sessions.remove(getId());
			}
			removeSessionCookie(request, getId());
		}
	}

}
