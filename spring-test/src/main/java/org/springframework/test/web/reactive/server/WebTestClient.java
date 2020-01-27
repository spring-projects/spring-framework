/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.hamcrest.Matcher;
import org.reactivestreams.Publisher;

import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
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
 * Client for testing web servers that uses {@link WebClient} internally to
 * perform requests while also providing a fluent API to verify responses.
 * This client can connect to any server over HTTP, or to a WebFlux application
 * via mock request and response objects.
 *
 * <p>Use one of the bindToXxx methods to create an instance. For example:
 * <ul>
 * <li>{@link #bindToController(Object...)}
 * <li>{@link #bindToRouterFunction(RouterFunction)}
 * <li>{@link #bindToApplicationContext(ApplicationContext)}
 * <li>{@link #bindToServer()}
 * <li>...
 * </ul>
 *
 * <p><strong>Warning</strong>: {@code WebTestClient} is not usable yet in
 * Kotlin due to a <a href="https://youtrack.jetbrains.com/issue/KT-5464">type inference issue</a>
 * which is expected to be fixed as of Kotlin 1.3. You can watch
 * <a href="https://github.com/spring-projects/spring-framework/issues/20606">gh-20606</a>
 * for up-to-date information. Meanwhile, the proposed alternative is to use
 * directly {@link WebClient} with its Reactor and Spring Kotlin extensions to
 * perform integration tests on an embedded WebFlux server.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @see StatusAssertions
 * @see HeaderAssertions
 * @see JsonPathAssertions
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


	// Static factory methods

	/**
	 * Use this server setup to test one `@Controller` at a time.
	 * This option loads the default configuration of
	 * {@link org.springframework.web.reactive.config.EnableWebFlux @EnableWebFlux}.
	 * There are builder methods to customize the Java config. The resulting
	 * WebFlux application will be tested without an HTTP server using a mock
	 * request and response.
	 * @param controllers one or more controller instances to tests
	 * (specified {@code Class} will be turned into instance)
	 * @return chained API to customize server and client config; use
	 * {@link MockServerSpec#configureClient()} to transition to client config
	 */
	static ControllerSpec bindToController(Object... controllers) {
		return new DefaultControllerSpec(controllers);
	}

	/**
	 * Use this option to set up a server from a {@link RouterFunction}.
	 * Internally the provided configuration is passed to
	 * {@code RouterFunctions#toWebHandler}. The resulting WebFlux application
	 * will be tested without an HTTP server using a mock request and response.
	 * @param routerFunction the RouterFunction to test
	 * @return chained API to customize server and client config; use
	 * {@link MockServerSpec#configureClient()} to transition to client config
	 */
	static RouterFunctionSpec bindToRouterFunction(RouterFunction<?> routerFunction) {
		return new DefaultRouterFunctionSpec(routerFunction);
	}

	/**
	 * Use this option to setup a server from the Spring configuration of your
	 * application, or some subset of it. Internally the provided configuration
	 * is passed to {@code WebHttpHandlerBuilder} to set up the request
	 * processing chain. The resulting WebFlux application will be tested
	 * without an HTTP server using a mock request and response.
	 * <p>Consider using the TestContext framework and
	 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration}
	 * in order to efficiently load and inject the Spring configuration into the
	 * test class.
	 * @param applicationContext the Spring context
	 * @return chained API to customize server and client config; use
	 * {@link MockServerSpec#configureClient()} to transition to client config
	 */
	static MockServerSpec<?> bindToApplicationContext(ApplicationContext applicationContext) {
		return new ApplicationContextSpec(applicationContext);
	}

	/**
	 * Integration testing with a "mock" server targeting the given WebHandler.
	 * @param webHandler the handler to test
	 * @return chained API to customize server and client config; use
	 * {@link MockServerSpec#configureClient()} to transition to client config
	 */
	static MockServerSpec<?> bindToWebHandler(WebHandler webHandler) {
		return new DefaultMockServerSpec(webHandler);
	}

	/**
	 * This server setup option allows you to connect to a running server via
	 * Reactor Netty.
	 * <p><pre class="code">
	 * WebTestClient client = WebTestClient.bindToServer()
	 *         .baseUrl("http://localhost:8080")
	 *         .build();
	 * </pre>
	 * @return chained API to customize client config
	 */
	static Builder bindToServer() {
		return new DefaultWebTestClientBuilder();
	}

	/**
	 * A variant of {@link #bindToServer()} with a pre-configured connector.
	 * <p><pre class="code">
	 * WebTestClient client = WebTestClient.bindToServer()
	 *         .baseUrl("http://localhost:8080")
	 *         .build();
	 * </pre>
	 * @return chained API to customize client config
	 * @since 5.0.2
	 */
	static Builder bindToServer(ClientHttpConnector connector) {
		return new DefaultWebTestClientBuilder(connector);
	}


	/**
	 * Base specification for setting up tests without a server.
	 *
	 * @param <B> a self reference to the builder type
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
		 * Register one or more {@link org.springframework.web.bind.annotation.ControllerAdvice}
		 * instances to be used in tests (specified {@code Class} will be turned into instance).
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
	 * Steps for customizing the {@link WebClient} used to test with,
	 * internally delegating to a
	 * {@link org.springframework.web.reactive.function.client.WebClient.Builder
	 * WebClient.Builder}.
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
		 * Configure the codecs for the {@code WebClient} in the
		 * {@link #exchangeStrategies(ExchangeStrategies) underlying}
		 * {@code ExchangeStrategies}.
		 * @param configurer the configurer to apply
		 * @since 5.1.13
		 */
		Builder codecs(Consumer<ClientCodecConfigurer> configurer);

		/**
		 * Configure the {@link ExchangeStrategies} to use.
		 * <p>For most cases, prefer using {@link #codecs(Consumer)} which allows
		 * customizing the codecs in the {@code ExchangeStrategies} rather than
		 * replace them. That ensures multiple parties can contribute to codecs
		 * configuration.
		 * <p>By default this is set to {@link ExchangeStrategies#withDefaults()}.
		 * @param strategies the strategies to use
		 */
		Builder exchangeStrategies(ExchangeStrategies strategies);

		/**
		 * Customize the strategies configured via
		 * {@link #exchangeStrategies(ExchangeStrategies)}. This method is
		 * designed for use in scenarios where multiple parties wish to update
		 * the {@code ExchangeStrategies}.
		 * @deprecated as of 5.1.13 in favor of {@link #codecs(Consumer)}
		 */
		@Deprecated
		Builder exchangeStrategies(Consumer<ExchangeStrategies.Builder> configurer);

		/**
		 * Max amount of time to wait for responses.
		 * <p>By default 5 seconds.
		 * @param timeout the response timeout value
		 */
		Builder responseTimeout(Duration timeout);

		/**
		 * Apply the given configurer to this builder instance.
		 * <p>This can be useful for applying pre-packaged customizations.
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
	 *
	 * @param <S> a self reference to the spec type
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
	 *
	 * @param <S> a self reference to the spec type
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


	/**
	 * Specification for providing body of a request.
	 */
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
		 * Set the body to the given {@code Object} value. This method invokes the
		 * {@link WebClient.RequestBodySpec#bodyValue(Object) bodyValue} method
		 * on the underlying {@code WebClient}.
		 * @param body the value to write to the request body
		 * @return spec for further declaration of the request
		 * @since 5.2
		 */
		RequestHeadersSpec<?> bodyValue(Object body);

		/**
		 * Set the body from the given {@code Publisher}. Shortcut for
		 * {@link #body(BodyInserter)} with a
		 * {@linkplain BodyInserters#fromPublisher Publisher inserter}.
		 * @param publisher the request body data
		 * @param elementClass the class of elements contained in the publisher
		 * @param <T> the type of the elements contained in the publisher
		 * @param <S> the type of the {@code Publisher}
		 * @return spec for further declaration of the request
		 */
		<T, S extends Publisher<T>> RequestHeadersSpec<?> body(S publisher, Class<T> elementClass);

		/**
		 * Variant of {@link #body(Publisher, Class)} that allows providing
		 * element type information with generics.
		 * @param publisher the request body data
		 * @param elementTypeRef the type reference of elements contained in the publisher
		 * @param <T> the type of the elements contained in the publisher
		 * @param <S> the type of the {@code Publisher}
		 * @return spec for further declaration of the request
		 * @since 5.2
		 */
		<T, S extends Publisher<T>> RequestHeadersSpec<?> body(
				S publisher, ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * Set the body from the given producer. This method invokes the
		 * {@link WebClient.RequestBodySpec#body(Object, Class)} method on the
		 * underlying {@code WebClient}.
		 * @param producer the producer to write to the request. This must be a
		 * {@link Publisher} or another producer adaptable to a
		 * {@code Publisher} via {@link ReactiveAdapterRegistry}
		 * @param elementClass the class of elements contained in the producer
		 * @return spec for further declaration of the request
		 * @since 5.2
		 */
		RequestHeadersSpec<?> body(Object producer, Class<?> elementClass);

		/**
		 * Set the body from the given producer. This method invokes the
		 * {@link WebClient.RequestBodySpec#body(Object, ParameterizedTypeReference)}
		 * method on the underlying {@code WebClient}.
		 * @param producer the producer to write to the request. This must be a
		 * {@link Publisher} or another producer adaptable to a
		 * {@code Publisher} via {@link ReactiveAdapterRegistry}
		 * @param elementTypeRef the type reference of elements contained in the producer
		 * @return spec for further declaration of the request
		 * @since 5.2
		 */
		RequestHeadersSpec<?> body(Object producer, ParameterizedTypeReference<?> elementTypeRef);

		/**
		 * Set the body of the request to the given {@code BodyInserter}.
		 * This method invokes the
		 * {@link WebClient.RequestBodySpec#body(BodyInserter)} method on the
		 * underlying {@code WebClient}.
		 * @param inserter the body inserter to use
		 * @return spec for further declaration of the request
		 * @see org.springframework.web.reactive.function.BodyInserters
		 */
		RequestHeadersSpec<?> body(BodyInserter<?, ? super ClientHttpRequest> inserter);

		/**
		 * Shortcut for {@link #body(BodyInserter)} with a
		 * {@linkplain BodyInserters#fromValue value inserter}.
		 * As of 5.2 this method delegates to {@link #bodyValue(Object)}.
		 * @deprecated as of Spring Framework 5.2 in favor of {@link #bodyValue(Object)}
		 */
		@Deprecated
		RequestHeadersSpec<?> syncBody(Object body);
	}


	/**
	 * Specification for providing request headers and the URI of a request.
	 *
	 * @param <S> a self reference to the spec type
	 */
	interface RequestHeadersUriSpec<S extends RequestHeadersSpec<S>> extends UriSpec<S>, RequestHeadersSpec<S> {
	}

	/**
	 * Specification for providing the body and the URI of a request.
	 */
	interface RequestBodyUriSpec extends RequestBodySpec, RequestHeadersUriSpec<RequestBodySpec> {
	}


	/**
	 * Chained API for applying assertions to a response.
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
		 * Consume and decode the response body to a single object of type
		 * {@code <B>} and then apply assertions.
		 * @param bodyType the expected body type
		 */
		<B> BodySpec<B, ?> expectBody(Class<B> bodyType);

		/**
		 * Alternative to {@link #expectBody(Class)} that accepts information
		 * about a target type with generics.
		 */
		<B> BodySpec<B, ?> expectBody(ParameterizedTypeReference<B> bodyType);

		/**
		 * Consume and decode the response body to {@code List<E>} and then apply
		 * List-specific assertions.
		 * @param elementType the expected List element type
		 */
		<E> ListBodySpec<E> expectBodyList(Class<E> elementType);

		/**
		 * Alternative to {@link #expectBodyList(Class)} that accepts information
		 * about a target type with generics.
		 */
		<E> ListBodySpec<E> expectBodyList(ParameterizedTypeReference<E> elementType);

		/**
		 * Consume and decode the response body to {@code byte[]} and then apply
		 * assertions on the raw content (e.g. isEmpty, JSONPath, etc.)
		 */
		BodyContentSpec expectBody();

		/**
		 * Exit the chained API and consume the response body externally. This
		 * is useful for testing infinite streams (e.g. SSE) where you need to
		 * to assert decoded objects as they come and then cancel at some point
		 * when test objectives are met. Consider using {@code StepVerifier}
		 * from {@literal "reactor-test"} to assert the {@code Flux<T>} stream
		 * of decoded objects.
		 *
		 * <p><strong>Note:</strong> Do not use this option for cases where there
		 * is no content (e.g. 204, 4xx) or you're not interested in the content.
		 * For such cases you can use {@code expectBody().isEmpty()} or
		 * {@code expectBody(Void.class)} which ensures that resources are
		 * released regardless of whether the response has content or not.
		 */
		<T> FluxExchangeResult<T> returnResult(Class<T> elementClass);

		/**
		 * Alternative to {@link #returnResult(Class)} that accepts information
		 * about a target type with generics.
		 */
		<T> FluxExchangeResult<T> returnResult(ParameterizedTypeReference<T> elementTypeRef);
	}


	/**
	 * Spec for expectations on the response body decoded to a single Object.
	 *
	 * @param <S> a self reference to the spec type
	 * @param <B> the body type
	 */
	interface BodySpec<B, S extends BodySpec<B, S>> {

		/**
		 * Assert the extracted body is equal to the given value.
		 */
		<T extends S> T isEqualTo(B expected);

		/**
		 * Assert the extracted body with a {@link Matcher}.
		 * @since 5.1
		 */
		<T extends S> T value(Matcher<B> matcher);

		/**
		 * Transform the extracted the body with a function, e.g. extracting a
		 * property, and assert the mapped value with a {@link Matcher}.
		 * @since 5.1
		 */
		<T extends S, R> T value(Function<B, R> bodyMapper, Matcher<R> matcher);

		/**
		 * Assert the extracted body with a {@link Consumer}.
		 * @since 5.1
		 */
		<T extends S> T value(Consumer<B> consumer);

		/**
		 * Assert the exchange result with the given {@link Consumer}.
		 */
		<T extends S> T consumeWith(Consumer<EntityExchangeResult<B>> consumer);

		/**
		 * Exit the chained API and return an {@code ExchangeResult} with the
		 * decoded response content.
		 */
		EntityExchangeResult<B> returnResult();
	}


	/**
	 * Spec for expectations on the response body decoded to a List.
	 *
	 * @param <E> the body list element type
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
		 * <a href="https://jsonassert.skyscreamer.org/">JSONassert</a> library
		 * on to be on the classpath.
		 * @param expectedJson the expected JSON content.
		 */
		BodyContentSpec json(String expectedJson);

		/**
		 * Parse expected and actual response content as XML and assert that
		 * the two are "similar", i.e. they contain the same elements and
		 * attributes regardless of order.
		 * <p>Use of this method requires the
		 * <a href="https://github.com/xmlunit/xmlunit">XMLUnit</a> library on
		 * the classpath.
		 * @param expectedXml the expected JSON content.
		 * @since 5.1
		 * @see org.springframework.test.util.XmlExpectationsHelper#assertXmlEqual(String, String)
		 */
		BodyContentSpec xml(String expectedXml);

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
		 * Access to response body assertions using an XPath expression to
		 * inspect a specific subset of the body.
		 * <p>The XPath expression can be a parameterized string using
		 * formatting specifiers as defined in {@link String#format}.
		 * @param expression the XPath expression
		 * @param args arguments to parameterize the expression
		 * @since 5.1
		 * @see #xpath(String, Map, Object...)
		 */
		default XpathAssertions xpath(String expression, Object... args) {
			return xpath(expression, null, args);
		}

		/**
		 * Access to response body assertions with specific namespaces using an
		 * XPath expression to inspect a specific subset of the body.
		 * <p>The XPath expression can be a parameterized string using
		 * formatting specifiers as defined in {@link String#format}.
		 * @param expression the XPath expression
		 * @param namespaces namespaces to use
		 * @param args arguments to parameterize the expression
		 * @since 5.1
		 */
		XpathAssertions xpath(String expression, @Nullable Map<String, String> namespaces, Object... args);

		/**
		 * Assert the response body content with the given {@link Consumer}.
		 * @param consumer the consumer for the response body; the input
		 * {@code byte[]} may be {@code null} if there was no response body.
		 */
		BodyContentSpec consumeWith(Consumer<EntityExchangeResult<byte[]>> consumer);

		/**
		 * Exit the chained API and return an {@code ExchangeResult} with the
		 * raw response content.
		 */
		EntityExchangeResult<byte[]> returnResult();
	}

}
