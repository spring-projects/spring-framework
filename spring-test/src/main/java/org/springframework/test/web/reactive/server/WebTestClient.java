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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.session.WebSessionManager;
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
	 * The name of a request header used to assign a unique id to every request
	 * performed through the {@code WebTestClient}. This can be useful for
	 * storing contextual information at all phases of request processing (e.g.
	 * from a server-side component) under that id and later to look up
	 * that information once an {@link ExchangeResult} is available.
	 */
	String WEBTESTCLIENT_REQUEST_ID = "WebTestClient-Request-Id";


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
	 * Return a builder to mutate properties of this web test client.
	 */
	Builder mutate();

	/**
	 * Mutate the {@link WebTestClient}, apply the given configurer, and build
	 * a new instance. Essentially a shortcut for:
	 * <pre>
	 * mutate().apply(configurer).build();
	 * </pre>
	 * @param configurer the configurer to apply
	 * @return the mutated test client
	 */
	WebTestClient mutateWith(WebTestClientConfigurer configurer);


	// Static, factory methods

	/**
	 * Integration testing with a "mock" server targeting specific annotated,
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
	 * Integration testing with a "mock" server with WebFlux infrastructure
	 * detected from an {@link ApplicationContext} such as
	 * {@code @EnableWebFlux} Java config and annotated controller Spring beans.
	 * @param applicationContext the context
	 * @return the {@code WebTestClient} builder
	 * @see org.springframework.web.reactive.config.EnableWebFlux
	 */
	static MockServerSpec<?> bindToApplicationContext(ApplicationContext applicationContext) {
		return new ApplicationContextSpec(applicationContext);
	}

	/**
	 * Integration testing without a server targeting WebFlux functional endpoints.
	 * @param routerFunction the RouterFunction to test
	 * @return the {@code WebTestClient} builder
	 */
	static RouterFunctionSpec bindToRouterFunction(RouterFunction<?> routerFunction) {
		return new DefaultRouterFunctionSpec(routerFunction);
	}

	/**
	 * Integration testing with a "mock" server targeting the given WebHandler.
	 * @param webHandler the handler to test
	 * @return the {@code WebTestClient} builder
	 */
	static MockServerSpec<?> bindToWebHandler(WebHandler webHandler) {
		return new DefaultMockServerSpec(webHandler);
	}

	/**
	 * Complete end-to-end integration tests with actual requests to a running server.
	 * @return the {@code WebTestClient} builder
	 */
	static Builder bindToServer() {
		return new DefaultWebTestClientBuilder();
	}


	/**
	 * Base specification for setting up tests without a server.
	 */
	interface MockServerSpec<B extends MockServerSpec<B>> {

		/**
		 * Register {@link WebFilter} instances to add to the mock server.
		 * @param filter one or more filters
		 */
		<T extends B> T webFilter(WebFilter... filter);

		/**
		 * Provide a session manager instance for the mock server.
		 * <p>By default an instance of
		 * {@link org.springframework.web.server.session.DefaultWebSessionManager
		 * DefaultWebSessionManager} is used.
		 * @param sessionManager the session manager to use
		 */
		<T extends B> T webSessionManager(WebSessionManager sessionManager);

		/**
		 * Shortcut for pre-packaged customizations to the mock server setup.
		 * @param configurer the configurer to apply
		 */
		<T extends B> T apply(MockServerConfigurer configurer);

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
		 * Configure resolvers for custom controller method arguments.
		 * @see WebFluxConfigurer#configureHttpMessageCodecs
		 */
		ControllerSpec argumentResolvers(Consumer<ArgumentResolverConfigurer> configurer);

		/**
		 * Configure custom HTTP message readers and writers or override built-in ones.
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
	 * Specification for customizing router function configuration.
	 */
	interface RouterFunctionSpec extends MockServerSpec<RouterFunctionSpec> {

		/**
		 * Configure handler strategies.
		 */
		RouterFunctionSpec handlerStrategies(HandlerStrategies handlerStrategies);

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
		 */
		Builder exchangeStrategies(ExchangeStrategies strategies);

		/**
		 * Max amount of time to wait for responses.
		 * <p>By default 5 seconds.
		 * @param timeout the response timeout value
		 */
		Builder responseTimeout(Duration timeout);

		/**
		 * Shortcut for pre-packaged customizations to WebTestClient builder.
		 * @param configurer the configurer to apply
		 */
		Builder apply(WebTestClientConfigurer configurer);

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
		 * Manipulate this request's cookies with the given consumer. The
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

	interface RequestHeadersUriSpec<S extends RequestHeadersSpec<S>>
			extends UriSpec<S>, RequestHeadersSpec<S> {
	}

	interface RequestBodyUriSpec extends RequestBodySpec, RequestHeadersUriSpec<RequestBodySpec> {
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
		<B> BodySpec<B, ?> expectBody(ParameterizedTypeReference<B> bodyType);

		/**
		 * Declare expectations on the response body decoded to {@code List<E>}.
		 * @param elementType the expected List element type
		 */
		<E> ListBodySpec<E> expectBodyList(Class<E> elementType);

		/**
		 * Variant of {@link #expectBodyList(Class)} for element types with generics.
		 */
		<E> ListBodySpec<E> expectBodyList(ParameterizedTypeReference<E> elementType);

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
		<T> FluxExchangeResult<T> returnResult(ParameterizedTypeReference<T> elementType);

		/**
		 * Return the exchange result with the body decoded to
		 * {@code Flux<DataBuffer>}. Use this option for infinite streams and
		 * consume the stream with the {@code StepVerifier} from the Reactor Add-Ons.
		 *
		 * @return
		 */
		FluxExchangeResult<DataBuffer> returnResult();
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
		 * Assert the exchange result with the given {@link Consumer}.
		 */
		<T extends S> T consumeWith(Consumer<EntityExchangeResult<B>> consumer);

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
		 * Assert the response body content with the given {@link Consumer}.
		 * @param consumer the consumer for the response body; the input
		 * {@code byte[]} may be {@code null} if there was no response body.
		 */
		BodyContentSpec consumeWith(Consumer<EntityExchangeResult<byte[]>> consumer);

		/**
		 * Return the exchange result with body content as {@code byte[]}.
		 */
		EntityExchangeResult<byte[]> returnResult();

	}

}
