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

package org.springframework.mock.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * Mock implementation of the {@link javax.servlet.http.HttpServletRequest} interface.
 *
 * <p>As of Spring 4.0, this set of mocks is designed on a Servlet 3.0 baseline.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rick Evans
 * @author Mark Fisher
 * @author Chris Beams
 * @author Sam Brannen
 * @since 1.0.2
 */
public class MockHttpServletRequest implements HttpServletRequest {

	/**
	 * The default protocol: 'http'.
	 */
	public static final String DEFAULT_PROTOCOL = "http";

	/**
	 * The default server address: '127.0.0.1'.
	 */
	public static final String DEFAULT_SERVER_ADDR = "127.0.0.1";

	/**
	 * The default server name: 'localhost'.
	 */
	public static final String DEFAULT_SERVER_NAME = "localhost";

	/**
	 * The default server port: '80'.
	 */
	public static final int DEFAULT_SERVER_PORT = 80;

	/**
	 * The default remote address: '127.0.0.1'.
	 */
	public static final String DEFAULT_REMOTE_ADDR = "127.0.0.1";

	/**
	 * The default remote host: 'localhost'.
	 */
	public static final String DEFAULT_REMOTE_HOST = "localhost";

	private static final String CONTENT_TYPE_HEADER = "Content-Type";

	private static final String CHARSET_PREFIX = "charset=";


	private boolean active = true;


	// ---------------------------------------------------------------------
	// ServletRequest properties
	// ---------------------------------------------------------------------

	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

	private String characterEncoding;

	private byte[] content;

	private String contentType;

	private final Map<String, String[]> parameters = new LinkedHashMap<String, String[]>(16);

	private String protocol = DEFAULT_PROTOCOL;

	private String scheme = DEFAULT_PROTOCOL;

	private String serverName = DEFAULT_SERVER_NAME;

	private int serverPort = DEFAULT_SERVER_PORT;

	private String remoteAddr = DEFAULT_REMOTE_ADDR;

	private String remoteHost = DEFAULT_REMOTE_HOST;

	/** List of locales in descending order */
	private final List<Locale> locales = new LinkedList<Locale>();

	private boolean secure = false;

	private final ServletContext servletContext;

	private int remotePort = DEFAULT_SERVER_PORT;

	private String localName = DEFAULT_SERVER_NAME;

	private String localAddr = DEFAULT_SERVER_ADDR;

	private int localPort = DEFAULT_SERVER_PORT;

	private boolean asyncStarted = false;

	private boolean asyncSupported = false;

	private MockAsyncContext asyncContext;

	private DispatcherType dispatcherType = DispatcherType.REQUEST;


	// ---------------------------------------------------------------------
	// HttpServletRequest properties
	// ---------------------------------------------------------------------

	private String authType;

	private Cookie[] cookies;

	private final Map<String, HeaderValueHolder> headers = new LinkedCaseInsensitiveMap<HeaderValueHolder>();

	private String method;

	private String pathInfo;

	private String contextPath = "";

	private String queryString;

	private String remoteUser;

	private final Set<String> userRoles = new HashSet<String>();

	private Principal userPrincipal;

	private String requestedSessionId;

	private String requestURI;

	private String servletPath = "";

	private HttpSession session;

	private boolean requestedSessionIdValid = true;

	private boolean requestedSessionIdFromCookie = true;

	private boolean requestedSessionIdFromURL = false;

	private final Map<String, Part> parts = new LinkedHashMap<String, Part>();


	// ---------------------------------------------------------------------
	// Constructors
	// ---------------------------------------------------------------------

	/**
	 * Create a new {@code MockHttpServletRequest} with a default
	 * {@link MockServletContext}.
	 * @see #MockHttpServletRequest(ServletContext, String, String)
	 */
	public MockHttpServletRequest() {
		this(null, "", "");
	}

	/**
	 * Create a new {@code MockHttpServletRequest} with a default
	 * {@link MockServletContext}.
	 * @param method the request method (may be {@code null})
	 * @param requestURI the request URI (may be {@code null})
	 * @see #setMethod
	 * @see #setRequestURI
	 * @see #MockHttpServletRequest(ServletContext, String, String)
	 */
	public MockHttpServletRequest(String method, String requestURI) {
		this(null, method, requestURI);
	}

	/**
	 * Create a new {@code MockHttpServletRequest} with the supplied {@link ServletContext}.
	 * @param servletContext the ServletContext that the request runs in
	 * (may be {@code null} to use a default {@link MockServletContext})
	 * @see #MockHttpServletRequest(ServletContext, String, String)
	 */
	public MockHttpServletRequest(ServletContext servletContext) {
		this(servletContext, "", "");
	}

