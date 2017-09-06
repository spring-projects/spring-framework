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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
	RequestHeadersUriSpec<?> get();

	/**
	 * Prepare an HTTP HEAD request.
	 * @return a spec for specifying the target URL
	 */
	RequestHeadersUriSpec<?> head();

	/**
	 * Prepare an HTTP POST request.
	 * @return a spec for specifying the target URL
	 */
	RequestBodyUriSpec post();

	/**
	 * Prepare an HTTP PUT request.
	 * @return a spec for specifying the target URL
	 */
	RequestBodyUriSpec put();

	/**
	 * Prepare an HTTP PATCH request.
	 * @return a spec for specifying the target URL
	 */
	RequestBodyUriSpec patch();

	/**
	 * Prepare an HTTP DELETE request.
	 * @return a spec for specifying the target URL
	 */
	RequestHeadersUriSpec<?> delete();

	/**
	 * Prepare an HTTP OPTIONS request.
	 * @return a spec for specifying the target URL
	 */
	RequestHeadersUriSpec<?> options();

	/**
	 * Prepare a request for the specified {@code HttpMethod}.
	 * @return a spec for specifying the target URL
	 */
	RequestBodyUriSpec method(HttpMethod method);


	/**
	 * Return a builder to mutate properties of this web client.
	 */
	Builder mutate();


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
		 * Add the given header to all requests that have not added it.
		 * @param headerName the header name
		 * @param headerValues the header values
		 */
		Builder defaultHeader(String headerName, String... headerValues);

		/**
		 * Manipulate the default headers with the given consumer. The
		 * headers provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
		 * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
		 * {@link HttpHeaders} methods.
		 * @param headersConsumer a function that consumes the {@code HttpHeaders}
		 * @return this builder
		 */
		Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Add the given header to all requests that haven't added it.
		 * @param cookieName the cookie name
		 * @param cookieValues the cookie values
		 */
		Builder defaultCookie(String cookieName, String... cookieValues);

		/**
		 * Manipulate the default cookies with the given consumer. The
		 * map provided to the consumer is "live", so that the consumer can be used to
		 * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing header values,
		 * {@linkplain MultiValueMap#remove(Object) remove} values, or use any of the other
		 * {@link MultiValueMap} methods.
		 * @param cookiesConsumer a function that consumes the cookies map
		 * @return this builder
		 */
		Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

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
		 * Add the given filter to the filter chain.
		 * @param filter the filter to be added to the chain
		 */
		Builder filter(ExchangeFilterFunction filter);

		/**
		 * Manipulate the filters with the given consumer. The
		 * list provided to the consumer is "live", so that the consumer can be used to remove
		 * filters, change ordering, etc.
		 * @param filtersConsumer a function that consumes the filter list
		 * @return this builder
		 */
		Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer);

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
		 * Clone this {@code WebClient.Builder}
		 */
		Builder clone();

		/**
		 * Shortcut for pre-packaged customizations to WebTest builder.
		 * @param builderConsumer the consumer to apply
		 */
		Builder apply(Consumer<Builder> builderConsumer);

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
		 * Manipulate the request's cookies with the given consumer. The
		 * map provided to the consumer is "live", so that the consumer can be used to
		 * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing header values,
		 * {@linkplain MultiValueMap#remove(Object) remove} values, or use any of the other
		 * {@link MultiValueMap} methods.
		 * @param cookiesConsumer a function that consumes the cookies map
		 * @return this builder
		 */
		S cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

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
		 * Manipulate the request's headers with the given consumer. The
		 * headers provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
		 * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
		 * {@link HttpHeaders} methods.
		 * @param headersConsumer a function that consumes the {@code HttpHeaders}
		 * @return this builder
		 */
		S headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Set the attribute with the given name to the given value.
		 * @param name the name of the attribute to add
		 * @param value the value of the attribute to add
		 * @return this builder
		 */
		S attribute(String name, Object value);

		/**
		 * Manipulate the request attributes with the given consumer. The attributes provided to
		 * the consumer are "live", so that the consumer can be used to inspect attributes,
		 * remove attributes, or use any of the other map-provided methods.
		 * @param attributesConsumer a function that consumes the attributes
		 * @return this builder
		 */
		S attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * Exchange the request for a {@code ClientResponse} with full access
		 * to the response status and headers before extracting the body.
		 * <p>Use {@link Mono#flatMap(Function)} or
		 * {@link Mono#flatMapMany(Function)} to compose further on the response:
		 * <pre>
		 * Mono&lt;Pojo&gt; mono = client.get().uri("/")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .exchange()
		 *     .flatMap(response -> response.bodyToMono(Pojo.class));
		 *
		 * Flux&lt;Pojo&gt; flux = client.get().uri("/")
		 *     .accept(MediaType.APPLICATION_STREAM_JSON)
		 *     .exchange()
		 *     .flatMapMany(response -> response.bodyToFlux(Pojo.class));
		 * </pre>
		 * <p>If the response body is not consumed with {@code bodyTo*}
		 * or {@code toEntity*} methods, it is your responsibility
		 * to release the HTTP resources with {@link ClientResponse#close()}.
		 * <pre>
		 * Mono&lt;HttpStatus&gt; mono = client.get().uri("/")
		 *     .exchange()
		 *     .map(response -> {
		 *         response.close();
		 *         return response.statusCode();
		 *     });
		 * </pre>
		 * @return a {@code Mono} with the response
		 * @see #retrieve()
		 */
		Mono<ClientResponse> exchange();

		/**
		 * A variant of {@link #exchange()} that provides the shortest path to
		 * retrieving the full response (i.e. status, headers, and body) where
		 * instead of returning {@code Mono<ClientResponse>} it exposes shortcut
		 * methods to extract the response body.
		 * <p>Use of this method is simpler when you don't need to deal directly
		 * with {@link ClientResponse}, e.g. to use a custom {@code BodyExtractor}
		 * or to check the status and headers before extracting the response.
		 * <pre>
		 * Mono&lt;Pojo&gt; bodyMono = client.get().uri("/")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .retrieve()
		 *     .bodyToMono(Pojo.class);
		 * </pre>
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
		 * Set the body of the request to the given asynchronous {@code Publisher}.
		 * <p>This method is a convenient shortcut for {@link #body(BodyInserter)} with a
		 * {@linkplain org.springframework.web.reactive.function.BodyInserters#fromPublisher}
		 * Publisher body inserter}.
		 * @param publisher the {@code Publisher} to write to the request
		 * @param typeReference the type reference of elements contained in the publisher
		 * @param <T> the type of the elements contained in the publisher
		 * @param <P> the type of the {@code Publisher}
		 * @return this builder
		 */
		<T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher, ParameterizedTypeReference<T> typeReference);

		/**
		 * Set the body of the request to the given asynchronous {@code Publisher}.
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
		 * Set the body of the request to the given synchronous {@code Object}.
		 * <p>This method is a convenient shortcut for {@link #body(BodyInserter)} with a
		 * {@linkplain org.springframework.web.reactive.function.BodyInserters#fromObject
		 * Object body inserter}.
		 * @param body the {@code Object} to write to the request
		 * @return this builder
		 */
		RequestHeadersSpec<?> syncBody(Object body);
	}


	interface ResponseSpec {

		/**
		 * Register a custom error function that gets invoked when the given {@link HttpStatus}
		 * predicate applies. The exception returned from the function will be returned from
		 * {@link #bodyToMono(Class)} and {@link #bodyToFlux(Class)}.
		 * <p>By default, an error handler is register that throws a
		 * {@link WebClientResponseException} when the response status code is 4xx or 5xx.
		 * @param statusPredicate a predicate that indicates whether {@code exceptionFunction}
		 * applies
		 * @param exceptionFunction the function that returns the exception
		 * @return this builder
		 */
		ResponseSpec onStatus(Predicate<HttpStatus> statusPredicate,
				Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction);

		/**
		 * Extract the body to a {@code Mono}. By default, if the response has status code 4xx or
		 * 5xx, the {@code Mono} will contain a {@link WebClientException}. This can be overridden
		 * with {@link #onStatus(Predicate, Function)}.
		 * @param bodyType the expected response body type
		 * @param <T> response body type
		 * @return a mono containing the body, or a {@link WebClientResponseException} if the
		 * status code is 4xx or 5xx
		 */
		<T> Mono<T> bodyToMono(Class<T> bodyType);

		/**
		 * Extract the body to a {@code Mono}. By default, if the response has status code 4xx or
		 * 5xx, the {@code Mono} will contain a {@link WebClientException}. This can be overridden
		 * with {@link #onStatus(Predicate, Function)}.
		 * @param typeReference a type reference describing the expected response body type
		 * @param <T> response body type
		 * @return a mono containing the body, or a {@link WebClientResponseException} if the
		 * status code is 4xx or 5xx
		 */
		<T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference);

		/**
		 * Extract the body to a {@code Flux}. By default, if the response has status code 4xx or
		 * 5xx, the {@code Flux} will contain a {@link WebClientException}. This can be overridden
         * with {@link #onStatus(Predicate, Function)}.
		 * @param elementType the type of element in the response
		 * @param <T> the type of elements in the response
		 * @return a flux containing the body, or a {@link WebClientResponseException} if the
		 * status code is 4xx or 5xx
		 */
		<T> Flux<T> bodyToFlux(Class<T> elementType);

		/**
		 * Extract the body to a {@code Flux}. By default, if the response has status code 4xx or
		 * 5xx, the {@code Flux} will contain a {@link WebClientException}. This can be overridden
         * with {@link #onStatus(Predicate, Function)}.
		 * @param typeReference a type reference describing the expected response body type
		 * @param <T> the type of elements in the response
		 * @return a flux containing the body, or a {@link WebClientResponseException} if the
		 * status code is 4xx or 5xx
		 */
		<T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference);

	}


	interface RequestHeadersUriSpec<S extends RequestHeadersSpec<S>>
			extends UriSpec<S>, RequestHeadersSpec<S> {
	}


	interface RequestBodyUriSpec extends RequestBodySpec, RequestHeadersUriSpec<RequestBodySpec> {
	}


}
