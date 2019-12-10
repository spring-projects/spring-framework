/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
		super(HttpStatus.METHOD_NOT_ALLOWED, "Request method '" + method + "' not supported");
		Assert.notNull(method, "'method' is required");
		if (supportedMethods == null) {
			supportedMethods = Collections.emptySet();
		}
		this.method = method;
		this.httpMethods = Collections.unmodifiableSet(new HashSet<>(supportedMethods));
	}


	/**
	 * Return a Map with an "Allow" header.
	 * @since 5.1.11
	 */
	@Override
	public Map<String, String> getHeaders() {
		return !CollectionUtils.isEmpty(this.httpMethods) ?
				Collections.singletonMap("Allow", StringUtils.collectionToDelimitedString(this.httpMethods, ", ")) :
				Collections.emptyMap();
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
