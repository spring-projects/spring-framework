/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.servlet.client;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.test.json.JsonComparator;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.json.JsonComparison;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.RouterFunctionMockMvcBuilder;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ApiVersionFormatter;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Client for testing web servers that uses {@link RestClient} internally to
 * perform requests while also providing a fluent API to verify responses.
 * This client can connect to any server over HTTP or to a {@link MockMvc} server
 * with a mock request and response.
 *
 * <p>Use one of the bindToXxx methods to create an instance. For example:
 * <ul>
 * <li>{@link #bindToController(Object...)}
 * <li>{@link #bindToRouterFunction(RouterFunction[])}
 * <li>{@link #bindToApplicationContext(WebApplicationContext)}
 * <li>{@link #bindToServer()}
 * <li>...
 * </ul>
 *
 * @author Rob Worsnop
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface RestTestClient {

	/**
	 * The name of a request header used to assign a unique id to every request
	 * performed through the {@code RestTestClient}. This can be useful for
	 * storing contextual information at all phases of request processing (for example,
	 * from a server-side component) under that id and later to look up
	 * that information once an {@link ExchangeResult} is available.
	 */
	String RESTTESTCLIENT_REQUEST_ID = "RestTestClient-Request-Id";


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
	 * Return a builder to mutate properties of this test client.
	 */
	<B extends Builder<B>> Builder<B> mutate();


	/**
	 * Begin creating a {@link RestTestClient} with a {@link MockMvcBuilders#standaloneSetup
	 * Standalone MockMvc setup}.
	 */
	static StandaloneSetupBuilder bindToController(Object... controllers) {
		return new DefaultRestTestClientBuilder.DefaultStandaloneSetupBuilder(controllers);
	}

	/**
	 * Begin creating a {@link RestTestClient} with a {@link MockMvcBuilders#routerFunctions}
	 * RouterFunction's MockMvc setup}.
	 */
	static RouterFunctionSetupBuilder bindToRouterFunction(RouterFunction<?>... routerFunctions) {
		return new DefaultRestTestClientBuilder.DefaultRouterFunctionSetupBuilder(routerFunctions);
	}

	/**
	 * Begin creating a {@link RestTestClient} with a {@link MockMvcBuilders#webAppContextSetup}
	 * WebAppContext MockMvc setup}.
	 */
	static WebAppContextSetupBuilder bindToApplicationContext(WebApplicationContext context) {
		return new DefaultRestTestClientBuilder.DefaultWebAppContextSetupBuilder(context);
	}

	/**
	 * Begin creating a {@link RestTestClient} by providing an already
	 * initialized {@link MockMvc} instance to use as the server.
	 */
	static Builder<?> bindTo(MockMvc mockMvc) {
		ClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mockMvc);
		return RestTestClient.bindToServer(requestFactory);
	}

	/**
	 * This server setup option allows you to connect to a live server.
	 * <p><pre class="code">
	 * RestTestClient client = RestTestClient.bindToServer()
	 *         .baseUrl("http://localhost:8080")
	 *         .build();
	 * </pre>
	 * @return chained API to customize client config
	 */
	static Builder<?> bindToServer() {
		return new DefaultRestTestClientBuilder<>();
	}

	/**
	 * A variant of {@link #bindToServer()} with a pre-configured request factory.
	 * @return chained API to customize client config
	 */
	static Builder<?> bindToServer(ClientHttpRequestFactory requestFactory) {
		return new DefaultRestTestClientBuilder<>(RestClient.builder().requestFactory(requestFactory));
	}


	/**
	 * Steps to customize the underlying {@link RestClient} via {@link RestClient.Builder}.
	 * @param <B> the type of builder
	 */
	interface Builder<B extends Builder<B>> {

		/**
		 * Configure a base URI as described in {@link RestClient#create(String)}.
		 */
		<T extends B> T baseUrl(String baseUrl);

		/**
		 * Provide a pre-configured {@link UriBuilderFactory} instance as an
		 * alternative to and effectively overriding {@link #baseUrl(String)}.
		 */
		<T extends B> T uriBuilderFactory(UriBuilderFactory uriBuilderFactory);

		/**
		 * Add the given header to all requests that haven't added it.
		 * @param headerName the header name
		 * @param headerValues the header values
		 */
		<T extends B> T defaultHeader(String headerName, String... headerValues);

		/**
		 * Manipulate the default headers with the given consumer. The
		 * headers provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
		 * {@linkplain HttpHeaders#remove(String) remove} values, or use any of the other
		 * {@link HttpHeaders} methods.
		 * @param headersConsumer a function that consumes the {@code HttpHeaders}
		 * @return this builder
		 */
		<T extends B> T defaultHeaders(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Add the given cookie to all requests that haven't already added it.
		 * @param cookieName the cookie name
		 * @param cookieValues the cookie values
		 */
		<T extends B> T defaultCookie(String cookieName, String... cookieValues);

		/**
		 * Manipulate the default cookies with the given consumer. The
		 * map provided to the consumer is "live", so that the consumer can be used to
		 * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing header values,
		 * {@linkplain MultiValueMap#remove(Object) remove} values, or use any of the other
		 * {@link MultiValueMap} methods.
		 * @param cookiesConsumer a function that consumes the cookies map
		 * @return this builder
		 */
		<T extends B> T defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

		/**
		 * Global option to specify an API version to add to every request,
		 * if not already set.
		 * @param version the version to use
		 * @return this builder
		 * @since 7.0
		 */
		<T extends B> T defaultApiVersion(Object version);

		/**
		 * Configure an {@link ApiVersionInserter} to abstract how an API version
		 * specified via {@link RequestHeadersSpec#apiVersion(Object)}
		 * is inserted into the request.
		 * @param apiVersionInserter the inserter to use
		 * @since 7.0
		 */
		<T extends B> T apiVersionInserter(ApiVersionInserter apiVersionInserter);

		/**
		 * Add the given request interceptor to the end of the interceptor chain.
		 * @param interceptor the interceptor to be added to the chain
		 */
		<T extends B> T requestInterceptor(ClientHttpRequestInterceptor interceptor);

		/**
		 * Manipulate the interceptors with the given consumer. The list provided to
		 * the consumer is "live", so that the consumer can be used to remove
		 * interceptors, change ordering, etc.
		 * @param interceptorsConsumer a function that consumes the interceptors list
		 * @return this builder
		 */
		<T extends B> T requestInterceptors(Consumer<List<ClientHttpRequestInterceptor>> interceptorsConsumer);

		/**
		 * Configure the message converters to use for the request and response body.
		 * @param configurer the configurer to apply on an empty {@link HttpMessageConverters.ClientBuilder}.
		 * @return this builder
		 */
		<T extends B> T configureMessageConverters(Consumer<HttpMessageConverters.ClientBuilder> configurer);

		/**
		 * Configure an {@code EntityExchangeResult} callback that is invoked
		 * every time after a response is fully decoded to a single entity, to a
		 * List of entities, or to a byte[]. In effect, equivalent to each and
		 * all of the below but registered once, globally:
		 * <pre>
		 * client.get().uri("/accounts/1")
		 *         .exchange()
		 *         .expectBody(Person.class).consumeWith(exchangeResult -&gt; ... ));
		 *
		 * client.get().uri("/accounts")
		 *         .exchange()
		 *         .expectBodyList(Person.class).consumeWith(exchangeResult -&gt; ... ));
		 *
		 * client.get().uri("/accounts/1")
		 *         .exchange()
		 *         .expectBody().consumeWith(exchangeResult -&gt; ... ));
		 * </pre>
		 * <p>Note that the configured consumer does not apply to responses
		 * decoded to {@code Flux<T>} which can be consumed outside the workflow
		 * of the test client, for example via {@code reactor.test.StepVerifier}.
		 * @param consumer the consumer to apply to entity responses
		 * @return the builder
		 */
		<T extends B> T entityExchangeResultConsumer(Consumer<EntityExchangeResult<?>> consumer);

		/**
		 * Build the {@link RestTestClient} instance.
		 */
		RestTestClient build();
	}


	/**
	 * Extension of {@link Builder} for tests against a MockMvc server.
	 * @param <S> the builder type
	 * @param <M> the type of {@link MockMvc} setup
	 */
	interface MockMvcSetupBuilder<S extends Builder<S>, M extends MockMvcBuilder> extends Builder<S> {

		<T extends S> T configureServer(Consumer<M> consumer);
	}


	/**
	 * Extension of {@link Builder} for tests витх а
	 * {@link MockMvcBuilders#standaloneSetup(Object...) standalone MockMvc setup}.
	 */
	interface StandaloneSetupBuilder extends MockMvcSetupBuilder<StandaloneSetupBuilder, StandaloneMockMvcBuilder> {
	}


	/**
	 * Extension of {@link Builder} for tests витх а
	 * {@link MockMvcBuilders#routerFunctions(RouterFunction[]) RouterFunction MockMvc setup}.
	 */
	interface RouterFunctionSetupBuilder extends MockMvcSetupBuilder<RouterFunctionSetupBuilder, RouterFunctionMockMvcBuilder> {
	}


	/**
	 * Extension of {@link Builder} for tests витх а
	 * {@link MockMvcBuilders#webAppContextSetup(WebApplicationContext) WebAppContext MockMvc setup}.
	 */
	interface WebAppContextSetupBuilder extends MockMvcSetupBuilder<WebAppContextSetupBuilder, DefaultMockMvcBuilder> {
	}


	/**
	 * Specification for providing the URI of a request.
	 *
	 * @param <S> a self reference to the spec type
	 */
	interface UriSpec<S extends RequestHeadersSpec<?>> {

		/**
		 * Specify the URI using an absolute, fully constructed {@link java.net.URI}.
		 * <p>If a {@link UriBuilderFactory} was configured for the client with
		 * a base URI, that base URI will <strong>not</strong> be applied to the
		 * supplied {@code java.net.URI}. If you wish to have a base URI applied to a
		 * {@code java.net.URI} you must invoke either {@link #uri(String, Object...)}
		 * or {@link #uri(String, Map)} &mdash; for example, {@code uri(myUri.toString())}.
		 * @return spec to add headers or perform the exchange
		 */
		S uri(URI uri);

		/**
		 * Specify the URI for the request using a URI template and URI variables.
		 * <p>If a {@link UriBuilderFactory} was configured for the client (for example,
		 * with a base URI) it will be used to expand the URI template.
		 * @return spec to add headers or perform the exchange
		 */
		S uri(String uri, @Nullable Object... uriVariables);

		/**
		 * Specify the URI for the request using a URI template and URI variables.
		 * <p>If a {@link UriBuilderFactory} was configured for the client (for example,
		 * with a base URI) it will be used to expand the URI template.
		 * @return spec to add headers or perform the exchange
		 */
		S uri(String uri, Map<String, ? extends @Nullable Object> uriVariables);

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
		 * {@linkplain HttpHeaders#remove(String) remove} values, or use any of the other
		 * {@link HttpHeaders} methods.
		 * @param headersConsumer a function that consumes the {@code HttpHeaders}
		 * @return this builder
		 */
		S headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Set an API version for the request. The version is inserted into the
		 * request by the {@linkplain Builder#apiVersionInserter(ApiVersionInserter)
		 * configured} {@code ApiVersionInserter}.
		 * @param version the API version of the request; this can be a String or
		 * some Object that can be formatted by the inserter &mdash; for example,
		 * through an {@link ApiVersionFormatter}
		 * @since 7.0
		 */
		S apiVersion(Object version);

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
		 * {@link RestClient.RequestBodySpec#body(Object)} (Object)
		 * bodyValue} method on the underlying {@code RestClient}.
		 * @param body the value to write to the request body
		 * @return spec for further declaration of the request
		 */
		RequestHeadersSpec<?> body(Object body);
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
		 * Apply multiple assertions to a response with the given
		 * {@linkplain ResponseSpecConsumer consumers}, with the guarantee that
		 * all assertions will be applied even if one or more assertions fails
		 * with an exception.
		 * <p>If a single {@link Error} or {@link RuntimeException} is thrown,
		 * it will be rethrown.
		 * <p>If multiple exceptions are thrown, this method will throw an
		 * {@link AssertionError} whose error message is a summary of all the
		 * exceptions. In addition, each exception will be added as a
		 * {@linkplain Throwable#addSuppressed(Throwable) suppressed exception} to
		 * the {@code AssertionError}.
		 * <p>This feature is similar to the {@code SoftAssertions} support in
		 * AssertJ and the {@code assertAll()} support in JUnit Jupiter.
		 *
		 * <h4>Example</h4>
		 * <pre class="code">
		 * restTestClient.get().uri("/hello").exchange()
		 *     .expectAll(
		 *         responseSpec -&gt; responseSpec.expectStatus().isOk(),
		 *         responseSpec -&gt; responseSpec.expectBody(String.class).isEqualTo("Hello, World!")
		 *     );
		 * </pre>
		 * @param consumers the list of {@code ResponseSpec} consumers
		 */
		ResponseSpec expectAll(ResponseSpecConsumer... consumers);

		/**
		 * Assertions on the response status.
		 */
		StatusAssertions expectStatus();

		/**
		 * Assertions on the headers of the response.
		 */
		HeaderAssertions expectHeader();

		/**
		 * Assertions on the cookies of the response.
		 */
		CookieAssertions expectCookie();

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
		 * Consume and decode the response body to {@code byte[]} and then apply
		 * assertions on the raw content (for example, isEmpty, JSONPath, etc.).
		 */
		BodyContentSpec expectBody();

		/**
		 * Exit the chained flow in order to consume the response body externally.
		 */
		<T> EntityExchangeResult<T> returnResult(Class<T> elementClass);

		/**
		 * Alternative to {@link #returnResult(Class)} that accepts information
		 * about a target type with generics.
		 */
		<T> EntityExchangeResult<T> returnResult(ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * {@link Consumer} of a {@link ResponseSpec}.
		 * @see ResponseSpec#expectAll(ResponseSpecConsumer...)
		 */
		@FunctionalInterface
		interface ResponseSpecConsumer extends Consumer<ResponseSpec> {
		}
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
		<T extends S> T isEqualTo(@Nullable B expected);

		/**
		 * Assert the extracted body with a {@link Matcher}.
		 * @since 5.1
		 */
		<T extends S> T value(Matcher<? super @Nullable B> matcher);

		/**
		 * Transform the extracted the body with a function, for example, extracting a
		 * property, and assert the mapped value with a {@link Matcher}.
		 */
		<T extends S, R> T value(Function<@Nullable B, @Nullable R> bodyMapper, Matcher<? super @Nullable R> matcher);

		/**
		 * Assert the extracted body with a {@link Consumer}.
		 */
		<T extends S> T value(Consumer<@Nullable B> consumer);

		/**
		 * Assert the exchange result with the given {@link Consumer}.
		 */
		<T extends S> T consumeWith(Consumer<EntityExchangeResult<B>> consumer);

		/**
		 * Exit the chained API and return an {@code EntityExchangeResult} with the
		 * decoded response content.
		 */
		EntityExchangeResult<B> returnResult();
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
		 * comparison verifying that they contain the same attribute-value pairs
		 * regardless of formatting with <em>lenient</em> checking (extensible
		 * and non-strict array ordering).
		 * <p>Use of this method requires the
		 * <a href="https://jsonassert.skyscreamer.org/">JSONassert</a> library
		 * to be on the classpath.
		 * @param expectedJson the expected JSON content
		 * @see #json(String, JsonCompareMode)
		 */
		default BodyContentSpec json(String expectedJson) {
			return json(expectedJson, JsonCompareMode.LENIENT);
		}

		/**
		 * Parse the expected and actual response content as JSON and perform a
		 * comparison using the given {@linkplain JsonCompareMode mode}. If the
		 * comparison failed, throws an {@link AssertionError} with the message
		 * of the {@link JsonComparison}.
		 * <p>Use of this method requires the
		 * <a href="https://jsonassert.skyscreamer.org/">JSONassert</a> library
		 * to be on the classpath.
		 * @param expectedJson the expected JSON content
		 * @param compareMode the compare mode
		 * @see #json(String)
		 */
		BodyContentSpec json(String expectedJson, JsonCompareMode compareMode);

		/**
		 * Parse the expected and actual response content as JSON and perform a
		 * comparison using the given {@link JsonComparator}. If the comparison
		 * failed, throws an {@link AssertionError} with the message  of the
		 * {@link JsonComparison}.
		 * @param expectedJson the expected JSON content
		 * @param comparator the comparator to use
		 */
		BodyContentSpec json(String expectedJson, JsonComparator comparator);

		/**
		 * Parse expected and actual response content as XML and assert that
		 * the two are "similar", i.e. they contain the same elements and
		 * attributes regardless of order.
		 * <p>Use of this method requires the
		 * <a href="https://github.com/xmlunit/xmlunit">XMLUnit</a> library on
		 * the classpath.
		 * @param expectedXml the expected XML content.
		 * @see org.springframework.test.util.XmlExpectationsHelper#assertXmlEqual(String, String)
		 */
		BodyContentSpec xml(String expectedXml);

		/**
		 * Access to response body assertions using a
		 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression
		 * to inspect a specific subset of the body.
		 * @param expression the JsonPath expression
		 */
		JsonPathAssertions jsonPath(String expression);

		/**
		 * Access to response body assertions using an XPath expression to
		 * inspect a specific subset of the body.
		 * <p>The XPath expression can be a parameterized string using
		 * formatting specifiers as defined in {@link String#format}.
		 * @param expression the XPath expression
		 * @param args arguments to parameterize the expression
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
		 * @param namespaces the namespaces to use
		 * @param args arguments to parameterize the expression
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
