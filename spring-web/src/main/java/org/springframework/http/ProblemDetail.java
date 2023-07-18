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

package org.springframework.http;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Representation for an RFC 7807 problem detail. Includes spec-defined
 * properties, and a {@link #getProperties() properties} map for additional,
 * non-standard properties.
 *
 * <p>For an extended response, an application can add to the
 * {@link #getProperties() properties} map. When using the Jackson library, the
 * {@code properties} map is expanded as top level JSON properties through the
 * {@link org.springframework.http.converter.json.ProblemDetailJacksonMixin}.
 *
 * <p>For an extended response, an application can also create a subclass with
 * additional properties. Subclasses can use the protected copy constructor to
 * re-create an existing {@code ProblemDetail} instance as the subclass, e.g.
 * from an {@code @ControllerAdvice} such as
 * {@link org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler} or
 * {@link org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 6.0
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

	@Nullable
	private Map<String, Object> properties;


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
	 * Copy constructor that a subclass can use to re-create and extend a
	 * {@code ProblemDetail} with additional properties.
	 */
	protected ProblemDetail(ProblemDetail other) {
		this.type = other.type;
		this.title = other.title;
		this.status = other.status;
		this.detail = other.detail;
		this.instance = other.instance;
		this.properties = (other.properties != null ? new LinkedHashMap<>(other.properties) : null);
	}

	/**
	 * No-arg constructor, for deserialization.
	 */
	protected ProblemDetail() {
	}


	/**
	 * Setter for the {@link #getType() problem type}.
	 * <p>By default, this is {@link #BLANK_TYPE}.
	 * @param type the problem type
	 */
	public void setType(URI type) {
		Assert.notNull(type, "'type' is required");
		this.type = type;
	}

	/**
	 * Return the configured {@link #setType(URI) problem type}.
	 */
	public URI getType() {
		return this.type;
	}

	/**
	 * Setter for the {@link #getTitle() problem title}.
	 * <p>By default, if not explicitly set and the status is well-known, this
	 * is sourced from the {@link HttpStatus#getReasonPhrase()}.
	 * @param title the problem title
	 */
	public void setTitle(@Nullable String title) {
		this.title = title;
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
	 * Setter for the {@link #getStatus() problem status}.
	 * @param httpStatus the problem status
	 */
	public void setStatus(HttpStatus httpStatus) {
		this.status = httpStatus.value();
	}

	/**
	 * Setter for the {@link #getStatus() problem status}.
	 * @param status the problem status
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * Return the status associated with the problem, provided either to the
	 * constructor or configured via {@link #setStatus(int)}.
	 */
	public int getStatus() {
		return this.status;
	}

	/**
	 * Setter for the {@link #getDetail() problem detail}.
	 * <p>By default, this is not set.
	 * @param detail the problem detail
	 */
	public void setDetail(@Nullable String detail) {
		this.detail = detail;
	}

	/**
	 * Return the configured {@link #setDetail(String) problem detail}.
	 */
	@Nullable
	public String getDetail() {
		return this.detail;
	}

	/**
	 * Setter for the {@link #getInstance() problem instance}.
	 * <p>By default, when {@code ProblemDetail} is returned from an
	 * {@code @ExceptionHandler} method, this is initialized to the request path.
	 * @param instance the problem instance
	 */
	public void setInstance(@Nullable URI instance) {
		this.instance = instance;
	}

	/**
	 * Return the configured {@link #setInstance(URI) problem instance}.
	 */
	@Nullable
	public URI getInstance() {
		return this.instance;
	}

	/**
	 * Set a "dynamic" property to be added to a generic {@link #getProperties()
	 * properties map}.
	 * <p>When Jackson JSON is present on the classpath, any properties set here
	 * are rendered as top level key-value pairs in the output JSON. Otherwise,
	 * they are rendered as a {@code "properties"} sub-map.
	 * @param name the property name
	 * @param value the property value, possibly {@code null} if the intent is
	 * to include a property with its value set to "null"
	 * @see org.springframework.http.converter.json.ProblemDetailJacksonMixin
	 */
	public void setProperty(String name, @Nullable Object value) {
		this.properties = (this.properties != null ? this.properties : new LinkedHashMap<>());
		this.properties.put(name, value);
	}

	/**
	 * Return a generic map of properties that are not known ahead of time,
	 * possibly {@code null} if no properties have been added. To add a property,
	 * use {@link #setProperty(String, Object)}.
	 * <p>When Jackson JSON is present on the classpath, the content of this map
	 * is unwrapped and rendered as top level key-value pairs in the output JSON.
	 * Otherwise, they are rendered as a {@code "properties"} sub-map.
	 * @see org.springframework.http.converter.json.ProblemDetailJacksonMixin
	 */
	@Nullable
	public Map<String, Object> getProperties() {
		return this.properties;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ProblemDetail that &&
				getType().equals(that.getType()) &&
				ObjectUtils.nullSafeEquals(getTitle(), that.getTitle()) &&
				this.status == that.status &&
				ObjectUtils.nullSafeEquals(this.detail, that.detail) &&
				ObjectUtils.nullSafeEquals(this.instance, that.instance) &&
				ObjectUtils.nullSafeEquals(this.properties, that.properties)));
	}

	@Override
	public int hashCode() {
		int result = this.type.hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(getTitle());
		result = 31 * result + this.status;
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.detail);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.instance);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.properties);
		return result;
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
		return "type='" + getType() + "'" +
				", title='" + getTitle() + "'" +
				", status=" + getStatus() +
				", detail='" + getDetail() + "'" +
				", instance='" + getInstance() + "'" +
				", properties='" + getProperties() + "'";
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

	/**
	 * Create a {@code ProblemDetail} instance with the given status and detail.
	 */
	public static ProblemDetail forStatusAndDetail(HttpStatusCode status, String detail) {
		Assert.notNull(status, "HttpStatusCode is required");
		ProblemDetail problemDetail = forStatus(status.value());
		problemDetail.setDetail(detail);
		return problemDetail;
	}

}
