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

package org.springframework.web.reactive.function.server;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Set;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.AbstractJackson2Codec;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * Entity-specific subtype of {@link ServerResponse} that exposes entity data.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface EntityResponse<T> extends ServerResponse {

	/**
	 * Return the entity that makes up this response.
	 */
	T entity();

	/**
	 * Return the {@code BodyInserter} that writes the entity to the output stream.
	 */
	BodyInserter<T, ? super ServerHttpResponse> inserter();

	// Static builder methods

	/**
	 * Create a builder with the given object.
	 *
	 * @param t the object that represents the body of the response
	 * @param <T> the type of the elements contained in the publisher
	 * @return the created builder
	 */
	static <T> Builder<T> fromObject(T t) {
		return new DefaultEntityResponseBuilder<>(t, BodyInserters.fromObject(t));
	}

	/**
	 * Create a builder with the given publisher.
	 *
	 * @param publisher the publisher that represents the body of the response
	 * @param elementClass the class of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <P> the type of the {@code Publisher}
	 * @return the created builder
	 */
	static <T, P extends Publisher<T>> Builder<P> fromPublisher(P publisher, Class<T> elementClass) {
		return new DefaultEntityResponseBuilder<>(publisher,
				BodyInserters.fromPublisher(publisher, elementClass));
	}

	/**
	 * Create a builder with the given publisher.
	 *
	 * @param publisher the publisher that represents the body of the response
	 * @param elementType the type of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <P> the type of the {@code Publisher}
	 * @return the created builder
	 */
	static <T, P extends Publisher<T>> Builder<P> fromPublisher(P publisher, ResolvableType elementType) {
		return new DefaultEntityResponseBuilder<>(publisher,
				BodyInserters.fromPublisher(publisher, elementType));
	}

	/**
	 * Defines a builder for {@code EntityResponse}.
	 */
	interface Builder<T> {

		/**
		 * Add the given header value(s) under the given name.
		 *
		 * @param headerName   the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder<T> header(String headerName, String... headerValues);

		/**
		 * Copy the given headers into the entity's headers map.
		 *
		 * @param headers the existing HttpHeaders to copy from
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder<T> headers(HttpHeaders headers);

		/**
		 * Set the status.
		 * @param status the response status
		 * @return this builder
		 */
		Builder<T> status(HttpStatus status);

		/**
		 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
		 * by the {@code Allow} header.
		 *
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		Builder<T> allow(HttpMethod... allowedMethods);

		/**
		 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
		 * by the {@code Allow} header.
		 *
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		Builder<T> allow(Set<HttpMethod> allowedMethods);

		/**
		 * Set the entity tag of the body, as specified by the {@code ETag} header.
		 *
		 * @param eTag the new entity tag
		 * @return this builder
		 * @see HttpHeaders#setETag(String)
		 */
		Builder<T> eTag(String eTag);

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
		Builder<T> lastModified(ZonedDateTime lastModified);

		/**
		 * Set the location of a resource, as specified by the {@code Location} header.
		 *
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
		 *
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
		 *
		 * @param requestHeaders request header names
		 * @return this builder
		 */
		Builder<T> varyBy(String... requestHeaders);

		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 *
		 * @param contentLength the content length
		 * @return this builder
		 * @see HttpHeaders#setContentLength(long)
		 */
		Builder<T> contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified by the
		 * {@code Content-Type} header.
		 *
		 * @param contentType the content type
		 * @return this builder
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		Builder<T> contentType(MediaType contentType);

		/**
		 * Add a serialization hint like {@link AbstractJackson2Codec#JSON_VIEW_HINT} to
		 * customize how the body will be serialized.
		 * @param key the hint key
		 * @param value the hint value
		 */
		Builder<T> hint(String key, Object value);

		/**
		 * Build the response.
		 *
		 * @return the built response
		 */
		Mono<EntityResponse<T>> build();

	}

}
