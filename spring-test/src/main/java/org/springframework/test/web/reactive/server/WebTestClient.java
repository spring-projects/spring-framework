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
import java.util.function.UnaryOperator;

import org.reactivestreams.Publisher;

import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.ServerCodecConfigurer;
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
import org.springframework.web.server.WebFilter;
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
	 * Filter the client with the given {@code ExchangeFilterFunction}.
	 * @param filterFunction the filter to apply to this client
	 * @return the filtered client
	 * @see ExchangeFilterFunction#apply(ExchangeFunction)
	 */
	WebTestClient filter(ExchangeFilterFunction filterFunction);

	/**
	 * Filter the client applying the given transformation function on the
	 * {@code ServerWebExchange} to every request.
	 * <p><strong>Note:</strong> this option is applicable only when testing
	 * without an actual running server.
	 * @param mutator the transformation function
	 * @return the filtered client
	 */
	WebTestClient exchangeMutator(UnaryOperator<ServerWebExchange> mutator);


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
		 * @param mutator the transforming function.
		 * @see ServerWebExchange#mutate()
		 */
		<T extends B> T exchangeMutator(UnaryOperator<ServerWebExchange> mutator);

		/**
		 * Configure {@link WebFilter}'s for server request processing.
		 * @param filter one or more filters
		 */
		<T extends B> T webFilter(WebFilter... filter);

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
		 * Modify or extend the list of built-in message readers and writers.
		 * @see WebFluxConfigurer#configureHttpMessageCodecs
		 */
		ControllerSpec httpMessageCodecs(Consumer<ServerCodecConfigurer> configurer);

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
	interface UriSpec<S extends RequestHeadersSpec<?>> {

		/**
		 * Specify the URI using an absolute, fully constructed {@link URI}.
		 * @return spec to add headers or perform the exchange
		 */
		S uri(URI uri);

		/**
		 * Specify the URI for the request using a URI template and URI variables.
		 * If a {@link UriBuilderFactory} was configured for the client (e.g.
		 * with a base URI) it will be used to expand the URI template.
		 * @return spec to add headers or perform the exchange
		 */
		S uri(String uri, Object... uriVariables);

		/**
		 * Specify the URI for the request using a URI template and URI variables.
		 * If a {@link UriBuilderFactory} was configured for the client (e.g.
		 * with a base URI) it will be used to expand the URI template.
		 * @return spec to add headers or perform the exchange
		 */
		S uri(String uri, Map<String, ?> uriVariables);

		/**
		 * Build the URI for the request with a {@link UriBuilder} obtained
		 * through the {@link UriBuilderFactory} configured for this client.
		 * @return spec to add headers or perform the exchange
		 */
		S uri(Function<UriBuilder, URI> uriFunction);

	}

	/**
	 * Specification for adding request headers and performing an exchange.
	 */
	interface RequestHeadersSpec<S extends RequestHeadersSpec<S>> {

		/**
		 * Set the list of acceptable {@linkplain MediaType media types}, as
		 * specified by the {@code Accept} header.
		 * @param acceptableMediaTypes the acceptable media types
		 * @return the same instance
		 */
		S accept(MediaType... acceptableMediaTypes);

		/**
		 * Set the list of acceptable {@linkplain Charset charsets}, as specified
		 * by the {@code Accept-Charset} header.
		 * @param acceptableCharsets the acceptable charsets
		 * @return the same instance
		 */
		S acceptCharset(Charset... acceptableCharsets);

		/**
		 * Add a cookie with the given name and value.
		 * @param name the cookie name
		 * @param value the cookie value
		 * @return the same instance
		 */
		S cookie(String name, String value);

		/**
		 * Copy the given cookies into the entity's cookies map.
		 *
		 * @param cookies the existing cookies to copy from
		 * @return the same instance
		 */
		S cookies(MultiValueMap<String, String> cookies);

		/**
		 * Set the value of the {@code If-Modified-Since} header.
		 * <p>The date should be specified as the number of milliseconds since
		 * January 1, 1970 GMT.
		 * @param ifModifiedSince the new value of the header
		 * @return the same instance
		 */
		S ifModifiedSince(ZonedDateTime ifModifiedSince);

		/**
		 * Set the values of the {@code If-None-Match} header.
		 * @param ifNoneMatches the new value of the header
		 * @return the same instance
		 */
		S ifNoneMatch(String... ifNoneMatches);

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return the same instance
		 */
		S header(String headerName, String... headerValues);

		/**
		 * Copy the given headers into the entity's headers map.
		 * @param headers the existing headers to copy from
		 * @return the same instance
		 */
		S headers(HttpHeaders headers);

		/**
		 * Perform the exchange without a request body.
		 * @return spec for decoding the response
		 */
		ResponseSpec exchange();

	}

	interface RequestBodySpec extends RequestHeadersSpec<RequestBodySpec> {
		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 * @param contentLength the content length
		 * @return the same instance
		 * @see HttpHeaders#setContentLength(long)
		 */
		RequestBodySpec contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified
		 * by the {@code Content-Type} header.
		 * @param contentType the content type
		 * @return the same instance
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		RequestBodySpec contentType(MediaType contentType);

		/**
		 * Set the body of the request to the given {@code BodyInserter}.
		 * @param inserter the inserter
		 * @return spec for decoding the response
		 * @see org.springframework.web.reactive.function.BodyInserters
		 */
		RequestHeadersSpec<?> body(BodyInserter<?, ? super ClientHttpRequest> inserter);

		/**
		 * Set the body of the request to the given asynchronous {@code Publisher}.
		 * @param publisher the request body data
		 * @param elementClass the class of elements contained in the publisher
		 * @param <T> the type of the elements contained in the publisher
		 * @param <S> the type of the {@code Publisher}
		 * @return spec for decoding the response
		 */
		<T, S extends Publisher<T>> RequestHeadersSpec<?> body(S publisher, Class<T> elementClass);

		/**
		 * Set the body of the request to the given synchronous {@code Object} and
		 * perform the request.
		 * @param body the {@code Object} to write to the request
		 * @return a {@code Mono} with the response
		 */
		RequestHeadersSpec<?> syncBody(Object body);

	}

	/**
	 * Spec for declaring expectations on the response.
	 */
	interface ResponseSpec {

		/**
		 * Declare expectations on the response status.
		 */
		StatusAssertions expectStatus();

		/**
		 * Declared expectations on the headers of the response.
		 */
		HeaderAssertions expectHeader();

		/**
		 * Declare expectations on the response body decoded to {@code <B>}.
		 * @param bodyType the expected body type
		 */
		<B> BodySpec<B, ?> expectBody(Class<B> bodyType);

		/**
		 * Variant of {@link #expectBody(Class)} for a body type with generics.
		 */
		<B> BodySpec<B, ?> expectBody(ResolvableType bodyType);

		/**
		 * Declare expectations on the response body decoded to {@code List<E>}.
		 * @param elementType the expected List element type
		 */
		<E> ListBodySpec<E> expectBodyList(Class<E> elementType);

		/**
		 * Variant of {@link #expectBodyList(Class)} for element types with generics.
		 */
		<E> ListBodySpec<E> expectBodyList(ResolvableType elementType);

		/**
		 * Declare expectations on the response body content.
		 */
		BodyContentSpec expectBody();

		/**
		 * Return the exchange result with the body decoded to {@code Flux<T>}.
		 * Use this option for infinite streams and consume the stream with
		 * the {@code StepVerifier} from the Reactor Add-Ons.
		 *
		 * @see <a href="https://github.com/reactor/reactor-addons">
		 *     https://github.com/reactor/reactor-addons</a>
		 */
		<T> FluxExchangeResult<T> returnResult(Class<T> elementType);

		/**
		 * Variant of {@link #returnResult(Class)} for element types with generics.
		 */
		<T> FluxExchangeResult<T> returnResult(ResolvableType elementType);
	}

	/**
	 * Spec for expectations on the response body decoded to a single Object.
	 */
	interface BodySpec<B, S extends BodySpec<B, S>> {

		/**
		 * Assert the extracted body is equal to the given value.
		 */
		<T extends S> T isEqualTo(B expected);

		/**
		 * Assert the extracted body with the given {@link Consumer}.
		 */
		<T extends S> T consumeWith(Consumer<B> consumer);

		/**
		 * Return the exchange result with the decoded body.
		 */
		EntityExchangeResult<B> returnResult();

	}

	/**
	 * Spec for expectations on the response body decoded to a List.
	 */
	interface ListBodySpec<E> extends BodySpec<List<E>, ListBodySpec<E>> {

		/**
		 * Assert the extracted list of values is of the given size.
		 * @param size the expected size
		 */
		ListBodySpec<E> hasSize(int size);

		/**
		 * Assert the extracted list of values contains the given elements.
		 * @param elements the elements to check
		 */
		@SuppressWarnings("unchecked")
		ListBodySpec<E> contains(E... elements);

		/**
		 * Assert the extracted list of values doesn't contain the given elements.
		 * @param elements the elements to check
		 */
		@SuppressWarnings("unchecked")
		ListBodySpec<E> doesNotContain(E... elements);

	}

	/**
	 * Spec for expectations on the response body content.
	 */
	interface BodyContentSpec {

		/**
		 * Assert the response body is empty and return the exchange result.
		 */
		EntityExchangeResult<Void> isEmpty();

		/**
		 * Parse the expected and actual response content as JSON and perform a
		 * "lenient" comparison verifying the same attribute-value pairs.
		 * <p>Use of this option requires the
		 * <a href="http://jsonassert.skyscreamer.org/">JSONassert<a/> library
		 * on to be on the classpath.
		 * @param expectedJson the expected JSON content.
		 */
		BodyContentSpec json(String expectedJson);

		/**
		 * Access to response body assertions using a
		 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression
		 * to inspect a specific subset of the body.
		 * <p>The JSON path expression can be a parameterized string using
		 * formatting specifiers as defined in {@link String#format}.
		 * @param expression the JsonPath expression
		 * @param args arguments to parameterize the expression
		 */
		JsonPathAssertions jsonPath(String expression, Object... args);

		/**
		 * Assert the response body content converted to a String with the given
		 * {@link Consumer}. The String is created with the {@link Charset} from
		 * the "content-type" response header or {@code UTF-8} otherwise.
		 * @param consumer the consumer for the response body; the input String
		 * may be {@code null} if there was no response body.
		 */
		BodyContentSpec consumeAsStringWith(Consumer<String> consumer);

		/**
		 * Assert the response body content with the given {@link Consumer}.
		 * @param consumer the consumer for the response body; the input
		 * {@code byte[]} may be {@code null} if there was no response body.
		 */
		BodyContentSpec consumeWith(Consumer<byte[]> consumer);

		/**
		 * Return the exchange result with body content as {@code byte[]}.
		 */
		EntityExchangeResult<byte[]> returnResult();

	}

}
