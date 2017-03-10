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
package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Main entry point for testing WebFlux server endpoints with an API similar to
 * that of {@link WebClient}, and actually delegating to a {@code WebClient}
 * instance, but with a focus on testing.
 *
 * <p>The {@code WebTestClient} has 3 setup options without a running server:
 * <ul>
 * <li>{@link #bindToController}
 * <li>{@link #bindToApplicationContext}
 * <li>{@link #bindToRouterFunction}
 * </ul>
 * <p>and 1 option for actual requests on a socket:
 * <ul>
 * <li>{@link #bindToServer()}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebTestClient {

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
	WebTestClient filter(ExchangeFilterFunction filterFunction);


	// Static, factory methods

	/**
	 * Integration testing without a server, targeting specific annotated,
	 * WebFlux controllers. The default configuration is the same as for
	 * {@link org.springframework.web.reactive.config.EnableWebFlux @EnableWebFlux}
	 * but can also be further customized through the returned spec.
	 * @param controllers the controllers to test
	 * @return spec for setting up controller configuration
	 */
	static ControllerSpec bindToController(Object... controllers) {
		return new DefaultControllerSpec(controllers);
	}

	/**
	 * Integration testing without a server, with WebFlux infrastructure detected
	 * from an {@link ApplicationContext} such as {@code @EnableWebFlux}
	 * Java config and annotated controller Spring beans.
	 * @param applicationContext the context
	 * @return the {@link WebTestClient} builder
	 * @see org.springframework.web.reactive.config.EnableWebFlux
	 */
	static MockServerSpec<?> bindToApplicationContext(ApplicationContext applicationContext) {
		return new ApplicationContextSpec(applicationContext);
	}

	/**
	 * Integration testing without a server, targeting WebFlux functional endpoints.
	 * @param routerFunction the RouterFunction to test
	 * @return the {@link WebTestClient} builder
	 */
	static MockServerSpec<?> bindToRouterFunction(RouterFunction<?> routerFunction) {
		return new RouterFunctionSpec(routerFunction);
	}

	/**
	 * Complete end-to-end integration tests with actual requests to a running server.
	 * @return the {@link WebTestClient} builder
	 */
	static Builder bindToServer() {
		return new DefaultWebTestClientBuilder();
	}


	/**
	 * Base specification for setting up tests without a server.
	 */
	interface MockServerSpec<B extends MockServerSpec<B>> {

		/**
		 * Configure a transformation function on {@code ServerWebExchange} to
		 * be applied at the start of server-side, request processing.
		 * @param function the transforming function.
		 * @see ServerWebExchange#mutate()
		 */
		<T extends B> T exchangeMutator(Function<ServerWebExchange, ServerWebExchange> function);

		/**
		 * Proceed to configure and build the test client.
		 */
		Builder configureClient();

		/**
		 * Shortcut to build the test client.
		 */
		WebTestClient build();

	}

	/**
	 * Specification for customizing controller configuration equivalent to, and
	 * internally delegating to, a {@link WebFluxConfigurer}.
	 */
	interface ControllerSpec extends MockServerSpec<ControllerSpec> {

		/**
		 * Register one or more
		 * {@link org.springframework.web.bind.annotation.ControllerAdvice
		 * ControllerAdvice} instances to be used in tests.
		 */
		ControllerSpec controllerAdvice(Object... controllerAdvice);

		/**
		 * Customize content type resolution.
		 * @see WebFluxConfigurer#configureContentTypeResolver
		 */
		ControllerSpec contentTypeResolver(Consumer<RequestedContentTypeResolverBuilder> consumer);

		/**
		 * Configure CORS support.
		 * @see WebFluxConfigurer#addCorsMappings
		 */
		ControllerSpec corsMappings(Consumer<CorsRegistry> consumer);

		/**
		 * Configure path matching options.
		 * @see WebFluxConfigurer#configurePathMatching
		 */
		ControllerSpec pathMatching(Consumer<PathMatchConfigurer> consumer);

		/**
		 * Modify or extend the list of built-in message readers.
		 * @see WebFluxConfigurer#configureMessageReaders
		 */
		ControllerSpec messageReaders(Consumer<List<HttpMessageReader<?>>> readers);

		/**
		 * Modify or extend the list of built-in message writers.
		 * @see WebFluxConfigurer#configureMessageWriters
		 */
		ControllerSpec messageWriters(Consumer<List<HttpMessageWriter<?>>> writers);

		/**
		 * Register formatters and converters to use for type conversion.
		 * @see WebFluxConfigurer#addFormatters
		 */
		ControllerSpec formatters(Consumer<FormatterRegistry> consumer);

		/**
		 * Configure a global Validator.
		 * @see WebFluxConfigurer#getValidator()
		 */
		ControllerSpec validator(Validator validator);

		/**
		 * Configure view resolution.
		 * @see WebFluxConfigurer#configureViewResolvers
		 */
		ControllerSpec viewResolvers(Consumer<ViewResolverRegistry> consumer);

	}

	/**
	 * Steps for customizing the {@link WebClient} used to test with
	 * internally delegating to a {@link WebClient.Builder}.
	 */
	interface Builder {

		/**
		 * Configure a base URI as described in
		 * {@link org.springframework.web.reactive.function.client.WebClient#create(String)
		 * WebClient.create(String)}.
		 */
		Builder baseUrl(String baseUrl);

		/**
		 * Provide a pre-configured {@link UriBuilderFactory} instance as an
		 * alternative to and effectively overriding {@link #baseUrl(String)}.
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
		 * Configure the {@link ExchangeStrategies} to use.
		 * <p>By default {@link ExchangeStrategies#withDefaults()} is used.
		 * @param strategies the strategies to use
		 */
		Builder exchangeStrategies(ExchangeStrategies strategies);

		/**
		 * Max amount of time to wait for responses.
		 * <p>By default 5 seconds.
		 * @param timeout the response timeout value
		 */
		Builder responseTimeout(Duration timeout);


		/**
		 * Build the {@link WebTestClient} instance.
		 */
		WebTestClient build();

	}


	/**
	 * Specification for providing the URI of a request.
	 */
	interface UriSpec {

		/**
		 * Specify the URI using an absolute, fully constructed {@link URI}.
		 * @return spec to add headers or perform the exchange
		 */
		HeaderSpec uri(URI uri);

		/**
		 * Specify the URI for the request using a URI template and URI variables.
		 * If a {@link UriBuilderFactory} was configured for the client (e.g.
		 * with a base URI) it will be used to expand the URI template.
		 * @return spec to add headers or perform the exchange
		 */
		HeaderSpec uri(String uri, Object... uriVariables);

		/**
		 * Specify the URI for the request using a URI template and URI variables.
		 * If a {@link UriBuilderFactory} was configured for the client (e.g.
		 * with a base URI) it will be used to expand the URI template.
		 * @return spec to add headers or perform the exchange
		 */
		HeaderSpec uri(String uri, Map<String, ?> uriVariables);

		/**
		 * Build the URI for the request with a {@link UriBuilder} obtained
		 * through the {@link UriBuilderFactory} configured for this client.
		 * @return spec to add headers or perform the exchange
		 */
		HeaderSpec uri(Function<UriBuilder, URI> uriFunction);

	}

	/**
	 * Specification for adding request headers and performing an exchange.
	 */
	interface HeaderSpec {

		/**
		 * Set the list of acceptable {@linkplain MediaType media types}, as
		 * specified by the {@code Accept} header.
		 * @param acceptableMediaTypes the acceptable media types
		 * @return the same instance
		 */
		HeaderSpec accept(MediaType... acceptableMediaTypes);

		/**
		 * Set the list of acceptable {@linkplain Charset charsets}, as specified
		 * by the {@code Accept-Charset} header.
		 * @param acceptableCharsets the acceptable charsets
		 * @return the same instance
		 */
		HeaderSpec acceptCharset(Charset... acceptableCharsets);

		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 * @param contentLength the content length
		 * @return the same instance
		 * @see HttpHeaders#setContentLength(long)
		 */
		HeaderSpec contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified
		 * by the {@code Content-Type} header.
		 * @param contentType the content type
		 * @return the same instance
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		HeaderSpec contentType(MediaType contentType);

		/**
		 * Add a cookie with the given name and value.
		 * @param name the cookie name
		 * @param value the cookie value
		 * @return the same instance
		 */
		HeaderSpec cookie(String name, String value);

		/**
		 * Copy the given cookies into the entity's cookies map.
		 *
		 * @param cookies the existing cookies to copy from
		 * @return the same instance
		 */
		HeaderSpec cookies(MultiValueMap<String, String> cookies);

		/**
		 * Set the value of the {@code If-Modified-Since} header.
		 * <p>The date should be specified as the number of milliseconds since
		 * January 1, 1970 GMT.
		 * @param ifModifiedSince the new value of the header
		 * @return the same instance
		 */
		HeaderSpec ifModifiedSince(ZonedDateTime ifModifiedSince);

		/**
		 * Set the values of the {@code If-None-Match} header.
		 * @param ifNoneMatches the new value of the header
		 * @return the same instance
		 */
		HeaderSpec ifNoneMatch(String... ifNoneMatches);

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return the same instance
		 */
		HeaderSpec header(String headerName, String... headerValues);

		/**
		 * Copy the given headers into the entity's headers map.
		 * @param headers the existing headers to copy from
		 * @return the same instance
		 */
		HeaderSpec headers(HttpHeaders headers);

		/**
		 * Perform the exchange without a request body.
		 * @return spec for decoding the response
		 */
		ResponseSpec exchange();

		/**
		 * Perform the exchange with the body for the request populated using
		 * a {@link BodyInserter}.
		 * @param inserter the inserter
		 * @param <T> the body type, or the the element type (for a stream)
		 * @return spec for decoding the response
		 * @see org.springframework.web.reactive.function.BodyInserters
		 */
		<T> ResponseSpec exchange(BodyInserter<T, ? super ClientHttpRequest> inserter);

		/**
		 * Perform the exchange and use the given {@code Publisher} for the
		 * request body.
		 * @param publisher the request body data
		 * @param elementClass the class of elements contained in the publisher
		 * @param <T> the type of the elements contained in the publisher
		 * @param <S> the type of the {@code Publisher}
		 * @return spec for decoding the response
		 */
		<T, S extends Publisher<T>> ResponseSpec exchange(S publisher, Class<T> elementClass);
	}

	/**
	 * Specification for processing the response and applying expectations.
	 */
	interface ResponseSpec {

		/**
		 * Assertions on the response status.
		 */
		StatusAssertions expectStatus();

		/**
		 * Assertions on the headers of the response.
		 */
		HeaderAssertions expectHeader();

		/**
		 * Assertions on the body of the response extracted to one or more
		 * representations of the given type.
		 */
		TypeBodySpec expectBody(Class<?> elementType);

		/**
		 * Variant of {@link #expectBody(Class)} for use with generic types.
		 */
		TypeBodySpec expectBody(ResolvableType elementType);

		/**
		 * Other assertions on the response body -- isEmpty, map, etc.
		 */
		BodySpec expectBody();

	}

	/**
	 * Specification for extracting entities from the response body.
	 */
	interface TypeBodySpec {

		/**
		 * Extract a single representations from the response.
		 */
		SingleValueBodySpec value();

		/**
		 * Extract a list of representations from the response.
		 */
		ListBodySpec list();

		/**
		 * Extract a list of representations consuming the first N elements.
		 */
		ListBodySpec list(int elementCount);

		/**
		 * Return request and response details for the exchange incluidng the
		 * response body decoded as {@code Flux<T>} where {@code <T>} is the
		 * expected element type. The returned {@code Flux} may for example be
		 * verified with the Reactor {@code StepVerifier}.
		 */
		<T> FluxExchangeResult<T> returnResult();
	}

	/**
	 * Specification to assert a single value extracted from the response body.
	 */
	interface SingleValueBodySpec {

		/**
		 * Assert the extracted body is equal to the given value.
		 */
		<T> EntityExchangeResult<T> isEqualTo(T expected);

		/**
		 * Return request and response details for the exchange including the
		 * extracted response body.
		 */
		<T> EntityExchangeResult<T> returnResult();
	}

	/**
	 * Specification to assert a list of values extracted from the response.
	 */
	interface ListBodySpec {

		/**
		 * Assert the extracted body is equal to the given list.
		 */
		<T> EntityExchangeResult<List<T>> isEqualTo(List<T> expected);

		/**
		 * Assert the extracted list of values is of the given size.
		 * @param size the expected size
		 */
		ListBodySpec hasSize(int size);

		/**
		 * Assert the extracted list of values contains the given elements.
		 * @param elements the elements to check
		 */
		ListBodySpec contains(Object... elements);

		/**
		 * Assert the extracted list of values doesn't contain the given elements.
		 * @param elements the elements to check
		 */
		ListBodySpec doesNotContain(Object... elements);

		/**
		 * Return request and response details for the exchange including the
		 * extracted response body.
		 */
		<T> EntityExchangeResult<List<T>> returnResult();
	}

	/**
	 * Specification to apply additional assertions on the response body.
	 */
	interface BodySpec {

		/**
		 * Consume the body and verify it is empty.
		 * @return request and response details from the exchange
		 */
		EntityExchangeResult<Void> isEmpty();

		/**
		 * Extract the response body as a Map with the given key and value type.
		 */
		MapBodySpec map(Class<?> keyType, Class<?> valueType);

		/**
		 * Variant of {@link #map(Class, Class)} for use with generic types.
		 */
		MapBodySpec map(ResolvableType keyType, ResolvableType valueType);

	}

	/**
	 * Specification to assert response the body extracted as a map.
	 */
	interface MapBodySpec {

		/**
		 * Assert the extracted map is equal to the given list of elements.
		 */
		<K, V> EntityExchangeResult<Map<K, V>> isEqualTo(Map<K, V> expected);

		/**
		 * Assert the extracted map has the given size.
		 * @param size the expected size
		 */
		MapBodySpec hasSize(int size);

		/**
		 * Assert the extracted map contains the given key value pair.
		 * @param key the key to check
		 * @param value the value to check
		 */
		MapBodySpec contains(Object key, Object value);

		/**
		 * Assert the extracted map contains the given keys.
		 * @param keys the keys to check
		 */
		MapBodySpec containsKeys(Object... keys);

		/**
		 * Assert the extracted map contains the given values.
		 * @param values the keys to check
		 */
		MapBodySpec containsValues(Object... values);

		/**
		 * Return request and response details for the exchange including the
		 * extracted response body.
		 */
		<K, V> EntityExchangeResult<Map<K, V>> returnResult();
	}

}
