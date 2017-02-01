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

package org.springframework.web.reactive.function.client;

import java.net.URI;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;

/**
 * Represents a typed, immutable, client-side HTTP request, as executed by the
 * {@link ExchangeFunction}. Instances of this interface can be created via static
 * builder methods.
 *
 * <p>Note that applications are more likely to perform requests through
 * {@link WebClient} rather than using this directly.
 *
 * @param <T> the type of the body that this request contains
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ClientRequest<T> {

	/**
	 * Return the HTTP method.
	 */
	HttpMethod method();

	/**
	 * Return the request URI.
	 */
	URI url();

	/**
	 * Return the headers of this request.
	 */
	HttpHeaders headers();

	/**
	 * Return the cookies of this request.
	 */
	MultiValueMap<String, String> cookies();

	/**
	 * Return the body inserter of this request.
	 */
	BodyInserter<T, ? super ClientHttpRequest> inserter();

	/**
	 * Writes this request to the given {@link ClientHttpRequest}.
	 *
	 * @param request the client http request to write to
	 * @param strategies the strategies to use when writing
	 * @return {@code Mono<Void>} to indicate when writing is complete
	 */
	Mono<Void> writeTo(ClientHttpRequest request, ExchangeStrategies strategies);


	// Static builder methods

	/**
	 * Create a builder with the method, URI, headers, and cookies of the given request.
	 *
	 * @param other the request to copy the method, URI, headers, and cookies from
	 * @return the created builder
	 */
	static Builder from(ClientRequest<?> other) {
		Assert.notNull(other, "'other' must not be null");
		return new DefaultClientRequestBuilder(other.method(), other.url())
				.headers(other.headers())
				.cookies(other.cookies());
	}

	/**
	 * Create a builder with the given method and url.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param url the URL
	 * @return the created builder
	 */
	static Builder method(HttpMethod method, URI url) {
		return new DefaultClientRequestBuilder(method, url);
	}


	/**
	 * Defines a builder for a request.
	 */
	interface Builder {

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * Copy the given headers into the entity's headers map.
		 *
		 * @param headers the existing HttpHeaders to copy from
		 * @return this builder
		 */
		Builder headers(HttpHeaders headers);

		/**
		 * Add a cookie with the given name and value.
		 * @param name the cookie name
		 * @param value the cookie value
		 * @return this builder
		 */
		Builder cookie(String name, String value);

		/**
		 * Copy the given cookies into the entity's cookies map.
		 *
		 * @param cookies the existing cookies to copy from
		 * @return this builder
		 */
		Builder cookies(MultiValueMap<String, String> cookies);

		/**
		 * Builds the request entity with no body.
		 * @return the request entity
		 */
		ClientRequest<Void> build();

		/**
		 * Set the body of the request to the given {@code BodyInserter} and return it.
		 * @param inserter the {@code BodyInserter} that writes to the request
		 * @param <T> the type contained in the body
		 * @return the built request
		 */
		<T> ClientRequest<T> body(BodyInserter<T, ? super ClientHttpRequest> inserter);

		/**
		 * Set the body of the request to the given {@code Publisher} and return it.
		 * @param publisher the {@code Publisher} to write to the request
		 * @param elementClass the class of elements contained in the publisher
		 * @param <T> the type of the elements contained in the publisher
		 * @param <S> the type of the {@code Publisher}
		 * @return the built request
		 */
		<T, S extends Publisher<T>> ClientRequest<S> body(S publisher, Class<T> elementClass);

	}

}
