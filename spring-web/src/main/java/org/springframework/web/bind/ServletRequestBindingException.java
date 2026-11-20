/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind;

import jakarta.servlet.ServletException;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

/**
 * Fatal binding exception, thrown when we want to treat binding exceptions as
 * unrecoverable.
 *
 * <p>Extends {@link ServletException} for convenient throwing in any Servlet
 * resource (such as a Filter).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ServletRequestBindingException extends ServletException implements ErrorResponse {

	private final String messageDetailCode;

	private final Object @Nullable [] messageDetailArguments;

	private @Nullable ProblemDetail body;


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
			@Nullable String msg, @Nullable String messageDetailCode, Object @Nullable [] messageDetailArguments) {

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
			@Nullable String messageDetailCode, Object @Nullable [] messageDetailArguments) {

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
	public synchronized ProblemDetail getBody() {
		if (this.body == null) {
			this.body = ProblemDetail.forStatus(getStatusCode());
		}
		return this.body;
	}

	@Override
	public String getDetailMessageCode() {
		return this.messageDetailCode;
	}

	@Override
	public Object @Nullable [] getDetailMessageArguments() {
		return this.messageDetailArguments;
	}

}