	/**
	 * Create a new {@code MockHttpServletRequest} with the supplied {@link ServletContext},
	 * {@code method}, and {@code requestURI}.
	 * <p>The preferred locale will be set to {@link Locale#ENGLISH}.
	 * @param servletContext the ServletContext that the request runs in (may be
	 * {@code null} to use a default {@link MockServletContext})
	 * @param method the request method (may be {@code null})
	 * @param requestURI the request URI (may be {@code null})
	 * @see #setMethod
	 * @see #setRequestURI
	 * @see #setPreferredLocales
	 * @see MockServletContext
	 */
	public MockHttpServletRequest(ServletContext servletContext, String method, String requestURI) {
		this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
		this.method = method;
		this.requestURI = requestURI;
		this.locales.add(Locale.ENGLISH);
	}


	// ---------------------------------------------------------------------
	// Lifecycle methods
	// ---------------------------------------------------------------------

	/**
	 * Return the ServletContext that this request is associated with. (Not
	 * available in the standard HttpServletRequest interface for some reason.)
	 */
	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	/**
	 * Return whether this request is still active (that is, not completed yet).
	 */
	public boolean isActive() {
		return this.active;
	}

	/**
	 * Mark this request as completed, keeping its state.
	 */
	public void close() {
		this.active = false;
	}

	/**
	 * Invalidate this request, clearing its state.
	 */
	public void invalidate() {
		close();
		clearAttributes();
	}

	/**
	 * Check whether this request is still active (that is, not completed yet),
	 * throwing an IllegalStateException if not active anymore.
	 */
	protected void checkActive() throws IllegalStateException {
		if (!this.active) {
			throw new IllegalStateException("Request is not active anymore");
		}
	}


	// ---------------------------------------------------------------------
	// ServletRequest interface
	// ---------------------------------------------------------------------

	@Override
	public Object getAttribute(String name) {
		checkActive();
		return this.attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		checkActive();
		return Collections.enumeration(new LinkedHashSet<String>(this.attributes.keySet()));
	}

	@Override
	public String getCharacterEncoding() {
		return this.characterEncoding;
	}

