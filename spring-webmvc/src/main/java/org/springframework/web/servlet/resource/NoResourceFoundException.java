/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	public NoResourceFoundException(HttpMethod httpMethod, String requestUri, String resourcePath) {
		super("No static resource " + resourcePath + " for request '" + requestUri + "'.");
		this.httpMethod = httpMethod;
		this.resourcePath = resourcePath;
		this.body = ProblemDetail.forStatusAndDetail(getStatusCode(), "No static resource " + resourcePath + ".");
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

	@Override
	public Object[] getDetailMessageArguments() {
		return new String[] { this.resourcePath };
	}
}
