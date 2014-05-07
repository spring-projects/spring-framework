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
import java.util.Set;

/**
 * A builder for {@link ResponseEntity} objects. Enforces common HTTP response practices
 * through a fluent API.
 *
 * <p>This class is typically used in @Controller methods through a static import.
 * For instance:
 * <pre class="code">
 * import static org.springframework.http.ResponseEntityBuilder.*;
 *
 * &#064;Controller
 * public class MyController {
 *
 *     &#064;RequestMapping(value="/entity", method=RequestMethod.GET)
 *     public ResponseEntity&lt;MyEntity&gt; myBean() {
 *         MyEntity entity = ...
 *         long lastModifiedDate = ...
 *         ResponseEntity&lt;MyEntity&gt; responseEntity = status(HttpStatus.OK)
 *             .header("Last-Modified", lastModifiedDate).body(entity);
 *         return responseEntity;
 *     }
 * }</pre>
 *
 * Or, using some of the convenience methods:
 *
 * <pre class="code">
 * ok().lastModified(lastModifiedDate).body(entity);
 * </pre>
 *
 * @author Arjen Poutsma
 * @since 4.1
 * @see ResponseEntity
 */
public class ResponseEntityBuilder {

	private ResponseEntityBuilder() {
	}

	/**
	 * Creates a new {@code ResponseEntityBuilder} with the given status.
	 * @param status the response status
	 * @return the new response entity builder
	 */
	public static ResponseBodyBuilder status(HttpStatus status) {
		return new DefaultResponseBuilder(status);
	}

	/**
	 * Creates a new {@code ResponseEntityBuilder} with the given status.
	 * @param status the response status
	 * @return the new response entity builder
	 */
	public static ResponseBodyBuilder status(int status) {
		return status(HttpStatus.valueOf(status));
	}

	/**
	 * Creates a new {@code ResponseEntityBuilder} with the status set to
	 * {@linkplain HttpStatus#OK OK}.
	 * @return the new response entity builder
	 */
	public static ResponseBodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * Creates a new {@code ResponseEntity} with the given body and the status set to
	 * {@linkplain HttpStatus#OK OK}.
	 * @return the new response entity
	 */
	public static ResponseHeadersBuilder ok(Object body) {
		ResponseBodyBuilder builder = ok();
		builder.body(body);
		return builder;
	}

	/**
	 * Creates a new {@code ResponseEntityBuilder} with a
	 * {@linkplain HttpStatus#CREATED CREATED} status and a location header set to the
	 * given URI.
	 * @param location the location URI
	 * @return the new response entity builder
	 */
	public static ResponseBodyBuilder created(URI location) {
		ResponseBodyBuilder builder = status(HttpStatus.CREATED);
		return builder.location(location);
	}

	/**
	 * Creates a new {@code ResponseEntityBuilder} with an
	 * {@link HttpStatus#ACCEPTED ACCEPTED} status.
	 * @return the new response entity builder
	 */
	public static ResponseBodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * Creates a new {@code ResponseEntityBuilder} with an
	 * {@link HttpStatus#NO_CONTENT NO_CONTENT} status.
	 * @return the new response entity builder
	 */
	public static ResponseHeadersBuilder noContent() {
		return status(HttpStatus.NO_CONTENT);
	}


	/**
	 * Defines a builder that adds headers to the response entity.
	 * @param <B> the builder subclass
	 */
	public interface ResponseHeadersBuilder<B extends ResponseHeadersBuilder<B>> {

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
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(Set<HttpMethod> allowedMethods);

		/**
		 * Sets the entity tag of the body, as specified by the {@code ETag} header.
		 * @param eTag the new entity tag
		 * @see HttpHeaders#setETag(String)
		 */
		B eTag(String eTag);

		/**
		 * Sets the time the resource was last changed, as specified by the
		 * {@code Last-Modified} header.
		 * <p>The date should be specified as the number of milliseconds since January 1, 1970 GMT.
		 * @param lastModified the last modified date
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(long lastModified);

		/**
		 * Set the location of a resource, as specified by the {@code Location} header.
		 * @param location the location
		 * @see HttpHeaders#setLocation(URI)
		 */
		B location(URI location);

		/**
		 * Builds the response entity.
		 * @return the response entity
		 * @see ResponseBodyBuilder#body(Object)
		 */
		ResponseEntity<?> build();

	}


	/**
	 * Defines a builder that adds a body to the response entity.
	 */
	public interface ResponseBodyBuilder extends ResponseHeadersBuilder<ResponseBodyBuilder> {

		/**
		 * Set the length of the body in bytes, as specified by the {@code Content-Length}
		 * header.
		 * @param contentLength the content length
		 * @see HttpHeaders#setContentLength(long)
		 */
		ResponseBodyBuilder contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified by the
		 * {@code Content-Type} header.
		 * @param contentType the content type
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		ResponseBodyBuilder contentType(MediaType contentType);

		/**
		 * Sets the body of the response entity and returns it.
		 * @param body the body of the response entity
		 * @param <T> the type of the body
		 * @return the built response entity
		 */
		<T> ResponseEntity<T> body(T body);

	}

	private static class DefaultResponseBuilder implements ResponseBodyBuilder {

		private final HttpStatus status;

		private final HttpHeaders headers = new HttpHeaders();

		private Object body = null;

		public DefaultResponseBuilder(HttpStatus status) {
			this.status = status;
		}

		@Override
		public ResponseBodyBuilder header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				headers.add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public ResponseBodyBuilder allow(Set<HttpMethod> allowedMethods) {
			headers.setAllow(allowedMethods);
			return this;
		}

		@Override
		public ResponseBodyBuilder contentLength(long contentLength) {
			headers.setContentLength(contentLength);
			return this;
		}

		@Override
		public ResponseBodyBuilder contentType(MediaType contentType) {
			headers.setContentType(contentType);
			return this;
		}

		@Override
		public ResponseBodyBuilder eTag(String eTag) {
			headers.setETag(eTag);
			return this;
		}

		@Override
		public ResponseBodyBuilder lastModified(long date) {
			headers.setLastModified(date);
			return this;
		}

		@Override
		public ResponseBodyBuilder location(URI location) {
			headers.setLocation(location);
			return this;
		}


		@Override
		@SuppressWarnings("unchecked")
		public ResponseEntity<?> build() {
			return new ResponseEntity(body, headers, status);
		}

		@Override
		public <T> ResponseEntity<T> body(T body) {
			this.body = body;
			return new ResponseEntity<T>(body, headers, status);
		}

	}

}
