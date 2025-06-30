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

package org.springframework.http.server.reactive;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Common base class for {@link ServerHttpRequest} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractServerHttpRequest implements ServerHttpRequest {

	private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");


	private final URI uri;

	private final @Nullable String contextPath;

	private @Nullable RequestPath path;

	private final HttpHeaders headers;

	private final HttpMethod method;

	private @Nullable MultiValueMap<String, String> queryParams;

	private @Nullable MultiValueMap<String, HttpCookie> cookies;

	private @Nullable SslInfo sslInfo;

	private @Nullable String id;

	private @Nullable String logPrefix;

	private @Nullable Supplier<Map<String, Object>> attributesSupplier;


	/**
	 * Constructor with the method, URI and headers for the request.
	 * @param method the HTTP method for the request
	 * @param uri the URI for the request
	 * @param contextPath the context path for the request
	 * @param headers the headers for the request (as {@link MultiValueMap})
	 * @since 7.0
	 */
	public AbstractServerHttpRequest(HttpMethod method, URI uri, @Nullable String contextPath,
			HttpHeaders headers) {

		Assert.notNull(method, "Method must not be null");
		Assert.notNull(uri, "Uri must not be null");
		Assert.notNull(headers, "Headers must not be null");

		this.method = method;
		this.uri = uri;
		this.contextPath = contextPath;
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
	}


	@Override
	public String getId() {
		if (this.id == null) {
			this.id = initId();
			if (this.id == null) {
				this.id = ObjectUtils.getIdentityHexString(this);
			}
		}
		return this.id;
	}

	/**
	 * Obtain the request id to use, or {@code null} in which case the Object
	 * identity of this request instance is used.
	 * @since 5.1
	 */
	protected @Nullable String initId() {
		return null;
	}

	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public Map<String, Object> getAttributes() {
		if (this.attributesSupplier != null) {
			return this.attributesSupplier.get();
		}
		else {
			return Collections.emptyMap();
		}
	}

	@Override
	public RequestPath getPath() {
		if (this.path == null) {
			this.path = RequestPath.parse(this.uri, this.contextPath);
		}
		return this.path;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		if (this.queryParams == null) {
			this.queryParams = CollectionUtils.unmodifiableMultiValueMap(initQueryParams());
		}
		return this.queryParams;
	}

	/**
	 * A method for parsing of the query into name-value pairs. The return
	 * value is turned into an immutable map and cached.
	 * <p>Note that this method is invoked lazily on first access to
	 * {@link #getQueryParams()}. The invocation is not synchronized but the
	 * parsing is thread-safe nevertheless.
	 */
	protected MultiValueMap<String, String> initQueryParams() {
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		String query = getURI().getRawQuery();
		if (query != null) {
			Matcher matcher = QUERY_PATTERN.matcher(query);
			while (matcher.find()) {
				String name = decodeQueryParam(matcher.group(1));
				String eq = matcher.group(2);
				String value = matcher.group(3);
				value = (value != null ? decodeQueryParam(value) : (StringUtils.hasLength(eq) ? "" : null));
				queryParams.add(name, value);
			}
		}
		return queryParams;
	}

	private String decodeQueryParam(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		if (this.cookies == null) {
			this.cookies = CollectionUtils.unmodifiableMultiValueMap(initCookies());
		}
		return this.cookies;
	}

	/**
	 * Obtain the cookies from the underlying "native" request and adapt those to
	 * an {@link HttpCookie} map. The return value is turned into an immutable
	 * map and cached.
	 * <p>Note that this method is invoked lazily on access to
	 * {@link #getCookies()}. Subclasses should synchronize cookie
	 * initialization if the underlying "native" request does not provide
	 * thread-safe access to cookie data.
	 */
	protected abstract MultiValueMap<String, HttpCookie> initCookies();

	@Override
	public @Nullable SslInfo getSslInfo() {
		if (this.sslInfo == null) {
			this.sslInfo = initSslInfo();
		}
		return this.sslInfo;
	}

	/**
	 * Obtain SSL session information from the underlying "native" request.
	 * @return the session information, or {@code null} if none available
	 * @since 5.0.2
	 */
	protected abstract @Nullable SslInfo initSslInfo();

	/**
	 * Return the underlying server response.
	 * <p><strong>Note:</strong> This is exposed mainly for internal framework
	 * use such as WebSocket upgrades in the spring-webflux module.
	 */
	public abstract <T> T getNativeRequest();

	/**
	 * For internal use in logging at the HTTP adapter layer.
	 * @since 5.1
	 */
	String getLogPrefix() {
		if (this.logPrefix == null) {
			this.logPrefix = "[" + initLogPrefix() + "] ";
		}
		return this.logPrefix;
	}

	/**
	 * Subclasses can override this to provide the prefix to use for log messages.
	 * <p>By default, this is {@link #getId()}.
	 * @since 5.3.15
	 */
	protected String initLogPrefix() {
		return getId();
	}

	/**
	 * Set the attribute supplier.
	 * <p><strong>Note:</strong> This is exposed mainly for internal framework
	 * use.
	 */
	public void setAttributesSupplier(Supplier<Map<String, Object>> attributesSupplier) {
		this.attributesSupplier = attributesSupplier;
	}
}
