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

package org.springframework.http.server.reactive;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

	private final HttpHeaders headers;

	private MultiValueMap<String, String> queryParams;

	private MultiValueMap<String, HttpCookie> cookies;


	/**
	 * Constructor with the URI and headers for the request.
	 * @param uri the URI for the request
	 * @param headers the headers for the request
	 */
	public AbstractServerHttpRequest(URI uri, HttpHeaders headers) {
		this.uri = uri;
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
	}


	@Override
	public URI getURI() {
		return this.uri;
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
	 *
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
				String name = matcher.group(1);
				String eq = matcher.group(2);
				String value = matcher.group(3);
				value = (value != null ? value : (StringUtils.hasLength(eq) ? "" : null));
				queryParams.add(name, value);
			}
		}
		return queryParams;
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
	 * {@link #getCookies()}. Sub-classes should synchronize cookie
	 * initialization if the underlying "native" request does not provide
	 * thread-safe access to cookie data.
	 */
	protected abstract MultiValueMap<String, HttpCookie> initCookies();

}
