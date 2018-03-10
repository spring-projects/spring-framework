/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * Extension of {@link HttpEntity} that adds a {@link HttpStatus} status code.
 * Used in {@code RestTemplate} as well {@code @Controller} methods.
 *
 * <p>In {@code RestTemplate}, this class is returned by
 * {@link org.springframework.web.client.RestTemplate#getForEntity getForEntity()} and
 * {@link org.springframework.web.client.RestTemplate#exchange exchange()}:
 * <pre class="code">
 * ResponseEntity&lt;String&gt; entity = template.getForEntity("http://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * HttpStatus statusCode = entity.getStatusCode();
 * </pre>
 *
 * <p>Can also be used in Spring MVC, as the return value from a @Controller method:
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
 * @since 3.0.2
 * @see #getStatusCode()
 */
public class ResponseEntity<T> extends HttpEntity<T> {

	private final Object status;


	/**
	 * Create a new {@code ResponseEntity} with the given status code, and no body nor headers.
	 * @param status the status code
	 */
	public ResponseEntity(HttpStatus status) {
		this(null, null, status);
	}

	/**
	 * Create a new {@code ResponseEntity} with the given body and status code, and no headers.
	 * @param body the entity body
	 * @param status the status code
	 */
	public ResponseEntity(@Nullable T body, HttpStatus status) {
		this(body, null, status);
	}

	/**
	 * Create a new {@code HttpEntity} with the given headers and status code, and no body.
	 * @param headers the entity headers
	 * @param status the status code
	 */
	public ResponseEntity(MultiValueMap<String, String> headers, HttpStatus status) {
		this(null, headers, status);
	}

	/**
	 * Create a new {@code HttpEntity} with the given body, headers, and status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param status the status code
	 */
	public ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, HttpStatus status) {
		super(body, headers);
		Assert.notNull(status, "HttpStatus must not be null");
		this.status = status;
	}

	/**
	 * Create a new {@code HttpEntity} with the given body, headers, and status code.
	 * Just used behind the nested builder API.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param status the status code (as {@code HttpStatus} or as {@code Integer} value)
	 */
	private ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, Object status) {
		super(body, headers);
		Assert.notNull(status, "HttpStatus must not be null");
		this.status = status;
	}


	/**
	 * Return the HTTP status code of the response.
	 * @return the HTTP status as an HttpStatus enum entry
	 */
	public HttpStatus getStatusCode() {
		if (this.status instanceof HttpStatus) {
			return (HttpStatus) this.status;
		}
		else {
			return HttpStatus.valueOf((Integer) this.status);
		}
	}

	/**
	 * Return the HTTP status code of the response.
	 * @return the HTTP status as an int value
	 * @since 4.3
	 */
	public int getStatusCodeValue() {
		if (this.status instanceof HttpStatus) {
			return ((HttpStatus) this.status).value();
		}
		else {
			return (Integer) this.status;
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		ResponseEntity<?> otherEntity = (ResponseEntity<?>) other;
		return ObjectUtils.nullSafeEquals(this.status, otherEntity.status);
	}

	@Override
	public int hashCode() {
		return (super.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.status));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("<");
		builder.append(this.status.toString());
		if (this.status instanceof HttpStatus) {
			builder.append(' ');
			builder.append(((HttpStatus) this.status).getReasonPhrase());
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
	public static BodyBuilder status(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
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
	 * A shortcut for creating a {@code ResponseEntity} with the given body and
	 * the status set to {@linkplain HttpStatus#OK OK}.
	 * @return the created {@code ResponseEntity}
	 * @since 4.1
	 */
	public static <T> ResponseEntity<T> ok(T body) {
		BodyBuilder builder = ok();
		return builder.body(body);
	}

	/**
	 * Create a new builder with a {@linkplain HttpStatus#CREATED CREATED} status
	 * and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 * @since 4.1
	 */
	public static BodyBuilder created(URI location) {
		BodyBuilder builder = status(HttpStatus.CREATED);
		return builder.location(location);
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
	 * Defines a builder that adds headers to the response entity.
	 * @param <B> the builder subclass
	 * @since 4.1
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
		B eTag(String etag);

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
		 * Configure one or more request header names (e.g. "Accept-Language") to
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

		private final Object statusCode;

		private final HttpHeaders headers = new HttpHeaders();

		public DefaultBuilder(Object statusCode) {
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
		public BodyBuilder eTag(String etag) {
			if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
				etag = "\"" + etag;
			}
			if (!etag.endsWith("\"")) {
				etag = etag + "\"";
			}
			this.headers.setETag(etag);
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
			String ccValue = cacheControl.getHeaderValue();
			if (ccValue != null) {
				this.headers.setCacheControl(cacheControl.getHeaderValue());
			}
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
