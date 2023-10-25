/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.ServletException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Exception thrown when a request handler does not support a
 * specific request method.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0
 */
@SuppressWarnings("serial")
public class HttpRequestMethodNotSupportedException extends ServletException implements ErrorResponse {

	private final String method;

	@Nullable
	private final String[] supportedMethods;

	private final ProblemDetail body;


	/**
	 * Create a new {@code HttpRequestMethodNotSupportedException}.
	 * @param method the unsupported HTTP request method
	 */
	public HttpRequestMethodNotSupportedException(String method) {
		this(method, (String[]) null);
	}

	/**
	 * Create a new {@code HttpRequestMethodNotSupportedException}.
	 * @param method the unsupported HTTP request method
	 * @param supportedMethods the actually supported HTTP methods (possibly {@code null})
	 */
	public HttpRequestMethodNotSupportedException(String method, @Nullable Collection<String> supportedMethods) {
		this(method, (supportedMethods != null ? StringUtils.toStringArray(supportedMethods) : null));
	}

	/**
	 * Create a new {@code HttpRequestMethodNotSupportedException}.
	 * @param method the unsupported HTTP request method
	 * @param supportedMethods the actually supported HTTP methods (possibly {@code null})
	 */
	private HttpRequestMethodNotSupportedException(String method, @Nullable String[] supportedMethods) {
		super("Request method '" + method + "' is not supported");
		this.method = method;
		this.supportedMethods = supportedMethods;

		String detail = "Method '" + method + "' is not supported.";
		this.body = ProblemDetail.forStatusAndDetail(getStatusCode(), detail);
	}


	/**
	 * Return the HTTP request method that caused the failure.
	 */
	public String getMethod() {
		return this.method;
	}

	/**
	 * Return the actually supported HTTP methods, or {@code null} if not known.
	 */
	@Nullable
	public String[] getSupportedMethods() {
		return this.supportedMethods;
	}

	/**
	 * Return the actually supported HTTP methods as {@link HttpMethod} instances,
	 * or {@code null} if not known.
	 * @since 3.2
	 */
	@Nullable
	public Set<HttpMethod> getSupportedHttpMethods() {
		if (this.supportedMethods == null) {
			return null;
		}
		Set<HttpMethod> supportedMethods = new LinkedHashSet<>(this.supportedMethods.length);
		for (String value : this.supportedMethods) {
			HttpMethod method = HttpMethod.valueOf(value);
			supportedMethods.add(method);
		}
		return supportedMethods;
	}

	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.METHOD_NOT_ALLOWED;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (ObjectUtils.isEmpty(this.supportedMethods)) {
			return HttpHeaders.EMPTY;
		}
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ALLOW, StringUtils.arrayToDelimitedString(this.supportedMethods, ", "));
		return headers;
	}

	@Override
	public ProblemDetail getBody() {
		return this.body;
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] {getMethod(), getSupportedHttpMethods()};
	}

}
