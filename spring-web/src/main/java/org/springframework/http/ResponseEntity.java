/*
 * Copyright 2002-present the original author or authors.
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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * Extension of {@link HttpEntity} that adds an {@link HttpStatusCode} status code.
 * Used in {@code RestTemplate} as well as in {@code @Controller} methods.
 *
 * <p>In {@code RestTemplate}, this class is returned by
 * {@link org.springframework.web.client.RestTemplate#getForEntity getForEntity()} and
 * {@link org.springframework.web.client.RestTemplate#exchange exchange()}:
 * <pre class="code">
 * ResponseEntity&lt;String&gt; entity = template.getForEntity("https://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * HttpStatus statusCode = entity.getStatusCode();
 * </pre>
 *
 * <p>This can also be used in Spring MVC as the return value from an
 * {@code @Controller} method:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   HttpHeaders responseHeaders = new HttpHeaders();
 *   responseHeaders.setLocation(location);
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new ResponseEntity&lt;String&gt;("Hello World", responseHeaders, HttpStatus.CREATED);
 * }
 * </pre>
 *
 * Or, by using a builder accessible via static methods:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   return ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body("Hello World");
 * }
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @since 3.0.2
 * @param <T> the body type
 * @see #getStatusCode()
 * @see org.springframework.web.client.RestOperations#getForEntity(String, Class, Object...)
 * @see org.springframework.web.client.RestOperations#getForEntity(String, Class, java.util.Map)
 * @see org.springframework.web.client.RestOperations#getForEntity(URI, Class)
 * @see RequestEntity
 */
public class ResponseEntity<T> extends HttpEntity<T> {

	private final HttpStatusCode status;


	/**
	 * Create a {@code ResponseEntity} with a status code only.
	 * @param status the status code
	 */
	public ResponseEntity(HttpStatusCode status) {
		this(null, (HttpHeaders) null, status);
	}

	/**
	 * Create a {@code ResponseEntity} with a body and status code.
	 * @param body the entity body
	 * @param status the status code
	 */
	public ResponseEntity(@Nullable T body, HttpStatusCode status) {
		this(body, (HttpHeaders) null, status);
	}

	/**
	 * Create a {@code ResponseEntity} with headers and a status code.
	 * @param headers the entity headers
	 * @param status the status code
	 * @since 7.0
	 */
	public ResponseEntity(HttpHeaders headers, HttpStatusCode status) {
		this(null, headers, status);
	}

	/**
	 * Create a {@code ResponseEntity} with a body, headers, and a raw status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param rawStatus the status code value
	 * @since 7.0
	 */
	public ResponseEntity(@Nullable T body, @Nullable HttpHeaders headers, int rawStatus) {
		this(body, headers, HttpStatusCode.valueOf(rawStatus));
	}

	/**
	 * Create a {@code ResponseEntity} with a body, headers, and a status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param statusCode the status code
	 * @since 7.0
	 */
	public ResponseEntity(@Nullable T body, @Nullable HttpHeaders headers, HttpStatusCode statusCode) {
		super(body, headers);
		Assert.notNull(statusCode, "HttpStatusCode must not be null");

		this.status = statusCode;
	}

