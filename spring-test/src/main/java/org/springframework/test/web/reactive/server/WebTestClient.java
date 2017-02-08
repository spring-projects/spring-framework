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
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.HttpHandler;
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
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
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
	 * @return spec for controller configuration and test client builder
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
	static WebClientSpec bindToApplicationContext(ApplicationContext applicationContext) {
		HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		return new DefaultWebClientSpec(httpHandler);
	}

	/**
	 * Integration testing without a server, targeting WebFlux functional endpoints.
	 * @param routerFunction the RouterFunction to test
	 * @return the {@link WebTestClient} builder
	 */
	static WebClientSpec bindToRouterFunction(RouterFunction<?> routerFunction) {
		HttpWebHandlerAdapter httpHandler = RouterFunctions.toHttpHandler(routerFunction);
		return new DefaultWebClientSpec(httpHandler);
	}

	/**
	 * Complete end-to-end integration tests with actual requests to a running server.
	 * @return the {@link WebTestClient} builder
	 */
	static WebClientSpec bindToServer() {
		return new DefaultWebClientSpec();
	}


	/**
	 * Specification for customizing controller configuration equivalent to, and
	 * internally delegating to, a {@link WebFluxConfigurer}.
	 */
	interface ControllerSpec {

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

		/**
		 * Proceed to configure the {@link WebClient} to test with.
		 */
		WebClientSpec webClientSpec();

		/**
		 * Shortcut to build the {@link WebTestClient}.
		 */
		WebTestClient build();

	}

	/**
	 * Steps for customizing the {@link WebClient} used to test with
	 * internally delegating to a {@link WebClient.Builder}.
	 */
	interface WebClientSpec {

		/**
		 * Configure a base URI as described in
		 * {@link org.springframework.web.reactive.function.client.WebClient#create(String)
		 * WebClient.create(String)}.
		 * @see #defaultUriVariables(Map)
		 * @see #uriBuilderFactory(UriBuilderFactory)
		 */
		WebClientSpec baseUrl(String baseUrl);

		/**
		 * Configure default URI variable values that will be used when expanding
		 * URI templates using a {@link Map}.
		 * @param defaultUriVariables the default values to use
		 * @see #baseUrl(String)
		 * @see #uriBuilderFactory(UriBuilderFactory)
		 */
		WebClientSpec defaultUriVariables(Map<String, ?> defaultUriVariables);

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
		WebClientSpec uriBuilderFactory(UriBuilderFactory uriBuilderFactory);

		/**
		 * Add the given header to all requests that haven't added it.
		 * @param headerName the header name
		 * @param headerValues the header values
		 */
		WebClientSpec defaultHeader(String headerName, String... headerValues);

		/**
		 * Add the given header to all requests that haven't added it.
		 * @param cookieName the cookie name
		 * @param cookieValues the cookie values
		 */
		WebClientSpec defaultCookie(String cookieName, String... cookieValues);

		/**
		 * Configure the {@link ExchangeStrategies} to use.
		 * <p>By default {@link ExchangeStrategies#withDefaults()} is used.
		 * @param strategies the strategies to use
		 */
		WebClientSpec exchangeStrategies(ExchangeStrategies strategies);

		/**
		 * Proceed to building the {@link WebTestClient}.
		 */
		Builder builder();

		/**
		 * Shortcut to build the {@link WebTestClient}.
		 */
		WebTestClient build();

	}

	/**
	 * Build steps to create a {@link WebTestClient}.
	 */
	interface Builder {

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
		 */
		HeaderSpec uri(String uri, Object... uriVariables);

		/**
		 * Specify the URI for the request using a URI template and URI variables.
		 * If a {@link UriBuilderFactory} was configured for the client (e.g.
		 * with a base URI) it will be used to expand the URI template.
		 */
		HeaderSpec uri(String uri, Map<String, ?> uriVariables);

		/**
		 * Build the URI for the request with a {@link UriBuilder} obtained
		 * through the {@link UriBuilderFactory} configured for this client.
		 */
		HeaderSpec uri(Function<UriBuilder, URI> uriFunction);

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
		 * @return options for asserting the response with
		 */
		ExchangeActions exchange();

		/**
		 * Set the body of the request to the given {@code BodyInserter} and
		 * perform the request.
		 * @param inserter the {@code BodyInserter} that writes to the request
		 * @param <T> the type contained in the body
		 * @return options for asserting the response with
		 */
		<T> ExchangeActions exchange(BodyInserter<T, ? super ClientHttpRequest> inserter);

		/**
		 * Set the body of the request to the given {@code Publisher} and
		 * perform the request.
		 * @param publisher the {@code Publisher} to write to the request
		 * @param elementClass the class of elements contained in the publisher
		 * @param <T> the type of the elements contained in the publisher
		 * @param <S> the type of the {@code Publisher}
		 * @return options for asserting the response with
		 */
		<T, S extends Publisher<T>> ExchangeActions exchange(S publisher, Class<T> elementClass);
	}

}
