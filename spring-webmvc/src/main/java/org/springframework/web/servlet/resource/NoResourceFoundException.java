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

package org.springframework.web.servlet.resource;

import jakarta.servlet.ServletException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

/**
 * Raised when {@link ResourceHttpRequestHandler} can not find a resource.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 * @see org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
 * @see org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
 */
@SuppressWarnings("serial")
public class NoResourceFoundException extends ServletException implements ErrorResponse {

	private final HttpMethod httpMethod;

	private final String resourcePath;

	private final ProblemDetail body;


	/**
	 * Create an instance.
	 */
	public NoResourceFoundException(HttpMethod httpMethod, String resourcePath) {
		super("No static resource " + resourcePath + ".");
		this.httpMethod = httpMethod;
		this.resourcePath = resourcePath;
		this.body = ProblemDetail.forStatusAndDetail(getStatusCode(), getMessage());
	}


	/**
	 * Return the HTTP method for the request.
	 */
	public HttpMethod getHttpMethod() {
		return this.httpMethod;
	}

	/**
	 * Return the path used to locate the resource.
	 * @see org.springframework.web.servlet.HandlerMapping#PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE
	 */
	public String getResourcePath() {
		return this.resourcePath;
	}

	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.NOT_FOUND;
	}

	@Override
	public ProblemDetail getBody() {
		return this.body;
	}

}
