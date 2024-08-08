/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.servlet.request;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.Mergeable;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * Base builder for {@link MockHttpServletRequest} required as input to
 * perform requests in {@link MockMvc}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Kamill Sokol
 * @since 6.2
 * @param <B> a self reference to the builder type
 */
public abstract class AbstractMockHttpServletRequestBuilder<B extends AbstractMockHttpServletRequestBuilder<B>>
		implements ConfigurableSmartRequestBuilder<B>, Mergeable {

	private final HttpMethod method;

	@Nullable
	private URI uri;

	private String contextPath = "";

	private String servletPath = "";

	@Nullable
	private String pathInfo = "";

	@Nullable
	private Boolean secure;

	@Nullable
	private Principal principal;

	@Nullable
	private MockHttpSession session;

	@Nullable
	private String remoteAddress;

	@Nullable
	private String characterEncoding;

	@Nullable
	private byte[] content;

	@Nullable
	private String contentType;

	private final MultiValueMap<String, Object> headers = new LinkedMultiValueMap<>();

	private final MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

	private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

	private final MultiValueMap<String, String> formFields = new LinkedMultiValueMap<>();

	private final List<Cookie> cookies = new ArrayList<>();

	private final List<Locale> locales = new ArrayList<>();

	private final Map<String, Object> requestAttributes = new LinkedHashMap<>();

	private final Map<String, Object> sessionAttributes = new LinkedHashMap<>();

	private final Map<String, Object> flashAttributes = new LinkedHashMap<>();

	private final List<RequestPostProcessor> postProcessors = new ArrayList<>();


	/**
	 * Create a new instance using the specified {@link HttpMethod}.
	 * @param httpMethod the HTTP method (GET, POST, etc.)
	 */
	protected AbstractMockHttpServletRequestBuilder(HttpMethod httpMethod) {
		Assert.notNull(httpMethod, "'httpMethod' is required");
		this.method = httpMethod;
	}

	@SuppressWarnings("unchecked")
	protected B self() {
		return (B) this;
	}

	/**
	 * Specify the URI using an absolute, fully constructed {@link java.net.URI}.
	 */
	public B uri(URI uri) {
		this.uri = uri;
		return self();
	}

	/**
	 * Specify the URI for the request using a URI template and URI variables.
	 */
	public B uri(String uriTemplate, Object... uriVariables) {
		return uri(initUri(uriTemplate, uriVariables));
	}

	private static URI initUri(String uri, Object[] vars) {
		Assert.notNull(uri, "'uri' must not be null");
		Assert.isTrue(uri.isEmpty() || uri.startsWith("/") || uri.startsWith("http://") || uri.startsWith("https://"),
				() -> "'uri' should start with a path or be a complete HTTP URI: " + uri);
		String uriString = (uri.isEmpty() ? "/" : uri);
		return UriComponentsBuilder.fromUriString(uriString).buildAndExpand(vars).encode().toUri();
	}

	/**
	 * Specify the portion of the requestURI that represents the context path.
	 * The context path, if specified, must match to the start of the request URI.
	 * <p>In most cases, tests can be written by omitting the context path from
	 * the requestURI. This is because most applications don't actually depend
	 * on the name under which they're deployed. If specified here, the context
	 * path must start with a "/" and must not end with a "/".
	 * @see jakarta.servlet.http.HttpServletRequest#getContextPath()
	 */
	public B contextPath(String contextPath) {
		if (StringUtils.hasText(contextPath)) {
			Assert.isTrue(contextPath.startsWith("/"), "Context path must start with a '/'");
			Assert.isTrue(!contextPath.endsWith("/"), "Context path must not end with a '/'");
		}
		this.contextPath = contextPath;
		return self();
	}

	/**
	 * Specify the portion of the requestURI that represents the path to which
	 * the Servlet is mapped. This is typically a portion of the requestURI
	 * after the context path.
	 * <p>In most cases, tests can be written by omitting the servlet path from
	 * the requestURI. This is because most applications don't actually depend
	 * on the prefix to which a servlet is mapped. For example if a Servlet is
	 * mapped to {@code "/main/*"}, tests can be written with the requestURI
	 * {@code "/accounts/1"} as opposed to {@code "/main/accounts/1"}.
	 * If specified here, the servletPath must start with a "/" and must not
	 * end with a "/".
	 * @see jakarta.servlet.http.HttpServletRequest#getServletPath()
	 */
	public B servletPath(String servletPath) {
		if (StringUtils.hasText(servletPath)) {
			Assert.isTrue(servletPath.startsWith("/"), "Servlet path must start with a '/'");
			Assert.isTrue(!servletPath.endsWith("/"), "Servlet path must not end with a '/'");
		}
		this.servletPath = servletPath;
		return self();
	}

	/**
	 * Specify the portion of the requestURI that represents the pathInfo.
	 * <p>If left unspecified (recommended), the pathInfo will be automatically derived
	 * by removing the contextPath and the servletPath from the requestURI and using any
	 * remaining part. If specified here, the pathInfo must start with a "/".
	 * <p>If specified, the pathInfo will be used as-is.
	 * @see jakarta.servlet.http.HttpServletRequest#getPathInfo()
	 */
	public B pathInfo(@Nullable String pathInfo) {
		if (StringUtils.hasText(pathInfo)) {
			Assert.isTrue(pathInfo.startsWith("/"), "Path info must start with a '/'");
		}
		this.pathInfo = pathInfo;
		return self();
	}

	/**
	 * Set the secure property of the {@link ServletRequest} indicating use of a
	 * secure channel, such as HTTPS.
	 * @param secure whether the request is using a secure channel
	 */
	public B secure(boolean secure){
		this.secure = secure;
		return self();
	}

	/**
	 * Set the character encoding of the request.
	 * @param encoding the character encoding
	 * @since 5.3.10
	 * @see StandardCharsets
	 * @see #characterEncoding(String)
	 */
	public B characterEncoding(Charset encoding) {
		return characterEncoding(encoding.name());
	}

	/**
	 * Set the character encoding of the request.
	 * @param encoding the character encoding
	 */
	public B characterEncoding(String encoding) {
		this.characterEncoding = encoding;
		return self();
	}

	/**
	 * Set the request body.
	 * <p>If content is provided and {@link #contentType(MediaType)} is set to
	 * {@code application/x-www-form-urlencoded}, the content will be parsed
	 * and used to populate the {@link #param(String, String...) request
	 * parameters} map.
	 * @param content the body content
	 */
	public B content(byte[] content) {
		this.content = content;
		return self();
	}

	/**
	 * Set the request body as a UTF-8 String.
	 * <p>If content is provided and {@link #contentType(MediaType)} is set to
	 * {@code application/x-www-form-urlencoded}, the content will be parsed
	 * and used to populate the {@link #param(String, String...) request
	 * parameters} map.
	 * @param content the body content
	 */
	public B content(String content) {
		this.content = content.getBytes(StandardCharsets.UTF_8);
		return self();
	}

	/**
	 * Set the 'Content-Type' header of the request.
	 * <p>If content is provided and {@code contentType} is set to
	 * {@code application/x-www-form-urlencoded}, the content will be parsed
	 * and used to populate the {@link #param(String, String...) request
	 * parameters} map.
	 * @param contentType the content type
	 */
	public B contentType(MediaType contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType.toString();
		return self();
	}

	/**
	 * Set the 'Content-Type' header of the request as a raw String value,
	 * possibly not even well-formed (for testing purposes).
	 * @param contentType the content type
	 * @since 4.1.2
	 */
	public B contentType(String contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType;
		return self();
	}

	/**
	 * Set the 'Accept' header to the given media type(s).
	 * @param mediaTypes one or more media types
	 */
	public B accept(MediaType... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		this.headers.set("Accept", MediaType.toString(Arrays.asList(mediaTypes)));
		return self();
	}

	/**
	 * Set the {@code Accept} header using raw String values, possibly not even
	 * well-formed (for testing purposes).
	 * @param mediaTypes one or more media types; internally joined as
	 * comma-separated String
	 */
	public B accept(String... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		this.headers.set("Accept", String.join(", ", mediaTypes));
		return self();
	}

	/**
	 * Add a header to the request. Values are always added.
	 * @param name the header name
	 * @param values one or more header values
	 */
	public B header(String name, Object... values) {
		addToMultiValueMap(this.headers, name, values);
		return self();
	}

	/**
	 * Add all headers to the request. Values are always added.
	 * @param httpHeaders the headers and values to add
	 */
	public B headers(HttpHeaders httpHeaders) {
		httpHeaders.forEach(this.headers::addAll);
		return self();
	}

	/**
	 * Add a request parameter to {@link MockHttpServletRequest#getParameterMap()}.
	 * <p>In the Servlet API, a request parameter may be parsed from the query
	 * string and/or from the body of an {@code application/x-www-form-urlencoded}
	 * request. This method simply adds to the request parameter map. You may
	 * also use add Servlet request parameters by specifying the query or form
	 * data through one of the following:
	 * <ul>
	 * <li>Supply a URL with a query to {@link MockMvcRequestBuilders}.
	 * <li>Add query params via {@link #queryParam} or {@link #queryParams}.
	 * <li>Provide {@link #content} with {@link #contentType}
	 * {@code application/x-www-form-urlencoded}.
	 * </ul>
	 * @param name the parameter name
	 * @param values one or more values
	 */
	public B param(String name, String... values) {
		addToMultiValueMap(this.parameters, name, values);
		return self();
	}

	/**
	 * Variant of {@link #param(String, String...)} with a {@link MultiValueMap}.
	 * @param params the parameters to add
	 * @since 4.2.4
	 */
	public B params(MultiValueMap<String, String> params) {
		params.forEach((name, values) -> {
			for (String value : values) {
				this.parameters.add(name, value);
			}
		});
		return self();
	}

	/**
	 * Append to the query string and also add to the
	 * {@link #param(String, String...) request parameters} map. The parameter
	 * name and value are encoded when they are added to the query string.
	 * @param name the parameter name
	 * @param values one or more values
	 * @since 5.2.2
	 */
	public B queryParam(String name, String... values) {
		param(name, values);
		this.queryParams.addAll(name, Arrays.asList(values));
		return self();
	}

	/**
	 * Append to the query string and also add to the
	 * {@link #params(MultiValueMap) request parameters} map. The parameter
	 * name and value are encoded when they are added to the query string.
	 * @param params the parameters to add
	 * @since 5.2.2
	 */
	public B queryParams(MultiValueMap<String, String> params) {
		params(params);
		this.queryParams.addAll(params);
		return self();
	}

	/**
	 * Append the given value(s) to the given form field and also add them to the
	 * {@linkplain #param(String, String...) request parameters} map.
	 * @param name the field name
	 * @param values one or more values
	 * @since 6.1.7
	 */
	public B formField(String name, String... values) {
		param(name, values);
		this.formFields.addAll(name, Arrays.asList(values));
		return self();
	}

	/**
	 * Variant of {@link #formField(String, String...)} with a {@link MultiValueMap}.
	 * @param formFields the form fields to add
	 * @since 6.1.7
	 */
	public B formFields(MultiValueMap<String, String> formFields) {
		params(formFields);
		this.formFields.addAll(formFields);
		return self();
	}

	/**
	 * Add the given cookies to the request. Cookies are always added.
	 * @param cookies the cookies to add
	 */
	public B cookie(Cookie... cookies) {
		Assert.notEmpty(cookies, "'cookies' must not be empty");
		this.cookies.addAll(Arrays.asList(cookies));
		return self();
	}

	/**
	 * Add the specified locales as preferred request locales.
	 * @param locales the locales to add
	 * @since 4.3.6
	 * @see #locale(Locale)
	 */
	public B locale(Locale... locales) {
		Assert.notEmpty(locales, "'locales' must not be empty");
		this.locales.addAll(Arrays.asList(locales));
		return self();
	}

	/**
	 * Set the locale of the request, overriding any previous locales.
	 * @param locale the locale, or {@code null} to reset it
	 * @see #locale(Locale...)
	 */
	public B locale(@Nullable Locale locale) {
		this.locales.clear();
		if (locale != null) {
			this.locales.add(locale);
		}
		return self();
	}

	/**
	 * Set a request attribute.
	 * @param name the attribute name
	 * @param value the attribute value
	 */
	public B requestAttr(String name, Object value) {
		addToMap(this.requestAttributes, name, value);
		return self();
	}

	/**
	 * Set a session attribute.
	 * @param name the session attribute name
	 * @param value the session attribute value
	 */
	public B sessionAttr(String name, Object value) {
		addToMap(this.sessionAttributes, name, value);
		return self();
	}

	/**
	 * Set session attributes.
	 * @param sessionAttributes the session attributes
	 */
	public B sessionAttrs(Map<String, Object> sessionAttributes) {
		Assert.notEmpty(sessionAttributes, "'sessionAttributes' must not be empty");
		sessionAttributes.forEach(this::sessionAttr);
		return self();
	}

	/**
	 * Set an "input" flash attribute.
	 * @param name the flash attribute name
	 * @param value the flash attribute value
	 */
	public B flashAttr(String name, Object value) {
		addToMap(this.flashAttributes, name, value);
		return self();
	}

	/**
	 * Set flash attributes.
	 * @param flashAttributes the flash attributes
	 */
	public B flashAttrs(Map<String, Object> flashAttributes) {
		Assert.notEmpty(flashAttributes, "'flashAttributes' must not be empty");
		flashAttributes.forEach(this::flashAttr);
		return self();
	}

	/**
	 * Set the HTTP session to use, possibly re-used across requests.
	 * <p>Individual attributes provided via {@link #sessionAttr(String, Object)}
	 * override the content of the session provided here.
	 * @param session the HTTP session
	 */
	public B session(MockHttpSession session) {
		Assert.notNull(session, "'session' must not be null");
		this.session = session;
		return self();
	}

	/**
	 * Set the principal of the request.
	 * @param principal the principal
	 */
	public B principal(Principal principal) {
		Assert.notNull(principal, "'principal' must not be null");
		this.principal = principal;
		return self();
	}

	/**
	 * Set the remote address of the request.
	 * @param remoteAddress the remote address (IP)
	 * @since 6.0.10
	 */
	public B remoteAddress(String remoteAddress) {
		Assert.hasText(remoteAddress, "'remoteAddress' must not be null or blank");
		this.remoteAddress = remoteAddress;
		return self();
	}

	/**
	 * An extension point for further initialization of {@link MockHttpServletRequest}
	 * in ways not built directly into the {@code MockHttpServletRequestBuilder}.
	 * Implementation of this interface can have builder-style methods themselves
	 * and be made accessible through static factory methods.
	 * @param postProcessor a post-processor to add
	 */
	@Override
	public B with(RequestPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "postProcessor is required");
		this.postProcessors.add(postProcessor);
		return self();
	}


	/**
	 * {@inheritDoc}
	 * @return always returns {@code true}.
	 */
	@Override
	public boolean isMergeEnabled() {
		return true;
	}

	/**
	 * Merges the properties of the "parent" RequestBuilder accepting values
	 * only if not already set in "this" instance.
	 * @param parent the parent {@code RequestBuilder} to inherit properties from
	 * @return the result of the merge
	 */
	@Override
	public Object merge(@Nullable Object parent) {
		if (parent == null) {
			return this;
		}
		if (!(parent instanceof AbstractMockHttpServletRequestBuilder<?> parentBuilder)) {
			throw new IllegalArgumentException("Cannot merge with [" + parent.getClass().getName() + "]");
		}
		if (this.uri == null) {
			this.uri = parentBuilder.uri;
		}
		if (!StringUtils.hasText(this.contextPath)) {
			this.contextPath = parentBuilder.contextPath;
		}
		if (!StringUtils.hasText(this.servletPath)) {
			this.servletPath = parentBuilder.servletPath;
		}
		if ("".equals(this.pathInfo)) {
			this.pathInfo = parentBuilder.pathInfo;
		}

		if (this.secure == null) {
			this.secure = parentBuilder.secure;
		}
		if (this.principal == null) {
			this.principal = parentBuilder.principal;
		}
		if (this.session == null) {
			this.session = parentBuilder.session;
		}
		if (this.remoteAddress == null) {
			this.remoteAddress = parentBuilder.remoteAddress;
		}

		if (this.characterEncoding == null) {
			this.characterEncoding = parentBuilder.characterEncoding;
		}
		if (this.content == null) {
			this.content = parentBuilder.content;
		}
		if (this.contentType == null) {
			this.contentType = parentBuilder.contentType;
		}

		for (Map.Entry<String, List<Object>> entry : parentBuilder.headers.entrySet()) {
			String headerName = entry.getKey();
			if (!this.headers.containsKey(headerName)) {
				this.headers.put(headerName, entry.getValue());
			}
		}
		for (Map.Entry<String, List<String>> entry : parentBuilder.parameters.entrySet()) {
			String paramName = entry.getKey();
			if (!this.parameters.containsKey(paramName)) {
				this.parameters.put(paramName, entry.getValue());
			}
		}
		for (Map.Entry<String, List<String>> entry : parentBuilder.queryParams.entrySet()) {
			String paramName = entry.getKey();
			if (!this.queryParams.containsKey(paramName)) {
				this.queryParams.put(paramName, entry.getValue());
			}
		}
		for (Map.Entry<String, List<String>> entry : parentBuilder.formFields.entrySet()) {
			String paramName = entry.getKey();
			if (!this.formFields.containsKey(paramName)) {
				this.formFields.put(paramName, entry.getValue());
			}
		}
		for (Cookie cookie : parentBuilder.cookies) {
			if (!containsCookie(cookie)) {
				this.cookies.add(cookie);
			}
		}
		for (Locale locale : parentBuilder.locales) {
			if (!this.locales.contains(locale)) {
				this.locales.add(locale);
			}
		}

		for (Map.Entry<String, Object> entry : parentBuilder.requestAttributes.entrySet()) {
			String attributeName = entry.getKey();
			if (!this.requestAttributes.containsKey(attributeName)) {
				this.requestAttributes.put(attributeName, entry.getValue());
			}
		}
		for (Map.Entry<String, Object> entry : parentBuilder.sessionAttributes.entrySet()) {
			String attributeName = entry.getKey();
			if (!this.sessionAttributes.containsKey(attributeName)) {
				this.sessionAttributes.put(attributeName, entry.getValue());
			}
		}
		for (Map.Entry<String, Object> entry : parentBuilder.flashAttributes.entrySet()) {
			String attributeName = entry.getKey();
			if (!this.flashAttributes.containsKey(attributeName)) {
				this.flashAttributes.put(attributeName, entry.getValue());
			}
		}

		this.postProcessors.addAll(0, parentBuilder.postProcessors);

		return this;
	}

	private boolean containsCookie(Cookie cookie) {
		for (Cookie cookieToCheck : this.cookies) {
			if (ObjectUtils.nullSafeEquals(cookieToCheck.getName(), cookie.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Build a {@link MockHttpServletRequest}.
	 */
	@Override
	public final MockHttpServletRequest buildRequest(ServletContext servletContext) {
		Assert.notNull(this.uri, "'uri' is required");
		MockHttpServletRequest request = createServletRequest(servletContext);

		request.setAsyncSupported(true);
		request.setMethod(this.method.name());

		String requestUri = this.uri.getRawPath();
		request.setRequestURI(requestUri);

		if (this.uri.getScheme() != null) {
			request.setScheme(this.uri.getScheme());
		}
		if (this.uri.getHost() != null) {
			request.setServerName(this.uri.getHost());
		}
		if (this.uri.getPort() != -1) {
			request.setServerPort(this.uri.getPort());
		}

		updatePathRequestProperties(request, requestUri);

		if (this.secure != null) {
			request.setSecure(this.secure);
		}
		if (this.principal != null) {
			request.setUserPrincipal(this.principal);
		}
		if (this.remoteAddress != null) {
			request.setRemoteAddr(this.remoteAddress);
		}
		if (this.session != null) {
			request.setSession(this.session);
		}

		request.setCharacterEncoding(this.characterEncoding);
		request.setContent(this.content);
		request.setContentType(this.contentType);

		this.headers.forEach((name, values) -> {
			for (Object value : values) {
				request.addHeader(name, value);
			}
		});

		if (!ObjectUtils.isEmpty(this.content) &&
				!this.headers.containsKey(HttpHeaders.CONTENT_LENGTH) &&
				!this.headers.containsKey(HttpHeaders.TRANSFER_ENCODING)) {

			request.addHeader(HttpHeaders.CONTENT_LENGTH, this.content.length);
		}

		String query = this.uri.getRawQuery();
		if (!this.queryParams.isEmpty()) {
			String str = UriComponentsBuilder.newInstance().queryParams(this.queryParams).build().encode().getQuery();
			query = StringUtils.hasLength(query) ? (query + "&" + str) : str;
		}
		if (query != null) {
			request.setQueryString(query);
		}
		addRequestParams(request, UriComponentsBuilder.fromUri(this.uri).build().getQueryParams());

		this.parameters.forEach((name, values) -> {
			for (String value : values) {
				request.addParameter(name, value);
			}
		});

		if (!this.formFields.isEmpty()) {
			if (this.content != null && this.content.length > 0) {
				throw new IllegalStateException("Could not write form data with an existing body");
			}
			Charset charset = (this.characterEncoding != null ?
					Charset.forName(this.characterEncoding) : StandardCharsets.UTF_8);
			MediaType mediaType = (request.getContentType() != null ?
					MediaType.parseMediaType(request.getContentType()) :
					new MediaType(MediaType.APPLICATION_FORM_URLENCODED, charset));
			if (!mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
				throw new IllegalStateException("Invalid content type: '" + mediaType +
						"' is not compatible with '" + MediaType.APPLICATION_FORM_URLENCODED + "'");
			}
			request.setContent(writeFormData(mediaType, charset));
			if (request.getContentType() == null) {
				request.setContentType(mediaType.toString());
			}
		}
		if (this.content != null && this.content.length > 0) {
			String requestContentType = request.getContentType();
			if (requestContentType != null) {
				try {
					MediaType mediaType = MediaType.parseMediaType(requestContentType);
					if (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)) {
						addRequestParams(request, parseFormData(mediaType));
					}
				}
				catch (Exception ex) {
					// Must be invalid, ignore
				}
			}
		}

		if (!ObjectUtils.isEmpty(this.cookies)) {
			request.setCookies(this.cookies.toArray(new Cookie[0]));
		}
		if (!ObjectUtils.isEmpty(this.locales)) {
			request.setPreferredLocales(this.locales);
		}

		this.requestAttributes.forEach(request::setAttribute);
		this.sessionAttributes.forEach((name, attribute) -> {
			HttpSession session = request.getSession();
			Assert.state(session != null, "No HttpSession");
			session.setAttribute(name, attribute);
		});

		FlashMap flashMap = new FlashMap();
		flashMap.putAll(this.flashAttributes);
		FlashMapManager flashMapManager = getFlashMapManager(request);
		flashMapManager.saveOutputFlashMap(flashMap, request, new MockHttpServletResponse());

		return request;
	}

	/**
	 * Create a new {@link MockHttpServletRequest} based on the supplied
	 * {@code ServletContext}.
	 * <p>Can be overridden in subclasses.
	 */
	protected MockHttpServletRequest createServletRequest(ServletContext servletContext) {
		return new MockHttpServletRequest(servletContext);
	}

	/**
	 * Update the contextPath, servletPath, and pathInfo of the request.
	 */
	private void updatePathRequestProperties(MockHttpServletRequest request, String requestUri) {
		if (!requestUri.startsWith(this.contextPath)) {
			throw new IllegalArgumentException(
					"Request URI [" + requestUri + "] does not start with context path [" + this.contextPath + "]");
		}
		request.setContextPath(this.contextPath);
		request.setServletPath(this.servletPath);

		if ("".equals(this.pathInfo)) {
			if (!requestUri.startsWith(this.contextPath + this.servletPath)) {
				throw new IllegalArgumentException(
						"Invalid servlet path [" + this.servletPath + "] for request URI [" + requestUri + "]");
			}
			String extraPath = requestUri.substring(this.contextPath.length() + this.servletPath.length());
			this.pathInfo = (StringUtils.hasText(extraPath) ?
					UrlPathHelper.defaultInstance.decodeRequestString(request, extraPath) : null);
		}
		request.setPathInfo(this.pathInfo);
	}

	private void addRequestParams(MockHttpServletRequest request, MultiValueMap<String, String> map) {
		map.forEach((key, values) -> values.forEach(value -> {
			value = (value != null ? UriUtils.decode(value, StandardCharsets.UTF_8) : null);
			request.addParameter(UriUtils.decode(key, StandardCharsets.UTF_8), value);
		}));
	}

	private byte[] writeFormData(MediaType mediaType, Charset charset) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		HttpOutputMessage message = new HttpOutputMessage() {
			@Override
			public OutputStream getBody() {
				return out;
			}

			@Override
			public HttpHeaders getHeaders() {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(mediaType);
				return headers;
			}
		};
		try {
			FormHttpMessageConverter messageConverter = new FormHttpMessageConverter();
			messageConverter.setCharset(charset);
			messageConverter.write(this.formFields, mediaType, message);
			return out.toByteArray();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to write form data to request body", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private MultiValueMap<String, String> parseFormData(MediaType mediaType) {
		HttpInputMessage message = new HttpInputMessage() {
			@Override
			public InputStream getBody() {
				byte[] bodyContent = AbstractMockHttpServletRequestBuilder.this.content;
				return (bodyContent != null ? new ByteArrayInputStream(bodyContent) : InputStream.nullInputStream());
			}
			@Override
			public HttpHeaders getHeaders() {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(mediaType);
				return headers;
			}
		};

		try {
			return new FormHttpMessageConverter().read(null, message);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to parse form data in request body", ex);
		}
	}

	private FlashMapManager getFlashMapManager(MockHttpServletRequest request) {
		FlashMapManager flashMapManager = null;
		try {
			ServletContext servletContext = request.getServletContext();
			WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
			flashMapManager = wac.getBean(DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
		}
		catch (IllegalStateException | NoSuchBeanDefinitionException ex) {
			// ignore
		}
		return (flashMapManager != null ? flashMapManager : new SessionFlashMapManager());
	}

	@Override
	public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
		for (RequestPostProcessor postProcessor : this.postProcessors) {
			request = postProcessor.postProcessRequest(request);
		}
		return request;
	}


	private static void addToMap(Map<String, Object> map, String name, Object value) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(value, "'value' must not be null");
		map.put(name, value);
	}

	private static <T> void addToMultiValueMap(MultiValueMap<String, T> map, String name, T[] values) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notEmpty(values, "'values' must not be empty");
		for (T value : values) {
			map.add(name, value);
		}
	}

}
