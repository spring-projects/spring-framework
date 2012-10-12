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

package org.springframework.test.web.mock.servlet.request;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.Mergeable;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.mock.servlet.MockMvc;
import org.springframework.test.web.mock.servlet.RequestBuilder;
import org.springframework.test.web.mock.servlet.setup.MockMvcBuilders;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * Default builder for {@link MockHttpServletRequest} required as input to
 * perform request in {@link MockMvc}.
 *
 * <p>Application tests will typically access this builder through the static
 * factory methods in {@link MockMvcBuilders}.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
  * @since 3.2
*/
public class MockHttpServletRequestBuilder implements RequestBuilder, Mergeable {

	private final UriComponents uriComponents;

	private final HttpMethod method;

	private final MultiValueMap<String, Object> headers = new LinkedMultiValueMap<String, Object>();

	private String contentType;

	private byte[] content;

	private final MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();

	private final List<Cookie> cookies = new ArrayList<Cookie>();

	private Locale locale;

	private String characterEncoding;

	private Principal principal;

	private Boolean secure;

	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

	private MockHttpSession session;

	private final Map<String, Object> sessionAttributes = new LinkedHashMap<String, Object>();

	private final Map<String, Object> flashAttributes = new LinkedHashMap<String, Object>();

	private String contextPath = "";

	private String servletPath = "";

	private String pathInfo = ValueConstants.DEFAULT_NONE;

	private final List<RequestPostProcessor> postProcessors =
			new ArrayList<RequestPostProcessor>();


	/**
	 * Package private constructor. To get an instance, use static factory
	 * methods in {@link MockMvcRequestBuilders}.
	 *
	 * <p>Although this class cannot be extended, additional ways to initialize
	 * the {@code MockHttpServletRequest} can be plugged in via
	 * {@link #with(RequestPostProcessor)}.
	 *
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	MockHttpServletRequestBuilder(HttpMethod httpMethod, String urlTemplate, Object... urlVariables) {

		Assert.notNull(urlTemplate, "uriTemplate is required");
		Assert.notNull(httpMethod, "httpMethod is required");

		this.uriComponents = UriComponentsBuilder.fromUriString(urlTemplate).buildAndExpand(urlVariables).encode();
		this.method = httpMethod;
	}

	/**
	 * Add a request parameter to the {@link MockHttpServletRequest}.
	 * If called more than once, the new values are added.
	 *
	 * @param name the parameter name
	 * @param values one or more values
	 */
	public MockHttpServletRequestBuilder param(String name, String... values) {
		addToMultiValueMap(this.parameters, name, values);
		return this;
	}

	/**
	 * Add a header to the request. Values are always added.
	 *
	 * @param name the header name
	 * @param values one or more header values
	 */
	public MockHttpServletRequestBuilder header(String name, Object... values) {
		addToMultiValueMap(this.headers, name, values);
		return this;
	}

	/**
	 * Add all headers to the request. Values are always added.
	 *
	 * @param httpHeaders the headers and values to add
	 */
	public MockHttpServletRequestBuilder headers(HttpHeaders httpHeaders) {
		for (String name : httpHeaders.keySet()) {
			Object[] values = ObjectUtils.toObjectArray(httpHeaders.get(name).toArray());
			addToMultiValueMap(this.headers, name, values);
		}
		return this;
	}

	/**
	 * Set the 'Content-Type' header of the request.
	 *
	 * @param mediaType the content type
	 */
	public MockHttpServletRequestBuilder contentType(MediaType mediaType) {
		Assert.notNull(mediaType, "'contentType' must not be null");
		this.contentType = mediaType.toString();
		this.headers.set("Content-Type", this.contentType);
		return this;
	}

	/**
	 * Set the 'Accept' header to the given media type(s).
	 *
	 * @param mediaTypes one or more media types
	 */
	public MockHttpServletRequestBuilder accept(MediaType... mediaTypes) {
		Assert.notEmpty(mediaTypes, "No 'Accept' media types");
		this.headers.set("Accept", MediaType.toString(Arrays.asList(mediaTypes)));
		return this;
	}

