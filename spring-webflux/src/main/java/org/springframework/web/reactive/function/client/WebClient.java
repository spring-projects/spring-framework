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
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * The main entry point for initiating web requests on the client side.
 *
 * <pre class="code">
 * // Initialize the client
 * WebClient client = WebClient.create("http://abc.com");
 *
 * // Perform requests...
 * Mono&#060;String&#062; result = client.get()
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
	UriSpec<RequestHeadersSpec<?>> get();

	/**
	 * Prepare an HTTP HEAD request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec<RequestHeadersSpec<?>> head();

	/**
	 * Prepare an HTTP POST request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec<RequestBodySpec> post();

	/**
	 * Prepare an HTTP PUT request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec<RequestBodySpec> put();

	/**
	 * Prepare an HTTP PATCH request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec<RequestBodySpec> patch();

	/**
	 * Prepare an HTTP DELETE request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec<RequestHeadersSpec<?>> delete();

	/**
	 * Prepare an HTTP OPTIONS request.
	 * @return a spec for specifying the target URL
	 */
	UriSpec<RequestHeadersSpec<?>> options();

	/**
	 * Prepare a request for the specified {@code HttpMethod}.
	 * @return a spec for specifying the target URL
	 */
	UriSpec<RequestBodySpec> method(HttpMethod method);


	/**
	 * Filter the client with the given {@code ExchangeFilterFunction}.
	 * @param filterFunction the filter to apply to this client
	 * @return the filtered client
	 * @see ExchangeFilterFunction#apply(ExchangeFunction)
	 */
	WebClient filter(ExchangeFilterFunction filterFunction);


	// Static, factory methods

	/**
	 * Create a new {@code WebClient} with no default, shared preferences across
	 * requests such as base URI, default headers, and others.
	 * @see #create(String)
	 */
	static WebClient create() {
		return new DefaultWebClientBuilder().build();
	}

	/**
	 * Configure a base URI for requests performed through the client for
	 * example to avoid repeating the same host, port, base path, or even
	 * query parameters with every request.
	 * <p>For example given this initialization:
	 * <pre class="code">
	 * WebClient client = WebClient.create("http://abc.com/v1");
	 * </pre>
	 * <p>The base URI is applied to exchanges with a URI template:
	 * <pre class="code">
	 * // GET http://abc.com/v1/accounts/43
	 * Mono&#060;Account&#062; result = client.get()
	 *         .uri("/accounts/{id}", 43)
	 *         .exchange()
	 *         .then(response -> response.bodyToMono(Account.class));
	 * </pre>
	 * <p>The base URI is also applied to exchanges with a {@code UriBuilder}:
	 * <pre class="code">
	 * // GET http://abc.com/v1/accounts?q=12
	 * Flux&#060;Account&#062; result = client.get()
	 *         .uri(builder -> builder.path("/accounts").queryParam("q", "12").build())
	 *         .exchange()
	 *         .then(response -> response.bodyToFlux(Account.class));
	 * </pre>
	 * <p>The base URI can be overridden with an absolute URI:
	 * <pre class="code">
	 * // GET http://xyz.com/path
	 * Mono&#060;Account&#062; result = client.get()
	 *         .uri("http://xyz.com/path")
	 *         .exchange()
	 *         .then(response -> response.bodyToMono(Account.class));
	 * </pre>
	 * <p>The base URI can be partially overridden with a {@code UriBuilder}:
	 * <pre class="code">
	 * // GET http://abc.com/v2/accounts?q=12
	 * Flux&#060;Account&#062; result = client.get()
	 *         .uri(builder -> builder.replacePath("/v2/accounts").queryParam("q", "12").build())
	 *         .exchange()
	 *         .then(response -> response.bodyToFlux(Account.class));
	 * </pre>
	 * @param baseUrl the base URI for all requests
	 */
	static WebClient create(String baseUrl) {
		return new DefaultWebClientBuilder().baseUrl(baseUrl).build();
	}

	/**
	 * Obtain a {@code WebClient} builder.
	 */
	static WebClient.Builder builder() {
		return new DefaultWebClientBuilder();
	}


	/**
	 * A mutable builder for a {@link WebClient}.
	 */
	interface Builder {

		/**
		 * Configure a base URI as described in {@link WebClient#create(String)
		 * WebClient.create(String)}.
		 * @see #defaultUriVariables(Map)
		 * @see #uriBuilderFactory(UriBuilderFactory)
		 */
		Builder baseUrl(String baseUrl);

		/**
		 * Configure default URI variable values that will be used when expanding
		 * URI templates using a {@link Map}.
		 * @param defaultUriVariables the default values to use
		 * @see #baseUrl(String)
		 * @see #uriBuilderFactory(UriBuilderFactory)
		 */
		Builder defaultUriVariables(Map<String, ?> defaultUriVariables);

		/**
		 * Provide a pre-configured {@link UriBuilderFactory} instance. This is
		 * an alternative to and effectively overrides the following:
		 * <ul>
		 * <li>{@link #baseUrl(String)}
		 * <li>{@link #defaultUriVariables(Map)}.
		 * </ul>
		 * @param uriBuilderFactory the URI builder factory to use
		 * @see #baseUrl(String)
		 * @see #defaultUriVariables(Map)
		 */
		Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory);

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
		 * Configure the {@link ClientHttpConnector} to use.
		 * <p>By default an instance of
		 * {@link org.springframework.http.client.reactive.ReactorClientHttpConnector
		 * ReactorClientHttpConnector} is created if this is not set. However a
		 * shared instance may be passed instead, e.g. for use with multiple
		 * {@code WebClient}'s targeting different base URIs.
		 * @param connector the connector to use
		 * @see #exchangeStrategies(ExchangeStrategies)
		 * @see #exchangeFunction(ExchangeFunction)
		 */
		Builder clientConnector(ClientHttpConnector connector);

		/**
		 * Configure the {@link ExchangeStrategies} to use.
		 * <p>By default {@link ExchangeStrategies#withDefaults()} is used.
		 * @param strategies the strategies to use
		 * @see #clientConnector(ClientHttpConnector)
		 * @see #exchangeFunction(ExchangeFunction)
		 */
		Builder exchangeStrategies(ExchangeStrategies strategies);

		/**
		 * Provide a pre-configured {@link ExchangeFunction} instance. This is
		 * an alternative to and effectively overrides the following:
		 * <ul>
		 * <li>{@link #clientConnector(ClientHttpConnector)}
		 * <li>{@link #exchangeStrategies(ExchangeStrategies)}.
		 * </ul>
		 * @param exchangeFunction the exchange function to use
		 * @see #clientConnector(ClientHttpConnector)
		 * @see #exchangeStrategies(ExchangeStrategies)
		 */
		Builder exchangeFunction(ExchangeFunction exchangeFunction);

		/**
		 * Builder the {@link WebClient} instance.
		 */
		WebClient build();

	}


	/**
	 * Contract for specifying the URI for a request.
	 */
	interface UriSpec<S extends RequestHeadersSpec<?>> {

		/**
		 * Specify the URI using an absolute, fully constructed {@link URI}.
		 */
		S uri(URI uri);

		/**
		 * Specify the URI for the request using a URI template and URI variables.
		 * If a {@link UriBuilderFactory} was configured for the client (e.g.
		 * with a base URI) it will be used to expand the URI template.
		 */
		S uri(String uri, Object... uriVariables);

		/**
		 * Specify the URI for the request using a URI template and URI variables.
		 * If a {@link UriBuilderFactory} was configured for the client (e.g.
		 * with a base URI) it will be used to expand the URI template.
		 */
		S uri(String uri, Map<String, ?> uriVariables);

		/**
		 * Build the URI for the request using the {@link UriBuilderFactory}
		 * configured for this client.
		 */
		S uri(Function<UriBuilder, URI> uriFunction);
	}

	/**
	 * Contract for specifying request headers leading up to the exchange.
	 */
	interface RequestHeadersSpec<S extends RequestHeadersSpec<S>> {

		/**
		 * Set the list of acceptable {@linkplain MediaType media types}, as
		 * specified by the {@code Accept} header.
		 * @param acceptableMediaTypes the acceptable media types
		 * @return this builder
		 */
		S accept(MediaType... acceptableMediaTypes);

		/**
		 * Set the list of acceptable {@linkplain Charset charsets}, as specified
		 * by the {@code Accept-Charset} header.
		 * @param acceptableCharsets the acceptable charsets
		 * @return this builder
		 */
		S acceptCharset(Charset... acceptableCharsets);

		/**
		 * Add a cookie with the given name and value.
		 * @param name the cookie name
		 * @param value the cookie value
		 * @return this builder
		 */
		S cookie(String name, String value);

		/**
		 * Copy the given cookies into the entity's cookies map.
		 * @param cookies the existing cookies to copy from
		 * @return this builder
		 */
		S cookies(MultiValueMap<String, String> cookies);

		/**
		 * Set the value of the {@code If-Modified-Since} header.
		 * <p>The date should be specified as the number of milliseconds since
		 * January 1, 1970 GMT.
		 * @param ifModifiedSince the new value of the header
		 * @return this builder
		 */
		S ifModifiedSince(ZonedDateTime ifModifiedSince);

		/**
		 * Set the values of the {@code If-None-Match} header.
		 * @param ifNoneMatches the new value of the header
		 * @return this builder
		 */
		S ifNoneMatch(String... ifNoneMatches);

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 */
		S header(String headerName, String... headerValues);

		/**
		 * Copy the given headers into the entity's headers map.
		 * @param headers the existing headers to copy from
		 * @return this builder
		 */
		S headers(HttpHeaders headers);

		/**
		 * Exchange the request for a {@code ClientResponse} with full access
		 * to the response status and headers before extracting the body.
		 *
		 * <p>Use {@link Mono#then(Function)} or {@link Mono#flatMap(Function)}
		 * to compose further on the response:
		 *
		 * <pre>
		 *	Mono&lt;Pojo&gt; mono = client.get().uri("/")
		 *		.accept(MediaType.APPLICATION_JSON)
		 *		.exchange()
		 *		.then(response -> response.bodyToMono(Pojo.class));
		 *
		 *	Flux&lt;Pojo&gt; flux = client.get().uri("/")
		 *		.accept(MediaType.APPLICATION_STREAM_JSON)
		 *		.exchange()
		 *		.then(response -> response.bodyToFlux(Pojo.class));
		 * </pre>
		 *
		 * @return a {@code Mono} with the response
		 */
		Mono<ClientResponse> exchange();

		/**
		 * A variant of {@link #exchange()} that provides the shortest path to
		 * retrieving the full response (i.e. status, headers, and body) where
		 * instead of returning {@code Mono<ClientResponse>} it exposes shortcut
		 * methods to extract the response body.
		 *
		 * <p>Use of this method is simpler when you don't need to deal directly
		 * with {@link ClientResponse}, e.g. to use a custom {@code BodyExtractor}
		 * or to check the status and headers before extracting the response.
		 *
		 * <pre>
		 *	Mono&lt;Pojo&gt; bodyMono = client.get().uri("/")
		 *		.accept(MediaType.APPLICATION_JSON)
		 *		.retrieve()
		 *		.bodyToMono(Pojo.class);
		 *
		 *	Mono&lt;ResponseEntity&lt;Pojo&gt;&gt; entityMono = client.get().uri("/")
		 *		.accept(MediaType.APPLICATION_JSON)
		 *		.retrieve()
		 *		.bodyToEntity(Pojo.class);
		 * </pre>
		 *
		 * @return spec with options for extracting the response body
		 */
		ResponseSpec retrieve();

	}

	interface RequestBodySpec extends RequestHeadersSpec<RequestBodySpec> {

		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 * @param contentLength the content length
		 * @return this builder
		 * @see HttpHeaders#setContentLength(long)
		 */
		RequestBodySpec contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified
		 * by the {@code Content-Type} header.
		 * @param contentType the content type
		 * @return this builder
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		RequestBodySpec contentType(MediaType contentType);

		/**
		 * Set the body of the request to the given {@code BodyInserter}.
		 * @param inserter the {@code BodyInserter} that writes to the request
		 * @return this builder
		 */
		RequestHeadersSpec<?> body(BodyInserter<?, ? super ClientHttpRequest> inserter);

		/**
		 * Set the body of the request to the given {@code Publisher}.
		 * <p>This method is a convenient shortcut for {@link #body(BodyInserter)} with a
		 * {@linkplain org.springframework.web.reactive.function.BodyInserters#fromPublisher}
		 * Publisher body inserter}.
		 * @param publisher the {@code Publisher} to write to the request
		 * @param elementClass the class of elements contained in the publisher
		 * @param <T> the type of the elements contained in the publisher
		 * @param <P> the type of the {@code Publisher}
		 * @return this builder
		 */
		<T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher, Class<T> elementClass);

		/**
		 * Set the body of the request to the given {@code Object}.
		 * <p>This method is a convenient shortcut for {@link #body(BodyInserter)} with a
		 * {@linkplain org.springframework.web.reactive.function.BodyInserters#fromObject
		 * Object body inserter}.
		 * @param body the {@code Object} to write to the request
		 * @return this builder
		 */
		RequestHeadersSpec<?> body(Object body);

	}

	interface ResponseSpec {

		/**
		 * Extract the response body to an Object of type {@code <T>} by
		 * invoking {@link ClientResponse#bodyToMono(Class)}.
		 *
		 * @param bodyType the expected response body type
		 * @param <T> response body type
		 * @return {@code Mono} with the result
		 */
		<T> Mono<T> bodyToMono(Class<T> bodyType);

		/**
		 * Extract the response body to a stream of Objects of type {@code <T>}
		 * by invoking {@link ClientResponse#bodyToFlux(Class)}.
		 *
		 * @param elementType the type of element in the response
		 * @param <T> the type of elements in the response
		 * @return the body of the response
		 */
		<T> Flux<T> bodyToFlux(Class<T> elementType);

		/**
		 * A variant of {@link #bodyToMono(Class)} that also provides access to
		 * the response status and headers.
		 *
		 * @param bodyType the expected response body type
		 * @param <T> response body type
		 * @return {@code Mono} with the result
		 */
		<T> Mono<ResponseEntity<T>> bodyToEntity(Class<T> bodyType);

		/**
		 * A variant of {@link #bodyToFlux(Class)} collected via
		 * {@link Flux#collectList()} and wrapped in {@code ResponseEntity}.
		 *
		 * @param elementType the expected response body list element type
		 * @param <T> the type of elements in the list
		 * @return {@code Mono} with the result
		 */
		<T> Mono<ResponseEntity<List<T>>> bodyToEntityList(Class<T> elementType);

	}

}