	@Override
	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
		updateContentTypeHeader();
	}

	private void updateContentTypeHeader() {
		if (this.contentType != null) {
			StringBuilder sb = new StringBuilder(this.contentType);
			if (!this.contentType.toLowerCase().contains(CHARSET_PREFIX) && this.characterEncoding != null) {
				sb.append(";").append(CHARSET_PREFIX).append(this.characterEncoding);
			}
			doAddHeaderValue(CONTENT_TYPE_HEADER, sb.toString(), true);
		}
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	@Override
	public int getContentLength() {
		return (this.content != null ? this.content.length : -1);
	}

	public long getContentLengthLong() {
		return getContentLength();
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
		if (contentType != null) {
			int charsetIndex = contentType.toLowerCase().indexOf(CHARSET_PREFIX);
			if (charsetIndex != -1) {
				String encoding = contentType.substring(charsetIndex + CHARSET_PREFIX.length());
				this.characterEncoding = encoding;
			}
			updateContentTypeHeader();
		}
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public ServletInputStream getInputStream() {
		if (this.content != null) {
			return new DelegatingServletInputStream(new ByteArrayInputStream(this.content));
		}
		else {
			return null;
		}
	}

	/**
	 * Set a single value for the specified HTTP parameter.
	 * <p>If there are already one or more values registered for the given
	 * parameter name, they will be replaced.
	 */
	public void setParameter(String name, String value) {
		setParameter(name, new String[] {value});
	}

	/**
	 * Set an array of values for the specified HTTP parameter.
	 * <p>If there are already one or more values registered for the given
	 * parameter name, they will be replaced.
	 */
	public void setParameter(String name, String[] values) {
		Assert.notNull(name, "Parameter name must not be null");
		this.parameters.put(name, values);
	}

	/**
	 * Sets all provided parameters <strong>replacing</strong> any existing
	 * values for the provided parameter names. To add without replacing
	 * existing values, use {@link #addParameters(java.util.Map)}.
	 */
	@SuppressWarnings("rawtypes")
	public void setParameters(Map params) {
		Assert.notNull(params, "Parameter map must not be null");
		for (Object key : params.keySet()) {
			Assert.isInstanceOf(String.class, key,
					"Parameter map key must be of type [" + String.class.getName() + "]");
			Object value = params.get(key);
			if (value instanceof String) {
				this.setParameter((String) key, (String) value);
			}
			else if (value instanceof String[]) {
				this.setParameter((String) key, (String[]) value);
			}
			else {
				throw new IllegalArgumentException(
						"Parameter map value must be single value " + " or array of type [" + String.class.getName() + "]");
			}
		}
	}

	/**
	 * Add a single value for the specified HTTP parameter.
	 * <p>If there are already one or more values registered for the given
	 * parameter name, the given value will be added to the end of the list.
	 */
	public void addParameter(String name, String value) {
		addParameter(name, new String[] {value});
	}

	/**
	 * Add an array of values for the specified HTTP parameter.
	 * <p>If there are already one or more values registered for the given
	 * parameter name, the given values will be added to the end of the list.
	 */
	public void addParameter(String name, String[] values) {
		Assert.notNull(name, "Parameter name must not be null");
		String[] oldArr = this.parameters.get(name);
		if (oldArr != null) {
			String[] newArr = new String[oldArr.length + values.length];
			System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
			System.arraycopy(values, 0, newArr, oldArr.length, values.length);
			this.parameters.put(name, newArr);
		}
		else {
			this.parameters.put(name, values);
		}
	}

	/**
	 * Adds all provided parameters <strong>without</strong> replacing any
	 * existing values. To replace existing values, use
	 * {@link #setParameters(java.util.Map)}.
	 */
	@SuppressWarnings("rawtypes")
	public void addParameters(Map params) {
		Assert.notNull(params, "Parameter map must not be null");
		for (Object key : params.keySet()) {
			Assert.isInstanceOf(String.class, key,
					"Parameter map key must be of type [" + String.class.getName() + "]");
			Object value = params.get(key);
			if (value instanceof String) {
				this.addParameter((String) key, (String) value);
			}
			else if (value instanceof String[]) {
				this.addParameter((String) key, (String[]) value);
			}
			else {
				throw new IllegalArgumentException("Parameter map value must be single value " +
						" or array of type [" + String.class.getName() + "]");
			}
		}
	}

	/**
	 * Remove already registered values for the specified HTTP parameter, if any.
	 */
	public void removeParameter(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		this.parameters.remove(name);
	}

	/**
	 * Removes all existing parameters.
	 */
	public void removeAllParameters() {
		this.parameters.clear();
	}

	@Override
	public String getParameter(String name) {
		String[] arr = (name != null ? this.parameters.get(name) : null);
		return (arr != null && arr.length > 0 ? arr[0] : null);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(this.parameters.keySet());
	}

	@Override
	public String[] getParameterValues(String name) {
		return (name != null ? this.parameters.get(name) : null);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return Collections.unmodifiableMap(this.parameters);
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	@Override
	public String getProtocol() {
		return this.protocol;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	@Override
	public String getScheme() {
		return this.scheme;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	@Override
	public String getServerName() {
		return this.serverName;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	@Override
	public int getServerPort() {
		return this.serverPort;
	}

	@Override
	public BufferedReader getReader() throws UnsupportedEncodingException {
		if (this.content != null) {
			InputStream sourceStream = new ByteArrayInputStream(this.content);
			Reader sourceReader = (this.characterEncoding != null) ?
					new InputStreamReader(sourceStream, this.characterEncoding) : new InputStreamReader(sourceStream);
			return new BufferedReader(sourceReader);
		}
		else {
			return null;
		}
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	@Override
	public String getRemoteAddr() {
		return this.remoteAddr;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	@Override
	public String getRemoteHost() {
		return this.remoteHost;
	}

	@Override
	public void setAttribute(String name, Object value) {
		checkActive();
		Assert.notNull(name, "Attribute name must not be null");
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			this.attributes.remove(name);
		}
	}

	@Override
	public void removeAttribute(String name) {
		checkActive();
		Assert.notNull(name, "Attribute name must not be null");
		this.attributes.remove(name);
	}

	/**
	 * Clear all of this request's attributes.
	 */
	public void clearAttributes() {
		this.attributes.clear();
	}

	/**
	 * Add a new preferred locale, before any existing locales.
	 * @see #setPreferredLocales
	 */
	public void addPreferredLocale(Locale locale) {
		Assert.notNull(locale, "Locale must not be null");
		this.locales.add(0, locale);
	}

	/**
	 * Set the list of preferred locales, in descending order, effectively replacing
	 * any existing locales.
	 * @see #addPreferredLocale
	 * @since 3.2
	 */
	public void setPreferredLocales(List<Locale> locales) {
		Assert.notEmpty(locales, "Locale list must not be empty");
		this.locales.clear();
		this.locales.addAll(locales);
	}

	@Override
	public Locale getLocale() {
		return this.locales.get(0);
	}

	@Override
	public Enumeration<Locale> getLocales() {
		return Collections.enumeration(this.locales);
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	@Override
	public boolean isSecure() {
		return this.secure;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return new MockRequestDispatcher(path);
	}

	@Override
	@Deprecated
	public String getRealPath(String path) {
		return this.servletContext.getRealPath(path);
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	@Override
	public int getRemotePort() {
		return this.remotePort;
	}

	public void setLocalName(String localName) {
		this.localName = localName;
	}

	@Override
	public String getLocalName() {
		return this.localName;
	}

	public void setLocalAddr(String localAddr) {
		this.localAddr = localAddr;
	}

	@Override
	public String getLocalAddr() {
		return this.localAddr;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	@Override
	public int getLocalPort() {
		return this.localPort;
	}

	@Override
	public AsyncContext startAsync() {
		return startAsync(this, null);
	}

	@Override
	public AsyncContext startAsync(ServletRequest request, ServletResponse response) {
		if (!this.asyncSupported) {
			throw new IllegalStateException("Async not supported");
		}
		this.asyncStarted = true;
		this.asyncContext = new MockAsyncContext(request, response);
		return this.asyncContext;
	}

	public void setAsyncStarted(boolean asyncStarted) {
		this.asyncStarted = asyncStarted;
	}

	@Override
	public boolean isAsyncStarted() {
		return this.asyncStarted;
	}

	public void setAsyncSupported(boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	@Override
	public boolean isAsyncSupported() {
		return this.asyncSupported;
	}

	public void setAsyncContext(MockAsyncContext asyncContext) {
		this.asyncContext = asyncContext;
	}

	@Override
	public AsyncContext getAsyncContext() {
		return this.asyncContext;
	}

	public void setDispatcherType(DispatcherType dispatcherType) {
		this.dispatcherType = dispatcherType;
	}

	@Override
	public DispatcherType getDispatcherType() {
		return this.dispatcherType;
	}


	// ---------------------------------------------------------------------
	// HttpServletRequest interface
	// ---------------------------------------------------------------------

	public void setAuthType(String authType) {
		this.authType = authType;
	}

	@Override
	public String getAuthType() {
		return this.authType;
	}

	public void setCookies(Cookie... cookies) {
		this.cookies = cookies;
	}

	@Override
	public Cookie[] getCookies() {
		return this.cookies;
	}

	/**
	 * Add a header entry for the given name.
	 * <p>If there was no entry for that header name before, the value will be used
	 * as-is. In case of an existing entry, a String array will be created,
	 * adding the given value (more specifically, its toString representation)
	 * as further element.
	 * <p>Multiple values can only be stored as list of Strings, following the
	 * Servlet spec (see {@code getHeaders} accessor). As alternative to
	 * repeated {@code addHeader} calls for individual elements, you can
	 * use a single call with an entire array or Collection of values as
	 * parameter.
	 * @see #getHeaderNames
	 * @see #getHeader
	 * @see #getHeaders
	 * @see #getDateHeader
	 * @see #getIntHeader
	 */
	public void addHeader(String name, Object value) {
		if (CONTENT_TYPE_HEADER.equalsIgnoreCase(name)) {
			setContentType((String) value);
			return;
		}
		doAddHeaderValue(name, value, false);
	}

	@SuppressWarnings("rawtypes")
	private void doAddHeaderValue(String name, Object value, boolean replace) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		Assert.notNull(value, "Header value must not be null");
		if (header == null || replace) {
			header = new HeaderValueHolder();
			this.headers.put(name, header);
		}
		if (value instanceof Collection) {
			header.addValues((Collection) value);
		}
		else if (value.getClass().isArray()) {
			header.addValueArray(value);
		}
		else {
			header.addValue(value);
		}
	}

	@Override
	public long getDateHeader(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		Object value = (header != null ? header.getValue() : null);
		if (value instanceof Date) {
			return ((Date) value).getTime();
		}
		else if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		else if (value != null) {
			throw new IllegalArgumentException(
					"Value for header '" + name + "' is neither a Date nor a Number: " + value);
		}
		else {
			return -1L;
		}
	}

	@Override
	public String getHeader(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		return (header != null ? header.getStringValue() : null);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		return Collections.enumeration(header != null ? header.getStringValues() : new LinkedList<String>());
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(this.headers.keySet());
	}

	@Override
	public int getIntHeader(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		Object value = (header != null ? header.getValue() : null);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		else if (value instanceof String) {
			return Integer.parseInt((String) value);
		}
		else if (value != null) {
			throw new NumberFormatException("Value for header '" + name + "' is not a Number: " + value);
		}
		else {
			return -1;
		}
	}

	public void setMethod(String method) {
		this.method = method;
	}

	@Override
	public String getMethod() {
		return this.method;
	}

	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	@Override
	public String getPathInfo() {
		return this.pathInfo;
	}

	@Override
	public String getPathTranslated() {
		return (this.pathInfo != null ? getRealPath(this.pathInfo) : null);
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	@Override
	public String getContextPath() {
		return this.contextPath;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	@Override
	public String getQueryString() {
		return this.queryString;
	}

	public void setRemoteUser(String remoteUser) {
		this.remoteUser = remoteUser;
	}

	@Override
	public String getRemoteUser() {
		return this.remoteUser;
	}

	public void addUserRole(String role) {
		this.userRoles.add(role);
	}

	@Override
	public boolean isUserInRole(String role) {
		return (this.userRoles.contains(role) || (this.servletContext instanceof MockServletContext &&
				((MockServletContext) this.servletContext).getDeclaredRoles().contains(role)));
	}

	public void setUserPrincipal(Principal userPrincipal) {
		this.userPrincipal = userPrincipal;
	}

	@Override
	public Principal getUserPrincipal() {
		return this.userPrincipal;
	}

	public void setRequestedSessionId(String requestedSessionId) {
		this.requestedSessionId = requestedSessionId;
	}

	@Override
	public String getRequestedSessionId() {
		return this.requestedSessionId;
	}

	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}

	@Override
	public String getRequestURI() {
		return this.requestURI;
	}

	@Override
	public StringBuffer getRequestURL() {
		StringBuffer url = new StringBuffer(this.scheme).append("://").append(this.serverName);

		if (this.serverPort > 0
				&& (("http".equalsIgnoreCase(scheme) && this.serverPort != 80) || ("https".equalsIgnoreCase(scheme) && this.serverPort != 443))) {
			url.append(':').append(this.serverPort);
		}

		if (StringUtils.hasText(getRequestURI())) {
			url.append(getRequestURI());
		}

		return url;
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	@Override
	public String getServletPath() {
		return this.servletPath;
	}

	public void setSession(HttpSession session) {
		this.session = session;
		if (session instanceof MockHttpSession) {
			MockHttpSession mockSession = ((MockHttpSession) session);
			mockSession.access();
		}
	}

	@Override
	public HttpSession getSession(boolean create) {
		checkActive();
		// Reset session if invalidated.
		if (this.session instanceof MockHttpSession && ((MockHttpSession) this.session).isInvalid()) {
			this.session = null;
		}
		// Create new session if necessary.
		if (this.session == null && create) {
			this.session = new MockHttpSession(this.servletContext);
		}
		return this.session;
	}

	@Override
	public HttpSession getSession() {
		return getSession(true);
	}

	/**
	 * The implementation of this (Servlet 3.1+) method calls
	 * {@link MockHttpSession#changeSessionId()} if the session is a mock session.
	 * Otherwise it simply returns the current session id.
	 * @since 4.0.3
	 */
	public String changeSessionId() {
		Assert.isTrue(this.session != null, "The request does not have a session");
		if (this.session instanceof MockHttpSession) {
			return ((MockHttpSession) session).changeSessionId();
		}
		return this.session.getId();
	}

	public void setRequestedSessionIdValid(boolean requestedSessionIdValid) {
		this.requestedSessionIdValid = requestedSessionIdValid;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return this.requestedSessionIdValid;
	}

	public void setRequestedSessionIdFromCookie(boolean requestedSessionIdFromCookie) {
		this.requestedSessionIdFromCookie = requestedSessionIdFromCookie;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return this.requestedSessionIdFromCookie;
	}

	public void setRequestedSessionIdFromURL(boolean requestedSessionIdFromURL) {
		this.requestedSessionIdFromURL = requestedSessionIdFromURL;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return this.requestedSessionIdFromURL;
	}

	@Override
	@Deprecated
	public boolean isRequestedSessionIdFromUrl() {
		return isRequestedSessionIdFromURL();
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void login(String username, String password) throws ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void logout() throws ServletException {
		this.userPrincipal = null;
		this.remoteUser = null;
		this.authType = null;
	}

	public void addPart(Part part) {
		this.parts.put(part.getName(), part);
	}

	@Override
	public Part getPart(String name) throws IOException, IllegalStateException, ServletException {
		return this.parts.get(name);
	}

	@Override
	public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException {
		return this.parts.values();
	}

}