	/**
	 * Set the request body.
	 *
	 * @param content the body content
	 */
	public MockHttpServletRequestBuilder content(byte[] content) {
		this.content = content;
		return this;
	}

	/**
	 * Add the given cookies to the request. Cookies are always added.
	 *
	 * @param cookies the cookies to add
	 */
	public MockHttpServletRequestBuilder cookie(Cookie... cookies) {
		Assert.notNull(cookies, "'cookies' must not be null");
		Assert.notEmpty(cookies, "'cookies' must not be empty");
		this.cookies.addAll(Arrays.asList(cookies));
		return this;
	}

	/**
	 * Set the locale of the request.
	 *
	 * @param locale the locale
	 */
	public MockHttpServletRequestBuilder locale(Locale locale) {
		this.locale = locale;
		return this;
	}

	/**
	 * Set the character encoding of the request.
	 *
	 * @param encoding the character encoding
	 */
	public MockHttpServletRequestBuilder characterEncoding(String encoding) {
		this.characterEncoding = encoding;
		return this;
	}

	/**
	 * Set a request attribute.
	 *
	 * @param name the attribute name
	 * @param value the attribute value
	 */
	public MockHttpServletRequestBuilder requestAttr(String name, Object value) {
		addAttributeToMap(this.attributes, name, value);
		return this;
	}

	/**
	 * Set a session attribute.
	 *
	 * @param name the session attribute name
	 * @param value the session attribute value
	 */
	public MockHttpServletRequestBuilder sessionAttr(String name, Object value) {
		addAttributeToMap(this.sessionAttributes, name, value);
		return this;
	}

	/**
	 * Set session attributes.
	 *
	 * @param sessionAttributes the session attributes
	 */
	public MockHttpServletRequestBuilder sessionAttrs(Map<String, Object> sessionAttributes) {
		Assert.notEmpty(sessionAttributes, "'sessionAttrs' must not be empty");
		for (String name : sessionAttributes.keySet()) {
			sessionAttr(name, sessionAttributes.get(name));
		}
		return this;
	}

	/**
	 * Set an "input" flash attribute.
	 *
	 * @param name the flash attribute name
	 * @param value the flash attribute value
	 */
	public MockHttpServletRequestBuilder flashAttr(String name, Object value) {
		addAttributeToMap(this.flashAttributes, name, value);
		return this;
	}

	/**
	 * Set flash attributes.
	 *
	 * @param flashAttributes the flash attributes
	 */
	public MockHttpServletRequestBuilder flashAttrs(Map<String, Object> flashAttributes) {
		Assert.notEmpty(flashAttributes, "'flashAttrs' must not be empty");
		for (String name : flashAttributes.keySet()) {
			flashAttr(name, flashAttributes.get(name));
		}
		return this;
	}

	/**
	 * Set the HTTP session to use, possibly re-used across requests.
	 *
	 * <p>Individual attributes provided via {@link #sessionAttr(String, Object)}
	 * override the content of the session provided here.
	 *
	 * @param session the HTTP session
	 */
	public MockHttpServletRequestBuilder session(MockHttpSession session) {
		Assert.notNull(session, "'session' must not be null");
		this.session = session;
		return this;
	}

	/**
	 * Set the principal of the request.
	 *
	 * @param principal the principal
	 */
	public MockHttpServletRequestBuilder principal(Principal principal) {
		Assert.notNull(principal, "'principal' must not be null");
		this.principal = principal;
		return this;
	}

	/**
	 * Specify the portion of the requestURI that represents the context path.
	 * The context path, if specified, must match to the start of the request
	 * URI.
	 *
	 * <p>In most cases, tests can be written by omitting the context path from
	 * the requestURI. This is because most applications don't actually depend
	 * on the name under which they're deployed. If specified here, the context
	 * path must start with a "/" and must not end with a "/".
	 *
	 * @see <a
	 * href="http://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#getContextPath%28%29">HttpServletRequest.getContextPath()</a>
	 */
	public MockHttpServletRequestBuilder contextPath(String contextPath) {
		if (StringUtils.hasText(contextPath)) {
			Assert.isTrue(contextPath.startsWith("/"), "Context path must start with a '/'");
			Assert.isTrue(!contextPath.endsWith("/"), "Context path must not end with a '/'");
		}
		this.contextPath = (contextPath != null) ? contextPath : "";
		return this;
	}

