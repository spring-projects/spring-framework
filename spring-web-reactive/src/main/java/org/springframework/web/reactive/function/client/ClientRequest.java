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

package org.springframework.web.reactive.function.client;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.BodyInserter;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.springframework.web.util.UriTemplateHandler;

/**
 * Represents a typed, immutable, client-side HTTP request, as executed by the {@link WebClient}.
 * Instances of this interface are created via static builder methods:
 * {@link #method(HttpMethod, String, Object...)}, {@link #GET(String, Object...)}, etc.
 *
 * @param <T> the type of the body that this request contains
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ClientRequest<T> {

	// Instance methods

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
	Mono<Void> writeTo(ClientHttpRequest request, WebClientStrategies strategies);

	// Static builder methods

	/**
	 * Create a builder with the method, URI, headers, and cookies of the given request.
	 *
	 * @param other the request to copy the method, URI, headers, and cookies from
	 * @return the created builder
	 */
	static BodyBuilder from(ClientRequest<?> other) {
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
	static BodyBuilder method(HttpMethod method, URI url) {
		return new DefaultClientRequestBuilder(method, url);
	}

	/**
	 * Create a builder with the given method and url template.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param urlTemplate the URL template
	 * @param uriVariables optional variables to expand the template
	 * @return the created builder
	 */
	static BodyBuilder method(HttpMethod method, String urlTemplate, Object... uriVariables) {
		UriTemplateHandler templateHandler = new DefaultUriTemplateHandler();
		URI url = templateHandler.expand(urlTemplate, uriVariables);
		return new DefaultClientRequestBuilder(method, url);
	}

	/**
	 * Create an HTTP GET builder with the given url template.
	 * @param urlTemplate the URL template
	 * @param uriVariables optional variables to expand the template
	 * @return the created builder
	 */
	static HeadersBuilder<?> GET(String urlTemplate, Object... uriVariables) {
		return method(HttpMethod.GET, urlTemplate, uriVariables);
	}

	/**
	 * Create an HTTP HEAD builder with the given url template.
	 * @param urlTemplate the URL template
	 * @param uriVariables optional variables to expand the template
	 * @return the created builder
	 */
	static HeadersBuilder<?> HEAD(String urlTemplate, Object... uriVariables) {
		return method(HttpMethod.HEAD, urlTemplate, uriVariables);
	}

	/**
	 * Create an HTTP POST builder with the given url template.
	 * @param urlTemplate the URL template
	 * @param uriVariables optional variables to expand the template
	 * @return the created builder
	 */
	static BodyBuilder POST(String urlTemplate, Object... uriVariables) {
		return method(HttpMethod.POST, urlTemplate, uriVariables);
	}

	/**
	 * Create an HTTP PUT builder with the given url template.
	 * @param urlTemplate the URL template
	 * @param uriVariables optional variables to expand the template
	 * @return the created builder
	 */
	static BodyBuilder PUT(String urlTemplate, Object... uriVariables) {
		return method(HttpMethod.PUT, urlTemplate, uriVariables);
	}

	/**
	 * Create an HTTP PATCH builder with the given url template.
	 * @param urlTemplate the URL template
	 * @param uriVariables optional variables to expand the template
	 * @return the created builder
	 */
	static BodyBuilder PATCH(String urlTemplate, Object... uriVariables) {
		return method(HttpMethod.PATCH, urlTemplate, uriVariables);
	}

	/**
	 * Create an HTTP DELETE builder with the given url template.
	 * @param urlTemplate the URL template
	 * @param uriVariables optional variables to expand the template
	 * @return the created builder
	 */
	static HeadersBuilder<?> DELETE(String urlTemplate, Object... uriVariables) {
		return method(HttpMethod.DELETE, urlTemplate, uriVariables);
	}

	/**
	 * Creates an HTTP OPTIONS builder with the given url template.
	 * @param urlTemplate the URL template
	 * @param uriVariables optional variables to expand the template
	 * @return the created builder
	 */
	static HeadersBuilder<?> OPTIONS(String urlTemplate, Object... uriVariables) {
		return method(HttpMethod.OPTIONS, urlTemplate, uriVariables);
	}


	/**
	 * Defines a builder that adds headers to the request.
	 *
	 * @param <B> the builder subclass
	 */
	interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName  the header name
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
		 */
		B headers(HttpHeaders headers);

		/**
		 * Set the list of acceptable {@linkplain MediaType media types}, as
		 * specified by the {@code Accept} header.
		 * @param acceptableMediaTypes the acceptable media types
		 * @return this builder
		 */
		B accept(MediaType... acceptableMediaTypes);

		/**
		 * Set the list of acceptable {@linkplain Charset charsets}, as specified
		 * by the {@code Accept-Charset} header.
		 * @param acceptableCharsets the acceptable charsets
		 * @return this builder
		 */
		B acceptCharset(Charset... acceptableCharsets);

		/**
		 * Set the value of the {@code If-Modified-Since} header.
		 * <p>The date should be specified as the number of milliseconds since
		 * January 1, 1970 GMT.
		 * @param ifModifiedSince the new value of the header
		 * @return this builder
		 */
		B ifModifiedSince(ZonedDateTime ifModifiedSince);

		/**
		 * Set the values of the {@code If-None-Match} header.
		 * @param ifNoneMatches the new value of the header
		 * @return this builder
		 */
		B ifNoneMatch(String... ifNoneMatches);

		/**
		 * Add a cookie with the given name and value.
		 * @param name the cookie name
		 * @param value the cookie value
		 * @return this builder
		 */
		B cookie(String name, String value);

		/**
		 * Copy the given cookies into the entity's cookies map.
		 *
		 * @param cookies the existing cookies to copy from
		 * @return this builder
		 */
		B cookies(MultiValueMap<String, String> cookies);

		/**
		 * Builds the request entity with no body.
		 * @return the request entity
		 */
		ClientRequest<Void> build();
	}


	/**
	 * Defines a builder that adds a body to the request entity.
	 */
	interface BodyBuilder extends HeadersBuilder<BodyBuilder> {


		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 * @param contentLength the content length
		 * @return this builder
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified
		 * by the {@code Content-Type} header.
		 * @param contentType the content type
		 * @return this builder
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

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
