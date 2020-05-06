/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.function;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.function.Consumer;

import javax.servlet.http.Cookie;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

/**
 * Entity-specific subtype of {@link ServerResponse} that exposes entity data.
 *
 * @author Arjen Poutsma
 * @since 5.2
 * @param <T> the entity type
 */
public interface EntityResponse<T> extends ServerResponse {

	/**
	 * Return the entity that makes up this response.
	 */
	T entity();


	// Static builder methods

	/**
	 * Create a builder with the given object.
	 * @param t the object that represents the body of the response
	 * @param <T> the type of element contained in the publisher
	 * @return the created builder
	 */
	static <T> Builder<T> fromObject(T t) {
		return DefaultEntityResponseBuilder.fromObject(t);
	}

	/**
	 * Create a builder with the given object and type reference.
	 * @param t the object that represents the body of the response
	 * @param entityType the type of the entity, used to capture the generic type
	 * @param <T> the type of element contained in the publisher
	 * @return the created builder
	 */
	static <T> Builder<T> fromObject(T t, ParameterizedTypeReference<T> entityType) {
		return DefaultEntityResponseBuilder.fromObject(t, entityType);
	}


	/**
	 * Defines a builder for {@code EntityResponse}.
	 * @param <T> the entity type
	 */
	interface Builder<T> {

		/**
		 * Add the given header value(s) under the given name.
		 * @param headerName   the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder<T> header(String headerName, String... headerValues);

		/**
		 * Manipulate this response's headers with the given consumer. The
		 * headers provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
		 * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
		 * {@link HttpHeaders} methods.
		 * @param headersConsumer a function that consumes the {@code HttpHeaders}
		 * @return this builder
		 */
		Builder<T> headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Set the HTTP status.
		 * @param status the response status
		 * @return this builder
		 */
		Builder<T> status(HttpStatus status);

		/**
		 * Set the HTTP status.
		 * @param status the response status
		 * @return this builder
		 */
		Builder<T> status(int status);

		/**
		 * Add the given cookie to the response.
		 * @param cookie the cookie to add
		 * @return this builder
		 */
		Builder<T> cookie(Cookie cookie);

		/**
		 * Manipulate this response's cookies with the given consumer. The
		 * cookies provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing cookies,
		 * {@linkplain MultiValueMap#remove(Object) remove} cookies, or use any of the other
		 * {@link MultiValueMap} methods.
		 * @param cookiesConsumer a function that consumes the cookies
		 * @return this builder
		 */
		Builder<T> cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

		/**
		 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
		 * by the {@code Allow} header.
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		Builder<T> allow(HttpMethod... allowedMethods);

		/**
		 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
		 * by the {@code Allow} header.
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		Builder<T> allow(Set<HttpMethod> allowedMethods);

		/**
		 * Set the entity tag of the body, as specified by the {@code ETag} header.
		 * @param etag the new entity tag
		 * @return this builder
		 * @see HttpHeaders#setETag(String)
		 */
		Builder<T> eTag(String etag);

		/**
		 * Set the time the resource was last changed, as specified by the
		 * {@code Last-Modified} header.
		 * <p>The date should be specified as the number of milliseconds since
		 * January 1, 1970 GMT.
		 * @param lastModified the last modified date
		 * @return this builder
		 * @see HttpHeaders#setLastModified(long)
		 */
		Builder<T> lastModified(ZonedDateTime lastModified);

		/**
		 * Set the time the resource was last changed, as specified by the
		 * {@code Last-Modified} header.
		 * <p>The date should be specified as the number of milliseconds since
		 * January 1, 1970 GMT.
		 * @param lastModified the last modified date
		 * @return this builder
		 * @since 5.1.4
		 * @see HttpHeaders#setLastModified(long)
		 */
		Builder<T> lastModified(Instant lastModified);
		/**
		 * Set the location of a resource, as specified by the {@code Location} header.
		 * @param location the location
		 * @return this builder
		 * @see HttpHeaders#setLocation(URI)
		 */
		Builder<T> location(URI location);

		/**
		 * Set the caching directives for the resource, as specified by the HTTP 1.1
		 * {@code Cache-Control} header.
		 * <p>A {@code CacheControl} instance can be built like
		 * {@code CacheControl.maxAge(3600).cachePublic().noTransform()}.
		 * @param cacheControl a builder for cache-related HTTP response headers
		 * @return this builder
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		Builder<T> cacheControl(CacheControl cacheControl);

		/**
		 * Configure one or more request header names (e.g. "Accept-Language") to
		 * add to the "Vary" response header to inform clients that the response is
		 * subject to content negotiation and variances based on the value of the
		 * given request headers. The configured request header names are added only
		 * if not already present in the response "Vary" header.
		 * @param requestHeaders request header names
		 * @return this builder
		 */
		Builder<T> varyBy(String... requestHeaders);

		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 * @param contentLength the content length
		 * @return this builder
		 * @see HttpHeaders#setContentLength(long)
		 */
		Builder<T> contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified by the
		 * {@code Content-Type} header.
		 * @param contentType the content type
		 * @return this builder
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		Builder<T> contentType(MediaType contentType);

		/**
		 * Build the response.
		 * @return the built response
		 */
		EntityResponse<T> build();
	}

}