	/**
	 * Specify the portion of the requestURI that represents the path to which
	 * the Servlet is mapped. This is typically a portion of the requestURI
	 * after the context path.
	 *
	 * <p>In most cases, tests can be written by omitting the servlet path from
	 * the requestURI. This is because most applications don't actually depend
	 * on the prefix to which a servlet is mapped. For example if a Servlet is
	 * mapped to {@code "/main/*"}, tests can be written with the requestURI
	 * {@code "/accounts/1"} as opposed to {@code "/main/accounts/1"}.
	 * If specified here, the servletPath must start with a "/" and must not
	 * end with a "/".
	 *
	 * @see <a
	 * href="http://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#getServletPath%28%29">HttpServletRequest.getServletPath()</a>
	 */
	public MockHttpServletRequestBuilder servletPath(String servletPath) {
		if (StringUtils.hasText(servletPath)) {
			Assert.isTrue(servletPath.startsWith("/"), "Servlet path must start with a '/'");
			Assert.isTrue(!servletPath.endsWith("/"), "Servlet path must not end with a '/'");
		}
		this.servletPath = (servletPath != null) ? servletPath : "";
		return this;
	}

	/**
	 * Specify the portion of the requestURI that represents the pathInfo.
	 *
	 * <p>If left unspecified (recommended), the pathInfo will be automatically
	 * derived by removing the contextPath and the servletPath from the
	 * requestURI and using any remaining part. If specified here, the pathInfo
	 * must start with a "/".
	 *
	 * <p>If specified, the pathInfo will be used as is.
	 *
	 * @see <a
	 * href="http://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#getPathInfo%28%29">HttpServletRequest.getServletPath()</a>
	 */
	public MockHttpServletRequestBuilder pathInfo(String pathInfo) {
		if (StringUtils.hasText(pathInfo)) {
			Assert.isTrue(pathInfo.startsWith("/"), "pathInfo must start with a '/'");
		}
		this.pathInfo = pathInfo;
		return this;
	}

	/**
	 * Set the secure property of the {@link ServletRequest} indicating use of a
	 * secure channel, such as HTTPS.
	 *
	 * @param secure whether the request is using a secure channel
	 */
	public MockHttpServletRequestBuilder secure(boolean secure){
		this.secure = secure;
		return this;
	}

	/**
	 * An extension point for further initialization of {@link MockHttpServletRequest}
	 * in ways not built directly into the {@code MockHttpServletRequestBuilder}.
	 * Implementation of this interface can have builder-style methods themselves
	 * and be made accessible through static factory methods.
	 *
	 * @param postProcessor a post-processor to add
	 */
	public MockHttpServletRequestBuilder with(RequestPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "postProcessor is required");
		this.postProcessors.add(postProcessor);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @return always returns {@code true}.
	 */
	public boolean isMergeEnabled() {
		return true;
	}

