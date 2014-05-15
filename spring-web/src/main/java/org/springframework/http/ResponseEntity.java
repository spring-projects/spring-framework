/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.HashSet;
import java.util.Set;

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
 * Or, by using the static convenience methods:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   return ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body("Hello World");
 * }
 * </pre>
 *
 * @author Arjen Poutsma
 * @since 3.0.2
 * @see #getStatusCode()
 */
public class ResponseEntity<T> extends HttpEntity<T> {

	private final HttpStatus statusCode;


	/**
	 * Create a new {@code ResponseEntity} with the given status code, and no body nor headers.
	 * @param statusCode the status code
	 */
	public ResponseEntity(HttpStatus statusCode) {
		super();
		this.statusCode = statusCode;
	}

	/**
	 * Create a new {@code ResponseEntity} with the given body and status code, and no headers.
	 * @param body the entity body
	 * @param statusCode the status code
	 */
	public ResponseEntity(T body, HttpStatus statusCode) {
		super(body);
		this.statusCode = statusCode;
	}

	/**
	 * Create a new {@code HttpEntity} with the given headers and status code, and no body.
	 * @param headers the entity headers
	 * @param statusCode the status code
	 */
	public ResponseEntity(MultiValueMap<String, String> headers, HttpStatus statusCode) {
		super(headers);
		this.statusCode = statusCode;
	}

	/**
	 * Create a new {@code HttpEntity} with the given body, headers, and status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param statusCode the status code
	 */
	public ResponseEntity(T body, MultiValueMap<String, String> headers, HttpStatus statusCode) {
		super(body, headers);
		this.statusCode = statusCode;
	}


	/**
	 * Return the HTTP status code of the response.
	 * @return the HTTP status as an HttpStatus enum value
	 */
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ResponseEntity)) {
			return false;
		}
		ResponseEntity<?> otherEntity = (ResponseEntity<?>) other;
		return (ObjectUtils.nullSafeEquals(this.statusCode, otherEntity.statusCode) && super.equals(other));
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.statusCode);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("<");
		builder.append(this.statusCode.toString());
		builder.append(' ');
		builder.append(this.statusCode.getReasonPhrase());
		builder.append(',');
		T body = getBody();
		HttpHeaders headers = getHeaders();
		if (body != null) {
			builder.append(body);
			if (headers != null) {
				builder.append(',');
			}
		}
		if (headers != null) {
			builder.append(headers);
		}
		builder.append('>');
		return builder.toString();
	}

	// Static builder methods

	/**
	 * Creates a new response entity builder with the given status.
	 * @param status the response status
	 * @return the new response entity builder
	 */
	public static BodyBuilder status(HttpStatus status) {
		return new DefaultBuilder(status);
	}

	/**
	 * Creates a new response entity builder with the given status.
	 * @param status the response status
	 * @return the new response entity builder
	 */
	public static BodyBuilder status(int status) {
		return status(HttpStatus.valueOf(status));
	}

	/**
	 * Creates a new response entity builder with the status set to
	 * {@linkplain HttpStatus#OK OK}.
	 * @return the new response entity builder
	 */
	public static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * Creates a new response entity with the given body and the status set to
	 * {@linkplain HttpStatus#OK OK}.
	 * @return the new response entity
	 */
	public static <T> ResponseEntity<T> ok(T body) {
		BodyBuilder builder = ok();
		return builder.body(body);
	}

	/**
	 * Creates a new response entity builder with a
	 * {@linkplain HttpStatus#CREATED CREATED} status and a location header set to the
	 * given URI.
	 * @param location the location URI
	 * @return the new response entity builder
	 */
	public static BodyBuilder created(URI location) {
		BodyBuilder builder = status(HttpStatus.CREATED);
		return builder.location(location);
	}

	/**
	 * Creates a new response entity builder with an
	 * {@link HttpStatus#ACCEPTED ACCEPTED} status.
	 * @return the new response entity builder
	 */
	public static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * Creates a new response entity builder with an
	 * {@link HttpStatus#NO_CONTENT NO_CONTENT} status.
	 * @return the new response entity builder
	 */
	public static HeadersBuilder<?> noContent() {
		return status(HttpStatus.NO_CONTENT);
	}


	/**
	 * Defines a builder that adds headers to the response entity.
	 * @param <B> the builder subclass
	 */
	public interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName  the header name
		 * @param headerValue the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified by the
		 * {@code Allow} header.
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(HttpMethod... allowedMethods);

		/**
		 * Sets the entity tag of the body, as specified by the {@code ETag} header.
		 * @param eTag the new entity tag
		 * @return this builder
		 * @see HttpHeaders#setETag(String)
		 */
		B eTag(String eTag);

		/**
		 * Sets the time the resource was last changed, as specified by the
		 * {@code Last-Modified} header.
		 * <p>The date should be specified as the number of milliseconds since January 1,
		 * 1970 GMT.
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
		 * Builds the response entity with no body.
		 * @return the response entity
		 * @see ResponseBodyBuilder#body(Object)
		 */
		ResponseEntity<Void> build();

	}


	/**
	 * Defines a builder that adds a body to the response entity.
	 */
	public interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * Set the length of the body in bytes, as specified by the {@code Content-Length}
		 * header.
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
		 * Sets the body of the response entity and returns it.
		 * @param body the body of the response entity
		 * @param <T> the type of the body
		 * @return the built response entity
		 */
		<T> ResponseEntity<T> body(T body);

	}


	private static class DefaultBuilder implements BodyBuilder {

		private final HttpStatus status;

		private final HttpHeaders headers = new HttpHeaders();


		public DefaultBuilder(HttpStatus status) {
			this.status = status;
		}

		@Override
		public BodyBuilder header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				headers.add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public BodyBuilder allow(HttpMethod... allowedMethods) {
			headers.setAllow(new HashSet<HttpMethod>(Arrays.asList(allowedMethods)));
			return this;
		}

		@Override
		public BodyBuilder contentLength(long contentLength) {
			headers.setContentLength(contentLength);
			return this;
		}

		@Override
		public BodyBuilder contentType(MediaType contentType) {
			headers.setContentType(contentType);
			return this;
		}

		@Override
		public BodyBuilder eTag(String eTag) {
			headers.setETag(eTag);
			return this;
		}

		@Override
		public BodyBuilder lastModified(long date) {
			headers.setLastModified(date);
			return this;
		}

		@Override
		public BodyBuilder location(URI location) {
			headers.setLocation(location);
			return this;
		}


		@Override
		public ResponseEntity<Void> build() {
			return new ResponseEntity<Void>(null, headers, status);
		}

		@Override
		public <T> ResponseEntity<T> body(T body) {
			return new ResponseEntity<T>(body, headers, status);
		}

	}


}
