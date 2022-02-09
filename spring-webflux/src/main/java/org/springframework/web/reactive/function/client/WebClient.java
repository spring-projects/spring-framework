/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Non-blocking, reactive client to perform HTTP requests, exposing a fluent,
 * reactive API over underlying HTTP client libraries such as Reactor Netty.
 *
 * <p>Use static factory methods {@link #create()} or {@link #create(String)},
 * or {@link WebClient#builder()} to prepare an instance.
 *
 * <p>For examples with a response body see:
 * <ul>
 * <li>{@link RequestHeadersSpec#retrieve() retrieve()}
 * <li>{@link RequestHeadersSpec#exchangeToMono(Function) exchangeToMono()}
 * <li>{@link RequestHeadersSpec#exchangeToFlux(Function) exchangeToFlux()}
 * </ul>
 * <p>For examples with a request body see:
 * <ul>
 * <li>{@link RequestBodySpec#bodyValue(Object) bodyValue(Object)}
 * <li>{@link RequestBodySpec#body(Publisher, Class) body(Publisher,Class)}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.0
 */
public interface WebClient {

	/**
	 * Start building an HTTP GET request.
	 * @return a spec for specifying the target URL
	 */
	RequestHeadersUriSpec<?> get();

	/**
	 * Start building an HTTP HEAD request.
	 * @return a spec for specifying the target URL
	 */
	RequestHeadersUriSpec<?> head();

	/**
	 * Start building an HTTP POST request.
	 * @return a spec for specifying the target URL
	 */
	RequestBodyUriSpec post();

	/**
	 * Start building an HTTP PUT request.
	 * @return a spec for specifying the target URL
	 */
	RequestBodyUriSpec put();

	/**
	 * Start building an HTTP PATCH request.
	 * @return a spec for specifying the target URL
	 */
	RequestBodyUriSpec patch();

	/**
	 * Start building an HTTP DELETE request.
	 * @return a spec for specifying the target URL
	 */
	RequestHeadersUriSpec<?> delete();

	/**
	 * Start building an HTTP OPTIONS request.
	 * @return a spec for specifying the target URL
	 */
	RequestHeadersUriSpec<?> options();

	/**
	 * Start building a request for the given {@code HttpMethod}.
	 * @return a spec for specifying the target URL
	 */
	RequestBodyUriSpec method(HttpMethod method);


	/**
	 * Return a builder to create a new {@code WebClient} whose settings are
	 * replicated from the current {@code WebClient}.
	 */
	Builder mutate();


	// Static, factory methods

	/**
	 * Create a new {@code WebClient} with Reactor Netty by default.
	 * @see #create(String)
	 * @see #builder()
	 */
	static WebClient create() {
		return new DefaultWebClientBuilder().build();
	}

	/**
	 * Variant of {@link #create()} that accepts a default base URL. For more
	 * details see {@link Builder#baseUrl(String) Builder.baseUrl(String)}.
	 * @param baseUrl the base URI for all requests
	 * @see #builder()
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
	 * A mutable builder for creating a {@link WebClient}.
	 */
	interface Builder {

		/**
		 * Configure a base URL for requests. Effectively a shortcut for:
		 * <p>
		 * <pre class="code">
		 * String baseUrl = "https://abc.go.com/v1";
		 * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
		 * WebClient client = WebClient.builder().uriBuilderFactory(factory).build();
		 * </pre>
		 * <p>The {@code DefaultUriBuilderFactory} is used to prepare the URL
		 * for every request with the given base URL, unless the URL request
		 * for a given URL is absolute in which case the base URL is ignored.
		 * <p><strong>Note:</strong> this method is mutually exclusive with
		 * {@link #uriBuilderFactory(UriBuilderFactory)}. If both are used, the
		 * baseUrl value provided here will be ignored.
		 * @see DefaultUriBuilderFactory#DefaultUriBuilderFactory(String)
		 * @see #uriBuilderFactory(UriBuilderFactory)
		 */
		Builder baseUrl(String baseUrl);

		/**
		 * Configure default URL variable values to use when expanding URI
		 * templates with a {@link Map}. Effectively a shortcut for:
		 * <p>
		 * <pre class="code">
		 * Map&lt;String, ?&gt; defaultVars = ...;
		 * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		 * factory.setDefaultVariables(defaultVars);
		 * WebClient client = WebClient.builder().uriBuilderFactory(factory).build();
		 * </pre>
		 * <p><strong>Note:</strong> this method is mutually exclusive with
		 * {@link #uriBuilderFactory(UriBuilderFactory)}. If both are used, the
		 * defaultUriVariables value provided here will be ignored.
		 * @see DefaultUriBuilderFactory#setDefaultUriVariables(Map)
		 * @see #uriBuilderFactory(UriBuilderFactory)
		 */
		Builder defaultUriVariables(Map<String, ?> defaultUriVariables);

		/**
		 * Provide a pre-configured {@link UriBuilderFactory} instance. This is
		 * an alternative to, and effectively overrides the following shortcut
		 * properties:
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
		 * Global option to specify a header to be added to every request,
		 * if the request does not already contain such a header.
		 * @param header the header name
		 * @param values the header values
		 */
		Builder defaultHeader(String header, String... values);

		/**
		 * Provides access to every {@link #defaultHeader(String, String...)}
		 * declared so far with the possibility to add, replace, or remove.
		 * @param headersConsumer the consumer
		 */
		Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Global option to specify a cookie to be added to every request,
		 * if the request does not already contain such a cookie.
		 * @param cookie the cookie name
		 * @param values the cookie values
		 */
		Builder defaultCookie(String cookie, String... values);

		/**
		 * Provides access to every {@link #defaultCookie(String, String...)}
		 * declared so far with the possibility to add, replace, or remove.
		 * @param cookiesConsumer a function that consumes the cookies map
		 */
		Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

		/**
		 * Provide a consumer to customize every request being built.
		 * @param defaultRequest the consumer to use for modifying requests
		 * @since 5.1
		 */
		Builder defaultRequest(Consumer<RequestHeadersSpec<?>> defaultRequest);

		/**
		 * Add the given filter to the end of the filter chain.
		 * @param filter the filter to be added to the chain
		 */
		Builder filter(ExchangeFilterFunction filter);

		/**
		 * Manipulate the filters with the given consumer. The list provided to
		 * the consumer is "live", so that the consumer can be used to remove
		 * filters, change ordering, etc.
		 * @param filtersConsumer a function that consumes the filter list
		 * @return this builder
		 */
		Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer);

		/**
		 * Configure the {@link ClientHttpConnector} to use. This is useful for
		 * plugging in and/or customizing options of the underlying HTTP client
		 * library (e.g. SSL).
		 * <p>By default this is set to
		 * {@link org.springframework.http.client.reactive.ReactorClientHttpConnector
		 * ReactorClientHttpConnector}.
		 * @param connector the connector to use
		 */
		Builder clientConnector(ClientHttpConnector connector);

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
		 * Provide an {@link ExchangeFunction} pre-configured with
		 * {@link ClientHttpConnector} and {@link ExchangeStrategies}.
		 * <p>This is an alternative to, and effectively overrides
		 * {@link #clientConnector}, and
		 * {@link #exchangeStrategies(ExchangeStrategies)}.
		 * @param exchangeFunction the exchange function to use
		 */
		Builder exchangeFunction(ExchangeFunction exchangeFunction);

		/**
		 * Apply the given {@code Consumer} to this builder instance.
		 * <p>This can be useful for applying pre-packaged customizations.
		 * @param builderConsumer the consumer to apply
		 */
		Builder apply(Consumer<Builder> builderConsumer);

		/**
		 * Clone this {@code WebClient.Builder}.
		 */
		Builder clone();

		/**
		 * Builder the {@link WebClient} instance.
		 */
		WebClient build();
	}


	/**
	 * Contract for specifying the URI for a request.
	 * @param <S> a self reference to the spec type
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
		 * Specify the URI starting with a URI template and finishing off with a
		 * {@link UriBuilder} created from the template.
		 * @since 5.2
		 */
		S uri(String uri, Function<UriBuilder, URI> uriFunction);

		/**
		 * Specify the URI by through a {@link UriBuilder}.
		 * @see #uri(String, Function)
		 */
		S uri(Function<UriBuilder, URI> uriFunction);
	}


	/**
	 * Contract for specifying request headers leading up to the exchange.
	 * @param <S> a self reference to the spec type
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
		 * Provides access to every cookie declared so far with the possibility
		 * to add, replace, or remove values.
		 * @param cookiesConsumer the consumer to provide access to
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
		 * Provides access to every header declared so far with the possibility
		 * to add, replace, or remove values.
		 * @param headersConsumer the consumer to provide access to
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
		 * Provides access to every attribute declared so far with the
		 * possibility to add, replace, or remove values.
		 * @param attributesConsumer the consumer to provide access to
		 * @return this builder
		 */
		S attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * Provide a function to populate the Reactor {@code Context}.
		 * @param contextModifier the function to modify the context with
		 * @since 5.3.1
		 * @deprecated in 5.3.2 to be removed soon after; this method cannot
		 * provide context to downstream (nested or subsequent) requests and is
		 * of limited value.
		 */
		@Deprecated
		S context(Function<Context, Context> contextModifier);

		/**
		 * Callback for access to the {@link ClientHttpRequest} that in turn
		 * provides access to the native request of the underlying HTTP library.
		 * This could be useful for setting advanced, per-request options that
		 * exposed by the underlying library.
		 * @param requestConsumer a consumer to access the
		 * {@code ClientHttpRequest} with
		 * @return {@code ResponseSpec} to specify how to decode the body
		 * @since 5.3
		 */
		S httpRequest(Consumer<ClientHttpRequest> requestConsumer);

		/**
		 * Proceed to declare how to extract the response. For example to extract
		 * a {@link ResponseEntity} with status, headers, and body:
		 * <p><pre>
		 * Mono&lt;ResponseEntity&lt;Person&gt;&gt; entityMono = client.get()
		 *     .uri("/persons/1")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .retrieve()
		 *     .toEntity(Person.class);
		 * </pre>
		 * <p>Or if interested only in the body:
		 * <p><pre>
		 * Mono&lt;Person&gt; entityMono = client.get()
		 *     .uri("/persons/1")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .retrieve()
		 *     .bodyToMono(Person.class);
		 * </pre>
		 * <p>By default, 4xx and 5xx responses result in a
		 * {@link WebClientResponseException}. To customize error handling, use
		 * {@link ResponseSpec#onStatus(Predicate, Function) onStatus} handlers.
		 */
		ResponseSpec retrieve();

		/**
		 * An alternative to {@link #retrieve()} that provides more control via
		 * access to the {@link ClientResponse}. This can be useful for advanced
		 * scenarios, for example to decode the response differently depending
		 * on the response status:
		 * <p><pre>
		 * Mono&lt;Person&gt; entityMono = client.get()
		 *     .uri("/persons/1")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .exchangeToMono(response -&gt; {
		 *         if (response.statusCode().equals(HttpStatus.OK)) {
		 *             return response.bodyToMono(Person.class);
		 *         }
		 *         else {
		 *             return response.createError();
		 *         }
		 *     });
		 * </pre>
		 * <p><strong>Note:</strong> After the returned {@code Mono} completes,
		 * the response body is automatically released if it hasn't been consumed.
		 * If the response content is needed, the provided function must declare
		 * how to decode it.
		 * @param responseHandler the function to handle the response with
		 * @param <V> the type of Object the response will be transformed to
		 * @return a {@code Mono} produced from the response
		 * @since 5.3
		 */
		<V> Mono<V> exchangeToMono(Function<ClientResponse, ? extends Mono<V>> responseHandler);

		/**
		 * An alternative to {@link #retrieve()} that provides more control via
		 * access to the {@link ClientResponse}. This can be useful for advanced
		 * scenarios, for example to decode the response differently depending
		 * on the response status:
		 * <p><pre>
		 * Flux&lt;Person&gt; entityMono = client.get()
		 *     .uri("/persons")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .exchangeToFlux(response -&gt; {
		 *         if (response.statusCode().equals(HttpStatus.OK)) {
		 *             return response.bodyToFlux(Person.class);
		 *         }
		 *         else {
		 *             return response.createError().flux();
		 *         }
		 *     });
		 * </pre>
		 * <p><strong>Note:</strong> After the returned {@code Flux} completes,
		 * the response body is automatically released if it hasn't been consumed.
		 * If the response content is needed, the provided function must declare
		 * how to decode it.
		 * @param responseHandler the function to handle the response with
		 * @param <V> the type of Objects the response will be transformed to
		 * @return a {@code Flux} of Objects produced from the response
		 * @since 5.3
		 */
		<V> Flux<V> exchangeToFlux(Function<ClientResponse, ? extends Flux<V>> responseHandler);

		/**
		 * Perform the HTTP request and return a {@link ClientResponse} with the
		 * response status and headers. You can then use methods of the response
		 * to consume the body:
		 * <p><pre>
		 * Mono&lt;Person&gt; mono = client.get()
		 *     .uri("/persons/1")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .exchange()
		 *     .flatMap(response -&gt; response.bodyToMono(Person.class));
		 *
		 * Flux&lt;Person&gt; flux = client.get()
		 *     .uri("/persons")
		 *     .accept(MediaType.APPLICATION_STREAM_JSON)
		 *     .exchange()
		 *     .flatMapMany(response -&gt; response.bodyToFlux(Person.class));
		 * </pre>
		 * <p><strong>NOTE:</strong> Unlike {@link #retrieve()}, when using
		 * {@code exchange()}, it is the responsibility of the application to
		 * consume any response content regardless of the scenario (success,
		 * error, unexpected data, etc). Not doing so can cause a memory leak.
		 * See {@link ClientResponse} for a list of all the available options
		 * for consuming the body. Generally prefer using {@link #retrieve()}
		 * unless you have a good reason to use {@code exchange()} which does
		 * allow to check the response status and headers before deciding how or
		 * if to consume the response.
		 * @return a {@code Mono} for the response
		 * @see #retrieve()
		 * @deprecated since 5.3 due to the possibility to leak memory and/or
		 * connections; please, use {@link #exchangeToMono(Function)},
		 * {@link #exchangeToFlux(Function)}; consider also using
		 * {@link #retrieve()} which provides access to the response status
		 * and headers via {@link ResponseEntity} along with error status
		 * handling.
		 */
		@Deprecated
		Mono<ClientResponse> exchange();
	}


	/**
	 * Contract for specifying request headers and body leading up to the exchange.
	 */
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
		 * Shortcut for {@link #body(BodyInserter)} with a
		 * {@linkplain BodyInserters#fromValue value inserter}.
		 * For example:
		 * <p><pre class="code">
		 * Person person = ... ;
		 *
		 * Mono&lt;Void&gt; result = client.post()
		 *     .uri("/persons/{id}", id)
		 *     .contentType(MediaType.APPLICATION_JSON)
		 *     .bodyValue(person)
		 *     .retrieve()
		 *     .bodyToMono(Void.class);
		 * </pre>
		 * <p>For multipart requests consider providing
		 * {@link org.springframework.util.MultiValueMap MultiValueMap} prepared
		 * with {@link org.springframework.http.client.MultipartBodyBuilder
		 * MultipartBodyBuilder}.
		 * @param body the value to write to the request body
		 * @return this builder
		 * @throws IllegalArgumentException if {@code body} is a
		 * {@link Publisher} or producer known to {@link ReactiveAdapterRegistry}
		 * @since 5.2
		 */
		RequestHeadersSpec<?> bodyValue(Object body);

		/**
		 * Shortcut for {@link #body(BodyInserter)} with a
		 * {@linkplain BodyInserters#fromPublisher Publisher inserter}.
		 * For example:
		 * <p><pre>
		 * Mono&lt;Person&gt; personMono = ... ;
		 *
		 * Mono&lt;Void&gt; result = client.post()
		 *     .uri("/persons/{id}", id)
		 *     .contentType(MediaType.APPLICATION_JSON)
		 *     .body(personMono, Person.class)
		 *     .retrieve()
		 *     .bodyToMono(Void.class);
		 * </pre>
		 * @param publisher the {@code Publisher} to write to the request
		 * @param elementClass the type of elements published
		 * @param <T> the type of the elements contained in the publisher
		 * @param <P> the type of the {@code Publisher}
		 * @return this builder
		 */
		<T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher, Class<T> elementClass);

		/**
		 * Variant of {@link #body(Publisher, Class)} that allows providing
		 * element type information with generics.
		 * @param publisher the {@code Publisher} to write to the request
		 * @param elementTypeRef the type of elements published
		 * @param <T> the type of the elements contained in the publisher
		 * @param <P> the type of the {@code Publisher}
		 * @return this builder
		 */
		<T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher,
				ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * Variant of {@link #body(Publisher, Class)} that allows using any
		 * producer that can be resolved to {@link Publisher} via
		 * {@link ReactiveAdapterRegistry}.
		 * @param producer the producer to write to the request
		 * @param elementClass the type of elements produced
		 * @return this builder
		 * @since 5.2
		 */
		RequestHeadersSpec<?> body(Object producer, Class<?> elementClass);

		/**
		 * Variant of {@link #body(Publisher, ParameterizedTypeReference)} that
		 * allows using any producer that can be resolved to {@link Publisher}
		 * via {@link ReactiveAdapterRegistry}.
		 * @param producer the producer to write to the request
		 * @param elementTypeRef the type of elements produced
		 * @return this builder
		 * @since 5.2
		 */
		RequestHeadersSpec<?> body(Object producer, ParameterizedTypeReference<?> elementTypeRef);

		/**
		 * Set the body of the request using the given body inserter.
		 * See {@link BodyInserters} for built-in {@link BodyInserter} implementations.
		 * @param inserter the body inserter to use for the request body
		 * @return this builder
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
	 * Contract for specifying response operations following the exchange.
	 */
	interface ResponseSpec {

		/**
		 * Provide a function to map specific error status codes to an error
		 * signal to be propagated downstream instead of the response.
		 * <p>By default, if there are no matching status handlers, responses
		 * with status codes &gt;= 400 are mapped to
		 * {@link WebClientResponseException} which is created with
		 * {@link ClientResponse#createException()}.
		 * <p>To suppress the treatment of a status code as an error and process
		 * it as a normal response, return {@code Mono.empty()} from the function.
		 * The response will then propagate downstream to be processed.
		 * <p>To ignore an error response completely, and propagate neither
		 * response nor error, use a {@link ExchangeFilterFunction filter}, or
		 * add {@code onErrorResume} downstream, for example:
		 * <pre class="code">
		 * webClient.get()
		 *     .uri("https://abc.com/account/123")
		 *     .retrieve()
		 *     .bodyToMono(Account.class)
		 *     .onErrorResume(WebClientResponseException.class,
		 *          ex -&gt; ex.getRawStatusCode() == 404 ? Mono.empty() : Mono.error(ex));
		 * </pre>
		 * @param statusPredicate to match responses with
		 * @param exceptionFunction to map the response to an error signal
		 * @return this builder
		 * @see ClientResponse#createException()
		 */
		ResponseSpec onStatus(Predicate<HttpStatus> statusPredicate,
				Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction);

		/**
		 * Variant of {@link #onStatus(Predicate, Function)} that works with
		 * raw status code values. This is useful for custom status codes.
		 * @param statusCodePredicate to match responses with
		 * @param exceptionFunction to map the response to an error signal
		 * @return this builder
		 * @since 5.1.9
		 */
		ResponseSpec onRawStatus(IntPredicate statusCodePredicate,
				Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction);

		/**
		 * Decode the body to the given target type. For an error response (status
		 * code of 4xx or 5xx), the {@code Mono} emits a {@link WebClientException}.
		 * Use {@link #onStatus(Predicate, Function)} to customize error response
		 * handling.
		 * @param elementClass the type to decode to
		 * @param <T> the target body type
		 * @return the decoded body
		 */
		<T> Mono<T> bodyToMono(Class<T> elementClass);

		/**
		 * Variant of {@link #bodyToMono(Class)} with a {@link ParameterizedTypeReference}.
		 * @param elementTypeRef the type to decode to
		 * @param <T> the target body type
		 * @return the decoded body
		 */
		<T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * Decode the body to a {@link Flux} with elements of the given type.
		 * For an error response (status code of 4xx or 5xx), the {@code Mono}
		 * emits a {@link WebClientException}. Use {@link #onStatus(Predicate, Function)}
		 * to customize error response handling.
		 * @param elementClass the type of element to decode to
		 * @param <T> the body element type
		 * @return the decoded body
		 */
		<T> Flux<T> bodyToFlux(Class<T> elementClass);

		/**
		 * Variant of {@link #bodyToMono(Class)} with a {@link ParameterizedTypeReference}.
		 * @param elementTypeRef the type of element to decode to
		 * @param <T> the body element type
		 * @return the decoded body
		 */
		<T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * Return a {@code ResponseEntity} with the body decoded to an Object of
		 * the given type. For an error response (status code of 4xx or 5xx), the
		 * {@code Mono} emits a {@link WebClientException}. Use
		 * {@link #onStatus(Predicate, Function)} to customize error response handling.
		 * @param bodyClass the expected response body type
		 * @param <T> response body type
		 * @return the {@code ResponseEntity} with the decoded body
		 * @since 5.2
		 */
		<T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyClass);

		/**
		 * Variant of {@link #bodyToMono(Class)} with a {@link ParameterizedTypeReference}.
		 * @param bodyTypeReference the expected response body type
		 * @param <T> the response body type
		 * @return the {@code ResponseEntity} with the decoded body
		 * @since 5.2
		 */
		<T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference);

		/**
		 * Return a {@code ResponseEntity} with the body decoded to a {@code List}
		 * of elements of the given type. For an error response (status code of
		 * 4xx or 5xx), the {@code Mono} emits a {@link WebClientException}.
		 * Use {@link #onStatus(Predicate, Function)} to customize error response
		 * handling.
		 * @param elementClass the type of element to decode the target Flux to
		 * @param <T> the body element type
		 * @return the {@code ResponseEntity}
		 * @since 5.2
		 */
		<T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass);

		/**
		 * Variant of {@link #toEntity(Class)} with a {@link ParameterizedTypeReference}.
		 * @param elementTypeRef the type of element to decode the target Flux to
		 * @param <T> the body element type
		 * @return the {@code ResponseEntity}
		 * @since 5.2
		 */
		<T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * Return a {@code ResponseEntity} with the body decoded to a {@code Flux}
		 * of elements of the given type. For an error response (status code of
		 * 4xx or 5xx), the {@code Mono} emits a {@link WebClientException}.
		 * Use {@link #onStatus(Predicate, Function)} to customize error response
		 * handling.
		 * <p><strong>Note:</strong> The {@code Flux} representing the body must
		 * be subscribed to or else associated resources will not be released.
		 * @param elementType the type of element to decode the target Flux to
		 * @param <T> the body element type
		 * @return the {@code ResponseEntity}
		 * @since 5.3.1
		 */
		<T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(Class<T> elementType);

		/**
		 * Variant of {@link #toEntityFlux(Class)} with a {@link ParameterizedTypeReference}.
		 * @param elementTypeReference the type of element to decode the target Flux to
		 * @param <T> the body element type
		 * @return the {@code ResponseEntity}
		 * @since 5.3.1
		 */
		<T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(ParameterizedTypeReference<T> elementTypeReference);

		/**
		 * Variant of {@link #toEntityFlux(Class)} with a {@link BodyExtractor}.
		 * @param bodyExtractor the {@code BodyExtractor} that reads from the response
		 * @param <T> the body element type
		 * @return the {@code ResponseEntity}
		 * @since 5.3.2
		 */
		<T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(BodyExtractor<Flux<T>, ? super ClientHttpResponse> bodyExtractor);

		/**
		 * Return a {@code ResponseEntity} without a body. For an error response
		 * (status code of 4xx or 5xx), the {@code Mono} emits a
		 * {@link WebClientException}. Use {@link #onStatus(Predicate, Function)}
		 * to customize error response handling.
		 * @return the {@code ResponseEntity}
		 * @since 5.2
		 */
		Mono<ResponseEntity<Void>> toBodilessEntity();
	}


	/**
	 * Contract for specifying request headers and URI for a request.
	 * @param <S> a self reference to the spec type
	 */
	interface RequestHeadersUriSpec<S extends RequestHeadersSpec<S>>
			extends UriSpec<S>, RequestHeadersSpec<S> {
	}


	/**
	 * Contract for specifying request headers, body and URI for a request.
	 */
	interface RequestBodyUriSpec extends RequestBodySpec, RequestHeadersUriSpec<RequestBodySpec> {
	}


}