	/**
	 * Merges the properties of the "parent" RequestBuilder accepting values
	 * only if not already set in "this" instance.
	 *
	 * @param parent the parent {@code RequestBuilder} to inherit properties from
	 * @return the result of the merge
	 */
	public Object merge(Object parent) {
		if (parent == null) {
			return this;
		}
		if (!(parent instanceof MockHttpServletRequestBuilder)) {
			throw new IllegalArgumentException("Cannot merge with [" + parent.getClass().getName() + "]");
		}

		MockHttpServletRequestBuilder parentBuilder = (MockHttpServletRequestBuilder) parent;

		for (String headerName : parentBuilder.headers.keySet()) {
			if (!this.headers.containsKey(headerName)) {
				this.headers.put(headerName, parentBuilder.headers.get(headerName));
			}
		}

		if (this.contentType == null) {
			this.contentType = parentBuilder.contentType;
		}

		if (this.content == null) {
			this.content = parentBuilder.content;
		}

		for (String paramName : parentBuilder.parameters.keySet()) {
			if (!this.parameters.containsKey(paramName)) {
				this.parameters.put(paramName, parentBuilder.parameters.get(paramName));
			}
		}

		for (Cookie cookie : parentBuilder.cookies) {
			if (!containsCookie(cookie)) {
				this.cookies.add(cookie);
			}
		}

		if (this.locale == null) {
			this.locale = parentBuilder.locale;
		}

		if (this.characterEncoding == null) {
			this.characterEncoding = parentBuilder.characterEncoding;
		}

		if (this.principal == null) {
			this.principal = parentBuilder.principal;
		}

		if (this.secure == null) {
			this.secure = parentBuilder.secure;
		}

		for (String attributeName : parentBuilder.attributes.keySet()) {
			if (!this.attributes.containsKey(attributeName)) {
				this.attributes.put(attributeName, parentBuilder.attributes.get(attributeName));
			}
		}

		if (this.session == null) {
			this.session = parentBuilder.session;
		}

		for (String sessionAttributeName : parentBuilder.sessionAttributes.keySet()) {
			if (!this.sessionAttributes.containsKey(sessionAttributeName)) {
				this.sessionAttributes.put(sessionAttributeName, parentBuilder.sessionAttributes.get(sessionAttributeName));
			}
		}

		for (String flashAttributeName : parentBuilder.flashAttributes.keySet()) {
			if (!this.flashAttributes.containsKey(flashAttributeName)) {
				this.flashAttributes.put(flashAttributeName, parentBuilder.flashAttributes.get(flashAttributeName));
			}
		}

		if (!StringUtils.hasText(this.contextPath)) {
			this.contextPath = parentBuilder.contextPath;
		}

		if (!StringUtils.hasText(this.servletPath)) {
			this.servletPath = parentBuilder.servletPath;
		}

		if (ValueConstants.DEFAULT_NONE.equals(this.pathInfo)) {
			this.pathInfo = parentBuilder.pathInfo;
		}

		this.postProcessors.addAll(parentBuilder.postProcessors);

		return this;
	}

