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

package org.springframework.http;

import java.net.URI;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Representation of an RFC 7807 problem detail, including all RFC-defined
 * fields. For an extended response with more fields, create a subclass that
 * exposes the additional fields.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC 7807</a>
 * @see org.springframework.web.ErrorResponse
 * @see org.springframework.web.ErrorResponseException
 */
public class ProblemDetail {

	private static final URI BLANK_TYPE = URI.create("about:blank");


	private URI type = BLANK_TYPE;

	@Nullable
	private String title;

	private int status;

	@Nullable
	private String detail;

	@Nullable
	private URI instance;


	/**
	 * Protected constructor for subclasses.
	 * <p>To create a {@link ProblemDetail} instance, use static factory methods,
	 * {@link #forStatus(HttpStatusCode)} or {@link #forStatus(int)}.
	 * @param rawStatusCode the response status to use
	 */
	protected ProblemDetail(int rawStatusCode) {
		this.status = rawStatusCode;
	}

	/**
	 * Copy constructor that could be used from a subclass to re-create a
	 * {@code ProblemDetail} in order to extend it with more fields.
	 */
	protected ProblemDetail(ProblemDetail other) {
		this.type = other.type;
		this.title = other.title;
		this.status = other.status;
		this.detail = other.detail;
		this.instance = other.instance;
	}

	/**
	 * For deserialization.
	 */
	protected ProblemDetail() {
	}


	/**
	 * Variant of {@link #setType(URI)} for chained initialization.
	 * @param type the problem type
	 * @return the same instance
	 */
	public ProblemDetail withType(URI type) {
		setType(type);
		return this;
	}

	/**
	 * Variant of {@link #setTitle(String)} for chained initialization.
	 * @param title the problem title
	 * @return the same instance
	 */
	public ProblemDetail withTitle(@Nullable String title) {
		setTitle(title);
		return this;
	}

	/**
	 * Variant of {@link #setStatus(int)} for chained initialization.
	 * @param statusCode the response status for the problem
	 * @return the same instance
	 */
	public ProblemDetail withStatus(HttpStatusCode statusCode) {
		Assert.notNull(statusCode, "HttpStatus is required");
		setStatus(statusCode.value());
		return this;
	}

	/**
	 * Variant of {@link #setStatus(int)} for chained initialization.
	 * @param status the response status value for the problem
	 * @return the same instance
	 */
	public ProblemDetail withStatus(int status) {
		setStatus(status);
		return this;
	}

	/**
	 * Variant of {@link #setDetail(String)} for chained initialization.
	 * @param detail the problem detail
	 * @return the same instance
	 */
	public ProblemDetail withDetail(@Nullable String detail) {
		setDetail(detail);
		return this;
	}

	/**
	 * Variant of {@link #setInstance(URI)} for chained initialization.
	 * @param instance the problem instance URI
	 * @return the same instance
	 */
	public ProblemDetail withInstance(@Nullable URI instance) {
		setInstance(instance);
		return this;
	}


	// Setters for deserialization

	/**
	 * Setter for the {@link #getType() problem type}.
	 * <p>By default, this is {@link #BLANK_TYPE}.
	 * @param type the problem type
	 * @see #withType(URI)
	 */
	public void setType(URI type) {
		Assert.notNull(type, "'type' is required");
		this.type = type;
	}

	/**
	 * Setter for the {@link #getTitle() problem title}.
	 * <p>By default, if not explicitly set and the status is well-known, this
	 * is sourced from the {@link HttpStatus#getReasonPhrase()}.
	 * @param title the problem title
	 * @see #withTitle(String)
	 */
	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	/**
	 * Setter for the {@link #getStatus() problem status}.
	 * @param status the problem status
	 * @see #withStatus(HttpStatusCode)
	 * @see #withStatus(int)
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * Setter for the {@link #getDetail() problem detail}.
	 * <p>By default, this is not set.
	 * @param detail the problem detail
	 * @see #withDetail(String)
	 */
	public void setDetail(@Nullable String detail) {
		this.detail = detail;
	}

	/**
	 * Setter for the {@link #getInstance() problem instance}.
	 * <p>By default, when {@code ProblemDetail} is returned from an
	 * {@code @ExceptionHandler} method, this is initialized to the request path.
	 * @param instance the problem instance
	 * @see #withInstance(URI)
	 */
	public void setInstance(@Nullable URI instance) {
		this.instance = instance;
	}


	// Getters

	/**
	 * Return the configured {@link #setType(URI) problem type}.
	 */
	public URI getType() {
		return this.type;
	}

	/**
	 * Return the configured {@link #setTitle(String) problem title}.
	 */
	@Nullable
	public String getTitle() {
		if (this.title == null) {
			HttpStatus httpStatus = HttpStatus.resolve(this.status);
			if (httpStatus != null) {
				return httpStatus.getReasonPhrase();
			}
		}
		return this.title;
	}

	/**
	 * Return the status associated with the problem, provided either to the
	 * constructor or configured via {@link #setStatus(int)}.
	 */
	public int getStatus() {
		return this.status;
	}

	/**
	 * Return the configured {@link #setDetail(String) problem detail}.
	 */
	@Nullable
	public String getDetail() {
		return this.detail;
	}

	/**
	 * Return the configured {@link #setInstance(URI) problem instance}.
	 */
	@Nullable
	public URI getInstance() {
		return this.instance;
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + initToStringContent() + "]";
	}

	/**
	 * Return a String representation of the {@code ProblemDetail} fields.
	 * Subclasses can override this to append additional fields.
	 */
	protected String initToStringContent() {
		return "type='" + this.type + "'" +
				", title='" + getTitle() + "'" +
				", status=" + getStatus() +
				", detail='" + getDetail() + "'" +
				", instance='" + getInstance() + "'";
	}


	// Static factory methods

	/**
	 * Create a {@code ProblemDetail} instance with the given status code.
	 */
	public static ProblemDetail forStatus(HttpStatusCode status) {
		Assert.notNull(status, "HttpStatusCode is required");
		return forStatus(status.value());
	}

	/**
	 * Create a {@code ProblemDetail} instance with the given status value.
	 */
	public static ProblemDetail forStatus(int status) {
		return new ProblemDetail(status);
	}

}
