/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Set;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * Represents a typed HTTP response, as returned by a {@linkplain HandlerFunction handler function} or
 * {@linkplain FilterFunction filter function}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @param <T> the type of the body that this response contains
 */
public interface Response<T> {

	// Static builder methods

	/**
	 * Create a builder with the status code and headers of the given response.
	 *
	 * @param other the response to copy the status and headers from
	 * @return the created builder
	 */
	static BodyBuilder from(Response<?> other) {
		Assert.notNull(other, "'other' must not be null");
		DefaultResponseBuilder builder = new DefaultResponseBuilder(other.statusCode().value());
		return builder.headers(other.headers());
	}

	/**
	 * Create a builder with the given status.
	 *
	 * @param status the response status
	 * @return the created builder
	 */
	static BodyBuilder status(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		return new DefaultResponseBuilder(status.value());
	}

	/**
	 * Create a builder with the given status.
	 *
	 * @param status the response status
	 * @return the created builder
	 */
	static BodyBuilder status(int status) {
		return new DefaultResponseBuilder(status);
	}

	/**
	 * Create a builder with the status set to {@linkplain HttpStatus#OK OK}.
	 *
	 * @return the created builder
	 */
	static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * Create a new builder with a {@linkplain HttpStatus#CREATED CREATED} status
	 * and a location header set to the given URI.
	 *
	 * @param location the location URI
	 * @return the created builder
	 */
	static BodyBuilder created(URI location) {
		BodyBuilder builder = status(HttpStatus.CREATED);
		return builder.location(location);
	}

	/**
	 * Create a builder with an {@linkplain HttpStatus#ACCEPTED ACCEPTED} status.
	 *
	 * @return the created builder
	 */
	static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#NO_CONTENT NO_CONTENT} status.
	 *
	 * @return the created builder
	 */
	static HeadersBuilder<?> noContent() {
		return status(HttpStatus.NO_CONTENT);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#BAD_REQUEST BAD_REQUEST} status.
	 *
	 * @return the created builder
	 */
	static BodyBuilder badRequest() {
		return status(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#NOT_FOUND NOT_FOUND} status.
	 *
	 * @return the created builder
	 */
	static HeadersBuilder<?> notFound() {
		return status(HttpStatus.NOT_FOUND);
	}

	/**
	 * Create a builder with an
	 * {@linkplain HttpStatus#UNPROCESSABLE_ENTITY UNPROCESSABLE_ENTITY} status.
	 *
	 * @return the created builder
	 */
	static BodyBuilder unprocessableEntity() {
		return status(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	// Instance methods

	/**
	 * Return the status code of this response.
	 */
	HttpStatus statusCode();

	/**
	 * Return the headers of this response.
	 */
	HttpHeaders headers();

	/**
	 * Return the body of this response.
	 */
	T body();

	/**
	 * Writes this response to the given web exchange.
	 *
	 * @param exchange the web exchange to write to
	 * @return {@code Mono<Void>} to indicate when request handling is complete
	 */
	Mono<Void> writeTo(ServerWebExchange exchange);

	/**
	 * Defines a builder that adds headers to the response entity.
	 *
	 * @param <B> the builder subclass
	 */
	interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * Add the given header value(s) under the given name.
		 *
		 * @param headerName   the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * Copy the given headers into the entity's headers map.
		 *
		 * @param headers the existing HttpHeaders to copy from
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		B headers(HttpHeaders headers);

		/**
		 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
		 * by the {@code Allow} header.
		 *
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(HttpMethod... allowedMethods);

		/**
		 * Set the entity tag of the body, as specified by the {@code ETag} header.
		 *
		 * @param eTag the new entity tag
		 * @return this builder
		 * @see HttpHeaders#setETag(String)
		 */
		B eTag(String eTag);

		/**
		 * Set the time the resource was last changed, as specified by the
		 * {@code Last-Modified} header.
		 * <p>The date should be specified as the number of milliseconds since
		 * January 1, 1970 GMT.
		 *
		 * @param lastModified the last modified date
		 * @return this builder
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(ZonedDateTime lastModified);

		/**
		 * Set the location of a resource, as specified by the {@code Location} header.
		 *
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
		 *
		 * @param cacheControl a builder for cache-related HTTP response headers
		 * @return this builder
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		B cacheControl(CacheControl cacheControl);

		/**
		 * Configure one or more request header names (e.g. "Accept-Language") to
		 * add to the "Vary" response header to inform clients that the response is
		 * subject to content negotiation and variances based on the value of the
		 * given request headers. The configured request header names are added only
		 * if not already present in the response "Vary" header.
		 *
		 * @param requestHeaders request header names
		 * @return this builder
		 */
		B varyBy(String... requestHeaders);

		/**
		 * Build the response entity with no body.
		 *
		 * @return the response entity
		 */
		Response<Void> build();
	}

	/**
	 * Defines a builder that adds a body to the response entity.
	 */
	interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 *
		 * @param contentLength the content length
		 * @return this builder
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified by the
		 * {@code Content-Type} header.
		 *
		 * @param contentType the content type
		 * @return this builder
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * Set the body of the response to the given object and return it.
		 *
		 * @param body the body of the response entity
		 * @return the built response
		 */
		<T> Response<T> body(T body);

		/**
		 * Set the body of the response to the given {@link Publisher} and return it.
		 * @param publisher the publisher to stream to the response body
		 * @param elementClass the class of elements contained in the publisher
		 * @param <T> the type of the elements contained in the publisher
		 * @return the built response
		 */
		<T, S extends Publisher<T>> Response<S> stream(S publisher, Class<T> elementClass);

		/**
		 * Set the body of the response to the given {@link Resource} and return it.
		 * If the resource can be resolved to a {@linkplain Resource#getFile() file}, it will be copied using
		 * <a href="https://en.wikipedia.org/wiki/Zero-copy">zero-copy</a>
		 *
		 * @param resource the resource to write to the response
		 * @return the built response
		 */
		Response<Resource> resource(Resource resource);

		/**
		 * Set the body of the response to the given {@link ServerSentEvent} publisher and return it.
		 * @param eventsPublisher the {@link ServerSentEvent} publisher to stream to the response body
		 * @param <T> the type of the elements contained in the {@link ServerSentEvent}
		 * @return the built response
		 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
		 */
		<T> Response<Publisher<ServerSentEvent<T>>> sse(Publisher<ServerSentEvent<T>> eventsPublisher);

		/**
		 * Set the body of the response to the given Server-Sent Event {@link Publisher} and return it.
		 * @param eventsPublisher the publisher to stream to the response body as Server-Sent Events
		 * @param eventClass the class of event contained in the publisher
		 * @param <T> the type of the elements contained in the publisher
		 * @return the built response
		 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
		 */
		<T> Response<Publisher<T>> sse(Publisher<T> eventsPublisher, Class<T> eventClass);
	}

}
