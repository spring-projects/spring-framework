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
package org.springframework.http.server.reactive;

import java.net.URI;
import java.net.URISyntaxException;
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


	private URI uri;

	private MultiValueMap<String, String> queryParams;

	private HttpHeaders headers;

	private MultiValueMap<String, HttpCookie> cookies;


	@Override
	public URI getURI() {
		if (this.uri == null) {
			try {
				this.uri = initUri();
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException("Could not get URI: " + ex.getMessage(), ex);
			}
		}
		return this.uri;
	}

	/**
	 * Initialize a URI that represents the request. Invoked lazily on the first
	 * call to {@link #getURI()} and then cached.
	 * @throws URISyntaxException
	 */
	protected abstract URI initUri() throws URISyntaxException;

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		if (this.queryParams == null) {
			this.queryParams = CollectionUtils.unmodifiableMultiValueMap(initQueryParams());
		}
		return this.queryParams;
	}

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
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = HttpHeaders.readOnlyHttpHeaders(initHeaders());
		}
		return this.headers;
	}

	/**
	 * Initialize the headers from the underlying request. Invoked lazily on the
	 * first call to {@link #getHeaders()} and then cached.
	 */
	protected abstract HttpHeaders initHeaders();

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		if (this.cookies == null) {
			this.cookies = CollectionUtils.unmodifiableMultiValueMap(initCookies());
		}
		return this.cookies;
	}

	/**
	 * Initialize the cookies from the underlying request. Invoked lazily on the
	 * first access to cookies via {@link #getHeaders()} and then cached.
	 */
	protected abstract MultiValueMap<String, HttpCookie> initCookies();

}