	/**
	 * Create a {@code ResponseEntity} with headers and a status code.
	 * @param headers the entity headers
	 * @param status the status code
	 * @deprecated in favor of {@link #ResponseEntity(HttpHeaders, HttpStatusCode)}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public ResponseEntity(MultiValueMap<String, String> headers, HttpStatusCode status) {
		this(null, headers, status);
	}

	/**
	 * Create a {@code ResponseEntity} with a body, headers, and a raw status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param rawStatus the status code value
	 * @since 5.3.2
	 * @deprecated in favor of {@link #ResponseEntity(Object, HttpHeaders, int)}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, int rawStatus) {
		this(body, headers, HttpStatusCode.valueOf(rawStatus));
	}

	/**
	 * Create a {@code ResponseEntity} with a body, headers, and a status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param statusCode the status code
	 * @deprecated in favor of {@link #ResponseEntity(Object, HttpHeaders, HttpStatusCode)}
	 */
	@SuppressWarnings("removal")
	@Deprecated(since = "7.0", forRemoval = true)
	public ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, HttpStatusCode statusCode) {
		super(body, headers);
		Assert.notNull(statusCode, "HttpStatusCode must not be null");

		this.status = statusCode;
	}


	/**
	 * Return the HTTP status code of the response.
	 * @return the HTTP status as an HttpStatus enum entry
	 */
	public HttpStatusCode getStatusCode() {
		return this.status;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		return (other instanceof ResponseEntity<?> otherEntity && ObjectUtils.nullSafeEquals(this.status, otherEntity.status));
	}

	@Override
	public int hashCode() {
		return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.status));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("<");
		builder.append(this.status);
		if (this.status instanceof HttpStatus httpStatus) {
			builder.append(' ');
			builder.append(httpStatus.getReasonPhrase());
		}
		builder.append(',');
		T body = getBody();
		HttpHeaders headers = getHeaders();
		if (body != null) {
			builder.append(body);
			builder.append(',');
		}
		builder.append(headers);
		builder.append('>');
		return builder.toString();
	}


	// Static builder methods

	/**
	 * Create a builder with the given status.
	 * @param status the response status
	 * @return the created builder
	 * @since 4.1
	 */
	public static BodyBuilder status(HttpStatusCode status) {
		Assert.notNull(status, "HttpStatusCode must not be null");
		return new DefaultBuilder(status);
	}

	/**
	 * Create a builder with the given status.
	 * @param status the response status
	 * @return the created builder
	 * @since 4.1
	 */
	public static BodyBuilder status(int status) {
		return new DefaultBuilder(status);
	}

	/**
	 * Create a builder with the status set to {@linkplain HttpStatus#OK OK}.
	 * @return the created builder
	 * @since 4.1
	 */
	public static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * A shortcut for creating a {@code ResponseEntity} with the given body
	 * and the status set to {@linkplain HttpStatus#OK OK}.
	 * @param body the body of the response entity (possibly empty)
	 * @return the created {@code ResponseEntity}
	 * @since 4.1
	 */
	public static <T> ResponseEntity<T> ok(@Nullable T body) {
		return ok().body(body);
	}

	/**
	 * A shortcut for creating a {@code ResponseEntity} with the given body
	 * and the {@linkplain HttpStatus#OK OK} status, or an empty body and a
	 * {@linkplain HttpStatus#NOT_FOUND NOT FOUND} status in case of an
	 * {@linkplain Optional#empty()} parameter.
	 * @return the created {@code ResponseEntity}
	 * @since 5.1
	 */
	public static <T> ResponseEntity<T> of(Optional<T> body) {
		Assert.notNull(body, "Body must not be null");
		return body.map(ResponseEntity::ok).orElseGet(() -> notFound().build());
	}

	/**
	 * Create a new {@link HeadersBuilder} with its status set to
	 * {@link ProblemDetail#getStatus()} and its body is set to
	 * {@link ProblemDetail}.
	 * <p><strong>Note:</strong> If there are no headers to add, there is usually
	 * no need to create a {@link ResponseEntity} since {@code ProblemDetail}
	 * is also supported as a return value from controller methods.
	 * @param body the problem detail to use
	 * @return the created builder
	 * @since 6.0
	 */
	public static HeadersBuilder<?> of(ProblemDetail body) {
		return new DefaultBuilder(body.getStatus()) {

			@SuppressWarnings("unchecked")
			@Override
			public <T> ResponseEntity<T> build() {
				return (ResponseEntity<T>) body(body);
			}
		};
	}

	/**
	 * A shortcut for creating a {@code ResponseEntity} with the given body
	 * and the {@linkplain HttpStatus#OK OK} status, or an empty body and a
	 * {@linkplain HttpStatus#NOT_FOUND NOT FOUND} status in case of a
	 * {@code null} parameter.
	 * @return the created {@code ResponseEntity}
	 * @since 6.0.5
	 */
	public static <T> ResponseEntity<T> ofNullable(@Nullable T body) {
		if (body == null) {
			return notFound().build();
		}
		return ResponseEntity.ok(body);
	}

	/**
	 * Create a new builder with a {@linkplain HttpStatus#CREATED CREATED} status
	 * and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 * @since 4.1
	 */
	public static BodyBuilder created(URI location) {
		return status(HttpStatus.CREATED).location(location);
	}

	/**
	 * Create a builder with an {@linkplain HttpStatus#ACCEPTED ACCEPTED} status.
	 * @return the created builder
	 * @since 4.1
	 */
	public static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#NO_CONTENT NO_CONTENT} status.
	 * @return the created builder
	 * @since 4.1
	 */
	public static HeadersBuilder<?> noContent() {
		return status(HttpStatus.NO_CONTENT);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#BAD_REQUEST BAD_REQUEST} status.
	 * @return the created builder
	 * @since 4.1
	 */
	public static BodyBuilder badRequest() {
		return status(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#NOT_FOUND NOT_FOUND} status.
	 * @return the created builder
	 * @since 4.1
	 */
	public static HeadersBuilder<?> notFound() {
		return status(HttpStatus.NOT_FOUND);
	}

	/**
	 * Create a builder with an
	 * {@linkplain HttpStatus#UNPROCESSABLE_ENTITY UNPROCESSABLE_ENTITY} status.
	 * @return the created builder
	 * @since 4.1.3
	 */
	public static BodyBuilder unprocessableEntity() {
		return status(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	/**
	 * Create a builder with an
	 * {@linkplain HttpStatus#INTERNAL_SERVER_ERROR INTERNAL_SERVER_ERROR} status.
	 * @return the created builder
	 * @since 5.3.8
	 */
	public static BodyBuilder internalServerError() {
		return status(HttpStatus.INTERNAL_SERVER_ERROR);
	}


	/**
	 * Defines a builder that adds headers to the response entity.
	 * @since 4.1
	 * @param <B> the builder subclass
	 */
	public interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * Copy the given headers into the entity's headers map.
		 * @param headers the existing HttpHeaders to copy from
		 * @return this builder
		 * @since 4.1.2
		 * @see HttpHeaders#add(String, String)
		 */
		B headers(@Nullable HttpHeaders headers);

		/**
		 * Manipulate this entity's headers with the given consumer. The
		 * headers provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
		 * {@linkplain HttpHeaders#remove(String) remove} values, or use any of the other
		 * {@link HttpHeaders} methods.
		 * @param headersConsumer a function that consumes the {@code HttpHeaders}
		 * @return this builder
		 * @since 5.2
		 */
		B headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
		 * by the {@code Allow} header.
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(HttpMethod... allowedMethods);

		/**
		 * Set the entity tag of the body, as specified by the {@code ETag} header.
		 * @param etag the new entity tag
		 * @return this builder
		 * @see HttpHeaders#setETag(String)
		 */
		B eTag(@Nullable String etag);

		/**
		 * Set the time the resource was last changed, as specified by the
		 * {@code Last-Modified} header.
		 * @param lastModified the last modified date
		 * @return this builder
		 * @since 5.1.4
		 * @see HttpHeaders#setLastModified(ZonedDateTime)
		 */
		B lastModified(ZonedDateTime lastModified);

		/**
		 * Set the time the resource was last changed, as specified by the
		 * {@code Last-Modified} header.
		 * @param lastModified the last modified date
		 * @return this builder
		 * @since 5.1.4
		 * @see HttpHeaders#setLastModified(Instant)
		 */
		B lastModified(Instant lastModified);

		/**
		 * Set the time the resource was last changed, as specified by the
		 * {@code Last-Modified} header.
		 * <p>The date should be specified as the number of milliseconds since
		 * January 1, 1970 GMT.
		 * @param lastModified the last modified date
		 * @return this builder
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(long lastModified);

		/**
		 * Set the location of a resource, as specified by the {@code Location} header.
		 * @param location the location
		 * @return this builder
		 * @see HttpHeaders#setLocation(URI)
		 */
		B location(URI location);

		/**
		 * Set the caching directives for the resource, as specified by the HTTP 1.1
		 * {@code Cache-Control} header.
		 * <p>A {@code CacheControl} instance can be built like
		 * {@code CacheControl.maxAge(3600).cachePublic().noTransform()}.
		 * @param cacheControl a builder for cache-related HTTP response headers
		 * @return this builder
		 * @since 4.2
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		B cacheControl(CacheControl cacheControl);

		/**
		 * Configure one or more request header names (for example, "Accept-Language") to
		 * add to the "Vary" response header to inform clients that the response is
		 * subject to content negotiation and variances based on the value of the
		 * given request headers. The configured request header names are added only
		 * if not already present in the response "Vary" header.
		 * @param requestHeaders request header names
		 * @since 4.3
		 */
		B varyBy(String... requestHeaders);

		/**
		 * Build the response entity with no body.
		 * @return the response entity
		 * @see BodyBuilder#body(Object)
		 */
		<T> ResponseEntity<T> build();
	}


	/**
	 * Defines a builder that adds a body to the response entity.
	 * @since 4.1
	 */
	public interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 * @param contentLength the content length
		 * @return this builder
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified by the
		 * {@code Content-Type} header.
		 * @param contentType the content type
		 * @return this builder
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * Set the body of the response entity and returns it.
		 * @param <T> the type of the body
		 * @param body the body of the response entity
		 * @return the built response entity
		 */
		<T> ResponseEntity<T> body(@Nullable T body);
	}


	private static class DefaultBuilder implements BodyBuilder {

		private final HttpStatusCode statusCode;

		private final HttpHeaders headers = new HttpHeaders();


		public DefaultBuilder(int statusCode) {
			this(HttpStatusCode.valueOf(statusCode));
		}

		public DefaultBuilder(HttpStatusCode statusCode) {
			this.statusCode = statusCode;
		}


		@Override
		public BodyBuilder header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				this.headers.add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public BodyBuilder headers(@Nullable HttpHeaders headers) {
			if (headers != null) {
				this.headers.putAll(headers);
			}
			return this;
		}

		@Override
		public BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
			headersConsumer.accept(this.headers);
			return this;
		}

		@Override
		public BodyBuilder allow(HttpMethod... allowedMethods) {
			this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
			return this;
		}

		@Override
		public BodyBuilder contentLength(long contentLength) {
			this.headers.setContentLength(contentLength);
			return this;
		}

		@Override
		public BodyBuilder contentType(MediaType contentType) {
			this.headers.setContentType(contentType);
			return this;
		}

		@Override
		public BodyBuilder eTag(@Nullable String tag) {
			this.headers.setETag(tag);
			return this;
		}

		@Override
		public BodyBuilder lastModified(ZonedDateTime date) {
			this.headers.setLastModified(date);
			return this;
		}

		@Override
		public BodyBuilder lastModified(Instant date) {
			this.headers.setLastModified(date);
			return this;
		}

		@Override
		public BodyBuilder lastModified(long date) {
			this.headers.setLastModified(date);
			return this;
		}

		@Override
		public BodyBuilder location(URI location) {
			this.headers.setLocation(location);
			return this;
		}

		@Override
		public BodyBuilder cacheControl(CacheControl cacheControl) {
			this.headers.setCacheControl(cacheControl);
			return this;
		}

		@Override
		public BodyBuilder varyBy(String... requestHeaders) {
			this.headers.setVary(Arrays.asList(requestHeaders));
			return this;
		}

		@Override
		public <T> ResponseEntity<T> build() {
			return body(null);
		}

		@Override
		public <T> ResponseEntity<T> body(@Nullable T body) {
			return new ResponseEntity<>(body, this.headers, this.statusCode);
		}
	}

}