	private boolean containsCookie(Cookie cookie) {
		for (Cookie c : this.cookies) {
			if (ObjectUtils.nullSafeEquals(c.getName(), cookie.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Build a {@link MockHttpServletRequest}.
	 */
	public final MockHttpServletRequest buildRequest(ServletContext servletContext) {

		MockHttpServletRequest request = createServletRequest(servletContext);

		String requestUri = this.uriComponents.getPath();
		request.setRequestURI(requestUri);

		updatePathRequestProperties(request, requestUri);

		if (this.uriComponents.getScheme() != null) {
			request.setScheme(this.uriComponents.getScheme());
		}
		if (this.uriComponents.getHost() != null) {
			request.setServerName(uriComponents.getHost());
		}
		if (this.uriComponents.getPort() != -1) {
			request.setServerPort(this.uriComponents.getPort());
		}

		request.setMethod(this.method.name());

		for (String name : this.headers.keySet()) {
			for (Object value : this.headers.get(name)) {
				request.addHeader(name, value);
			}
		}

		try {
			if (this.uriComponents.getQuery() != null) {
				String query = UriUtils.decode(this.uriComponents.getQuery(), "UTF-8");
				request.setQueryString(query);
			}

			for (Entry<String, List<String>> entry : this.uriComponents.getQueryParams().entrySet()) {
				for (String value : entry.getValue()) {
					request.addParameter(
							UriUtils.decode(entry.getKey(), "UTF-8"),
							UriUtils.decode(value, "UTF-8"));
				}
			}
		}
		catch (UnsupportedEncodingException ex) {
			// shouldn't happen
		}

		for (String name : this.parameters.keySet()) {
			for (String value : this.parameters.get(name)) {
				request.addParameter(name, value);
			}
		}

		request.setContentType(this.contentType);
		request.setContent(this.content);

		request.setCookies(this.cookies.toArray(new Cookie[this.cookies.size()]));

		if (this.locale != null) {
			request.addPreferredLocale(this.locale);
		}

		request.setCharacterEncoding(this.characterEncoding);

		request.setUserPrincipal(this.principal);

		if (this.secure != null) {
			request.setSecure(this.secure);
		}

		for (String name : this.attributes.keySet()) {
			request.setAttribute(name, this.attributes.get(name));
		}

		// Set session before session and flash attributes

		if (this.session != null) {
			request.setSession(this.session);
		}

		for (String name : this.sessionAttributes.keySet()) {
			request.getSession().setAttribute(name, this.sessionAttributes.get(name));
		}

		FlashMap flashMap = new FlashMap();
		flashMap.putAll(this.flashAttributes);

		FlashMapManager flashMapManager = getFlashMapManager(request);
		flashMapManager.saveOutputFlashMap(flashMap, request, new MockHttpServletResponse());

		// Apply post-processors at the very end

		for (RequestPostProcessor postProcessor : this.postProcessors) {
			request = postProcessor.postProcessRequest(request);
			Assert.notNull(request, "Post-processor [" + postProcessor.getClass().getName() + "] returned null");
		}

		return request;
	}

	/**
	 * Creates a new {@link MockHttpServletRequest} based on the given
	 * {@link ServletContext}. Can be overridden in sub-classes.
	 */
	protected MockHttpServletRequest createServletRequest(ServletContext servletContext) {
		return ClassUtils.hasMethod(ServletRequest.class, "startAsync") ?
				createServlet3Request(servletContext) : new MockHttpServletRequest(servletContext);
	}

	private MockHttpServletRequest createServlet3Request(ServletContext servletContext) {
		try {
			String className = "org.springframework.test.web.mock.servlet.request.Servlet3MockHttpServletRequest";
			Class<?> clazz = ClassUtils.forName(className, MockHttpServletRequestBuilder.class.getClassLoader());
			Constructor<?> constructor = clazz.getConstructor(ServletContext.class);
			return (MockHttpServletRequest) BeanUtils.instantiateClass(constructor, servletContext);
		}
		catch (Throwable t) {
			throw new IllegalStateException("Failed to instantiate MockHttpServletRequest", t);
		}
	}

	/**
	 * Update the contextPath, servletPath, and pathInfo of the request.
	 */
	private void updatePathRequestProperties(MockHttpServletRequest request, String requestUri) {

		Assert.isTrue(requestUri.startsWith(this.contextPath),
				"requestURI [" + requestUri + "] does not start with contextPath [" + this.contextPath + "]");

		request.setContextPath(this.contextPath);
		request.setServletPath(this.servletPath);

		if (ValueConstants.DEFAULT_NONE.equals(this.pathInfo)) {

			Assert.isTrue(requestUri.startsWith(this.contextPath + this.servletPath),
					"Invalid servletPath [" + this.servletPath + "] for requestURI [" + requestUri + "]");

			String extraPath = requestUri.substring(this.contextPath.length() + this.servletPath.length());
			this.pathInfo = (StringUtils.hasText(extraPath)) ? extraPath : null;
		}

		request.setPathInfo(this.pathInfo);
	}

	private FlashMapManager getFlashMapManager(MockHttpServletRequest request) {
		FlashMapManager flashMapManager = null;
		try {
			ServletContext servletContext = request.getServletContext();
			WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
			flashMapManager = wac.getBean(DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
		}
		catch (IllegalStateException ex) {
		}
		catch (NoSuchBeanDefinitionException ex) {
		}
		return (flashMapManager != null) ? flashMapManager : new SessionFlashMapManager();
	}

	private static <T> void addToMultiValueMap(MultiValueMap<String, T> map, String name, T[] values) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(values, "'values' is required");
		Assert.notEmpty(values, "'values' must not be empty");
		for (T value : values) {
			map.add(name, value);
		}
	}

	private static void addAttributeToMap(Map<String, Object> map, String name, Object value) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(value, "'value' must not be null");
		map.put(name, value);
	}

}
