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

package org.springframework.web.bind;

import jakarta.servlet.ServletException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;
import org.springframework.web.ErrorResponse;

/**
 * Fatal binding exception, thrown when we want to
 * treat binding exceptions as unrecoverable.
 *
 * <p>Extends ServletException for convenient throwing in any Servlet resource
 * (such as a Filter), and NestedServletException for proper root cause handling
 * (as the plain ServletException doesn't expose its root cause at all).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ServletRequestBindingException extends ServletException implements ErrorResponse {

	private final ProblemDetail body = ProblemDetail.forStatus(getStatusCode());

	private final String messageDetailCode;

	@Nullable
	private final Object[] messageDetailArguments;


	/**
	 * Constructor with a message only.
	 * @param msg the detail message
	 */
	public ServletRequestBindingException(@Nullable String msg) {
		this(msg, null, null);
	}

	/**
	 * Constructor with a message and a cause.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public ServletRequestBindingException(@Nullable String msg, @Nullable Throwable cause) {
		this(msg, cause, null, null);
	}

	/**
	 * Constructor for ServletRequestBindingException.
	 * @param msg the detail message
	 * @param messageDetailCode the code to use to resolve the problem "detail"
	 * through a {@link org.springframework.context.MessageSource}
	 * @param messageDetailArguments the arguments to make available when
	 * resolving the problem "detail" through a {@code MessageSource}
	 * @since 6.0
	 */
	protected ServletRequestBindingException(
			@Nullable String msg, @Nullable String messageDetailCode, @Nullable Object[] messageDetailArguments) {

		this(msg, null, messageDetailCode, messageDetailArguments);
	}

	/**
	 * Constructor for ServletRequestBindingException.
	 * @param msg the detail message
	 * @param cause the root cause
	 * @param messageDetailCode the code to use to resolve the problem "detail"
	 * through a {@link org.springframework.context.MessageSource}
	 * @param messageDetailArguments the arguments to make available when
	 * resolving the problem "detail" through a {@code MessageSource}
	 * @since 6.0
	 */
	protected ServletRequestBindingException(@Nullable String msg, @Nullable Throwable cause,
			@Nullable String messageDetailCode, @Nullable Object[] messageDetailArguments) {

		super(msg, cause);
		this.messageDetailCode = initMessageDetailCode(messageDetailCode);
		this.messageDetailArguments = messageDetailArguments;
	}

	private String initMessageDetailCode(@Nullable String messageDetailCode) {
		return (messageDetailCode != null ?
				messageDetailCode : ErrorResponse.getDefaultDetailMessageCode(getClass(), null));
	}


	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatus.BAD_REQUEST;
	}

	@Override
	public ProblemDetail getBody() {
		return this.body;
	}

	@Override
	public String getDetailMessageCode() {
		return this.messageDetailCode;
	}

	@Override
	@Nullable
	public Object[] getDetailMessageArguments() {
		return this.messageDetailArguments;
	}

}
