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

package org.springframework.web.multipart.support;

import jakarta.servlet.ServletException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.multipart.MultipartResolver;

/**
 * Signals the part of a "multipart/form-data" request, identified by name
 * could not be found. This may be because the request is not a multipart
 * request, or a part with that name is not present, or because the application
 * is not configured correctly for processing multipart requests, e.g. there
 * is no {@link MultipartResolver}.
 *
 * <p><strong>Note:</strong> This exception does not extend from
 * {@link org.springframework.web.bind.ServletRequestBindingException} because
 * it can also be raised at a lower level, i.e. from this package which does
 * low level multipart request parsing, independent of higher level request
 * binding features.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@SuppressWarnings("serial")
public class MissingServletRequestPartException extends ServletException implements ErrorResponse {

	private final String requestPartName;

	private final ProblemDetail body = ProblemDetail.forStatus(getStatusCode());


	/**
	 * Constructor for MissingServletRequestPartException.
	 * @param requestPartName the name of the missing part of the multipart request
	 */
	public MissingServletRequestPartException(String requestPartName) {
		super("Required part '" + requestPartName + "' is not present.");
		this.requestPartName = requestPartName;
		getBody().setDetail(getMessage());
	}


	/**
	 * Return the name of the offending part of the multipart request.
	 */
	public String getRequestPartName() {
		return this.requestPartName;
	}

	/**
	 * Return the HTTP status code to use for the response.
	 */
	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.BAD_REQUEST;
	}

	/**
	 * Return the body for the response, formatted as an RFC 7807
	 * {@link ProblemDetail} whose {@link ProblemDetail#getStatus() status}
	 * should match the response status.
	 */
	@Override
	public ProblemDetail getBody() {
		return this.body;
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] {getRequestPartName()};
	}

}
