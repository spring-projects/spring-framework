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

package org.springframework.web;

import java.net.URI;

import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetails;
import org.springframework.lang.Nullable;

/**
 * {@link RuntimeException} that implements {@link ErrorResponse} to expose
 * an HTTP status, response headers, and a body formatted as an RFC 7808
 * {@link ProblemDetails}.
 *
 * <p>The exception can be used as is, or it can be extended as a more specific
 * exception that populates the {@link ProblemDetails#setType(URI) type} or
 * {@link ProblemDetails#setDetail(String) detail} fields, or potentially adds
 * other non-standard properties.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
@SuppressWarnings("serial")
public class ErrorResponseException extends NestedRuntimeException implements ErrorResponse {

	private final HttpStatusCode status;

	private final HttpHeaders headers = new HttpHeaders();

	private final ProblemDetails body;

	private final String messageDetailCode;

	@Nullable
	private final Object[] messageDetailArguments;


	/**
	 * Constructor with a {@link HttpStatusCode}.
	 */
	public ErrorResponseException(HttpStatusCode status) {
		this(status, null);
	}

	/**
	 * Constructor with a {@link HttpStatusCode} and an optional cause.
	 */
	public ErrorResponseException(HttpStatusCode status, @Nullable Throwable cause) {
		this(status, ProblemDetails.forStatus(status), cause);
	}

	/**
	 * Constructor with a given {@link ProblemDetails} instance, possibly a
	 * subclass of {@code ProblemDetails} with extended fields.
	 */
	public ErrorResponseException(HttpStatusCode status, ProblemDetails body, @Nullable Throwable cause) {
		this(status, body, cause, null, null);
	}

	/**
	 * Constructor with a given {@link ProblemDetails}, and a
	 * {@link org.springframework.context.MessageSource} code and arguments to
	 * resolve the detail message with.
	 * @since 6.0
	 */
	public ErrorResponseException(
			HttpStatusCode status, ProblemDetails body, @Nullable Throwable cause,
			@Nullable String messageDetailCode, @Nullable Object[] messageDetailArguments) {

		super(null, cause);
		this.status = status;
		this.body = body;
		this.messageDetailCode = initMessageDetailCode(messageDetailCode);
		this.messageDetailArguments = messageDetailArguments;
	}

	private String initMessageDetailCode(@Nullable String messageDetailCode) {
		return (messageDetailCode != null ?
				messageDetailCode : ErrorResponse.getDefaultDetailMessageCode(getClass(), null));
	}


	@Override
	public HttpStatusCode getStatusCode() {
		return this.status;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * Set the {@link ProblemDetails#setType(URI) type} field of the response body.
	 * @param type the problem type
	 */
	public void setType(URI type) {
		this.body.setType(type);
	}

	/**
	 * Set the {@link ProblemDetails#setTitle(String) title} field of the response body.
	 * @param title the problem title
	 */
	public void setTitle(@Nullable String title) {
		this.body.setTitle(title);
	}

	/**
	 * Set the {@link ProblemDetails#setDetail(String) detail} field of the response body.
	 * @param detail the problem detail
	 */
	public void setDetail(@Nullable String detail) {
		this.body.setDetail(detail);
	}

	/**
	 * Set the {@link ProblemDetails#setInstance(URI) instance} field of the response body.
	 * @param instance the problem instance
	 */
	public void setInstance(@Nullable URI instance) {
		this.body.setInstance(instance);
	}

	/**
	 * Return the body for the response. To customize the body content, use:
	 * <ul>
	 * <li>{@link #setType(URI)}
	 * <li>{@link #setTitle(String)}
	 * <li>{@link #setDetail(String)}
	 * <li>{@link #setInstance(URI)}
	 * </ul>
	 * <p>By default, the status field of {@link ProblemDetails} is initialized
	 * from the status provided to the constructor, which in turn may also
	 * initialize the title field from the status reason phrase, if the status
	 * is well-known. The instance field, if not set, is initialized from the
	 * request path when a {@code ProblemDetails} is returned from an
	 * {@code @ExceptionHandler} method.
	 */
	@Override
	public final ProblemDetails getBody() {
		return this.body;
	}

	@Override
	public String getDetailMessageCode() {
		return this.messageDetailCode;
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return this.messageDetailArguments;
	}

	@Override
	public String getMessage() {
		return this.status + (!this.headers.isEmpty() ? ", headers=" + this.headers : "") + ", " + this.body;
	}

}
