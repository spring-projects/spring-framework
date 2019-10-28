/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Exception thrown when a request handler does not support a
 * specific request method.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class HttpRequestMethodNotSupportedException extends ServletException {

	private final String method;

	@Nullable
	private final String[] supportedMethods;


	/**
	 * Create a new HttpRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 */
	public HttpRequestMethodNotSupportedException(String method) {
		this(method, (String[]) null);
	}

	/**
	 * Create a new HttpRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 * @param msg the detail message
	 */
	public HttpRequestMethodNotSupportedException(String method, String msg) {
		this(method, null, msg);
	}

	/**
	 * Create a new HttpRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 * @param supportedMethods the actually supported HTTP methods (may be {@code null})
	 */
	public HttpRequestMethodNotSupportedException(String method, @Nullable Collection<String> supportedMethods) {
		this(method, (supportedMethods != null ? StringUtils.toStringArray(supportedMethods) : null));
	}

	/**
	 * Create a new HttpRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 * @param supportedMethods the actually supported HTTP methods (may be {@code null})
	 */
	public HttpRequestMethodNotSupportedException(String method, @Nullable String[] supportedMethods) {
		this(method, supportedMethods, "Request method '" + method + "' not supported");
	}

	/**
	 * Create a new HttpRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 * @param supportedMethods the actually supported HTTP methods
	 * @param msg the detail message
	 */
	public HttpRequestMethodNotSupportedException(String method, @Nullable String[] supportedMethods, String msg) {
		super(msg);
		this.method = method;
		this.supportedMethods = supportedMethods;
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
		List<HttpMethod> supportedMethods = new LinkedList<>();
		for (String value : this.supportedMethods) {
			HttpMethod resolved = HttpMethod.resolve(value);
			if (resolved != null) {
				supportedMethods.add(resolved);
			}
		}
		return EnumSet.copyOf(supportedMethods);
	}

}
