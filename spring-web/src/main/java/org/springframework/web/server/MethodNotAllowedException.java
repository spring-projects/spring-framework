/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.server;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Exception for errors that fit response status 405 (method not allowed).
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class MethodNotAllowedException extends ResponseStatusException {

	private final String method;

	private final Set<HttpMethod> httpMethods;


	public MethodNotAllowedException(HttpMethod method, Collection<HttpMethod> supportedMethods) {
		this(method.name(), supportedMethods);
	}

	public MethodNotAllowedException(String method, @Nullable Collection<HttpMethod> supportedMethods) {
		super(HttpStatus.METHOD_NOT_ALLOWED, "Request method '" + method + "' is not supported.",
				null, null, new Object[] {method, supportedMethods});

		Assert.notNull(method, "'method' is required");
		if (supportedMethods == null) {
			supportedMethods = Collections.emptySet();
		}
		this.method = method;
		this.httpMethods = Collections.unmodifiableSet(new LinkedHashSet<>(supportedMethods));
		if (!this.httpMethods.isEmpty()) {
			setDetail("Supported methods: " + this.httpMethods);
		}
	}


	/**
	 * Return HttpHeaders with an "Allow" header that documents the allowed
	 * HTTP methods for this URL, if available, or an empty instance otherwise.
	 */
	@Override
	public HttpHeaders getHeaders() {
		if (CollectionUtils.isEmpty(this.httpMethods)) {
			return HttpHeaders.EMPTY;
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setAllow(this.httpMethods);
		return headers;
	}

	/**
	 * Delegates to {@link #getHeaders()}.
	 * @since 5.1.13
	 * @deprecated as of 6.0 in favor of {@link #getHeaders()}
	 */
	@Deprecated(since = "6.0")
	@Override
	public HttpHeaders getResponseHeaders() {
		return getHeaders();
	}

	/**
	 * Return the HTTP method for the failed request.
	 */
	public String getHttpMethod() {
		return this.method;
	}

	/**
	 * Return the list of supported HTTP methods.
	 */
	public Set<HttpMethod> getSupportedMethods() {
		return this.httpMethods;
	}

}
