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
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

/**
 * The main class for performing Web requests.
 *
 * <pre class="code">
 *
 * // Create ExchangeFunction (application-wide)
 *
 * ClientHttpConnector connector = new ReactorClientHttpConnector();
 * ExchangeFunction exchangeFunction = ExchangeFunctions.create(connector);
 *
 * // Create WebClient (per base URI)
 *
 * String baseUri = "http://abc.com";
 * UriBuilderFactory factory = new DefaultUriBuilderFactory(baseUri);
 * WebClient operations = WebClient.create(exchangeFunction, factory);
 *
 * // Perform requests...
 *
 * Mono<String> result = operations.get()
 *     .uri("/foo")
 *     .exchange()
 *     .then(response -> response.bodyToMono(String.class));
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface WebClient {

	/**
	 * Prepare an HTTP GET request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec get();

	/**
	 * Prepare an HTTP HEAD request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec head();

	/**
	 * Prepare an HTTP POST request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec post();

	/**
	 * Prepare an HTTP PUT request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec put();

	/**
	 * Prepare an HTTP PATCH request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec patch();

	/**
	 * Prepare an HTTP DELETE request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec delete();

	/**
	 * Prepare an HTTP OPTIONS request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec options();


	/**
	 * Filter the client with the given {@code ExchangeFilterFunction}.
	 * @param filterFunction the filter to apply to this client
	 * @return the filtered client
	 * @see ExchangeFilterFunction#apply(ExchangeFunction)
	 */
	WebClient filter(ExchangeFilterFunction filterFunction);


	// Static, factory methods

	/**
	 * Shortcut for:
	 * <pre class="code">
	 * WebClient client = builder(baseUrl).build();
	 * </pre>
	 * @param baseUrl the base URI for all requests
	 */
	static WebClient create(String baseUrl) {
		return new DefaultWebClientBuilder(baseUrl).build();
	}

	/**
	 * Obtain a {@code WebClient} builder with a base URI to be used as the
	 * base for expanding URI templates during exchanges. The given String
	 * is used to create an instance of {@link DefaultUriBuilderFactory} whose
	 * {@link DefaultUriBuilderFactory#DefaultUriBuilderFactory(String)
	 * constructor} provides more details on how the base URI is applied.
	 * @param baseUrl the base URI for all requests
	 */
	static WebClient.Builder builder(String baseUrl) {
		return new DefaultWebClientBuilder(baseUrl);
	}

	/**
	 * Obtain a {@code WebClient} builder with the {@link UriBuilderFactory}
	 * to use for expanding URI templates during exchanges.
	 * @param uriBuilderFactory the factory to use
	 */
	static WebClient.Builder builder(UriBuilderFactory uriBuilderFactory) {
		return new DefaultWebClientBuilder(uriBuilderFactory);
	}


	/**
	 * A mutable builder for a {@link WebClient}.
	 */
	interface Builder {

		/**
		 * Configure the {@link ClientHttpConnector} to use.
		 * <p>By default an instance of
		 * {@link org.springframework.http.client.reactive.ReactorClientHttpConnector
		 * ReactorClientHttpConnector} is created if this is not set. However a
		 * shared instance may be passed instead, e.g. for use with multiple
		 * {@code WebClient}'s targeting different base URIs.
		 * @param connector the connector to use
		 */
		Builder clientConnector(ClientHttpConnector connector);

		/**
		 * Configure the {@link ExchangeStrategies} to use.
		 * <p>By default {@link ExchangeStrategies#withDefaults()} is used.
		 * @param strategies the strategies to use
		 */
		Builder exchangeStrategies(ExchangeStrategies strategies);

		/**
		 * Configure directly an {@link ExchangeFunction} instead of separately
		 * providing a {@link ClientHttpConnector} and/or
		 * {@link ExchangeStrategies}.
		 * @param exchangeFunction the exchange function to use
		 */
		Builder exchangeFunction(ExchangeFunction exchangeFunction);

		/**
		 * Add the given header to all requests that haven't added it.
		 * @param headerName the header name
		 * @param headerValues the header values
		 */
		Builder defaultHeader(String headerName, String... headerValues);

		/**
		 * Add the given header to all requests that haven't added it.
		 * @param cookieName the cookie name
		 * @param cookieValues the cookie values
		 */
		Builder defaultCookie(String cookieName, String... cookieValues);

		/**
		 * Builder the {@link WebClient} instance.
		 */
		WebClient build();

	}


	/**
	 * Contract for specifying the URI for a request.
	 */
	interface UriSpec {

		/**
		 * Specify the URI using an absolute, fully constructed {@link URI}.
		 */
		HeaderSpec uri(URI uri);

		/**
		 * Specify the URI for the request using a URI template and URI variables.
		 * If a {@link UriBuilderFactory} was configured for the client (e.g.
		 * with a base URI) it will be used to expand the URI template.
		 * @see #builder(String)
		 */
		HeaderSpec uri(String uri, Object... uriVariables);

		/**
		 * Build the URI for the request using the {@link UriBuilderFactory}
		 * configured for this client.
		 * @see #builder(String)
		 */
		HeaderSpec uri(Function<UriBuilderFactory, URI> uriFunction);

	}

	/**
	 * Contract for specifying request headers leading up to the exchange.
	 */
	interface HeaderSpec {

		/**
		 * Set the list of acceptable {@linkplain MediaType media types}, as
		 * specified by the {@code Accept} header.
		 * @param acceptableMediaTypes the acceptable media types
		 * @return this builder
		 */
		HeaderSpec accept(MediaType... acceptableMediaTypes);

		/**
		 * Set the list of acceptable {@linkplain Charset charsets}, as specified
		 * by the {@code Accept-Charset} header.
		 * @param acceptableCharsets the acceptable charsets
		 * @return this builder
		 */
		HeaderSpec acceptCharset(Charset... acceptableCharsets);

		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 * @param contentLength the content length
		 * @return this builder
		 * @see HttpHeaders#setContentLength(long)
		 */
		HeaderSpec contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified
		 * by the {@code Content-Type} header.
		 * @param contentType the content type
		 * @return this builder
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		HeaderSpec contentType(MediaType contentType);

		/**
		 * Add a cookie with the given name and value.
		 * @param name the cookie name
		 * @param value the cookie value
		 * @return this builder
		 */
		HeaderSpec cookie(String name, String value);

		/**
		 * Copy the given cookies into the entity's cookies map.
		 *
		 * @param cookies the existing cookies to copy from
		 * @return this builder
		 */
		HeaderSpec cookies(MultiValueMap<String, String> cookies);

		/**
		 * Set the value of the {@code If-Modified-Since} header.
		 * <p>The date should be specified as the number of milliseconds since
		 * January 1, 1970 GMT.
		 * @param ifModifiedSince the new value of the header
		 * @return this builder
		 */
		HeaderSpec ifModifiedSince(ZonedDateTime ifModifiedSince);

		/**
		 * Set the values of the {@code If-None-Match} header.
		 * @param ifNoneMatches the new value of the header
		 * @return this builder
		 */
		HeaderSpec ifNoneMatch(String... ifNoneMatches);

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 */
		HeaderSpec header(String headerName, String... headerValues);

		/**
		 * Copy the given headers into the entity's headers map.
		 * @param headers the existing headers to copy from
		 * @return this builder
		 */
		HeaderSpec headers(HttpHeaders headers);

		/**
		 * Perform the request without a request body.
		 * @return a {@code Mono} with the response
		 */
		Mono<ClientResponse> exchange();

		/**
		 * Set the body of the request to the given {@code BodyInserter} and
		 * perform the request.
		 * @param inserter the {@code BodyInserter} that writes to the request
		 * @param <T> the type contained in the body
		 * @return a {@code Mono} with the response
		 */
		<T> Mono<ClientResponse> exchange(BodyInserter<T, ? super ClientHttpRequest> inserter);

		/**
		 * Set the body of the request to the given {@code Publisher} and
		 * perform the request.
		 * @param publisher the {@code Publisher} to write to the request
		 * @param elementClass the class of elements contained in the publisher
		 * @param <T> the type of the elements contained in the publisher
		 * @param <S> the type of the {@code Publisher}
		 * @return a {@code Mono} with the response
		 */
		<T, S extends Publisher<T>> Mono<ClientResponse> exchange(S publisher, Class<T> elementClass);

	}

}
