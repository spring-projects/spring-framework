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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.servlet.Filter;
import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.json.JsonComparator;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.json.JsonComparison;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.DispatcherServletCustomizer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.test.web.servlet.setup.RouterFunctionMockMvcBuilder;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Client for testing web servers.
 *
 * @author Rob Worsnop
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
	Builder mutate();



	/**
	 * Begin creating a {@link RestTestClient} by providing the {@code @Controller}
	 * instance(s) to handle requests with.
	 * <p>Internally this is delegated to and equivalent to using
	 * {@link org.springframework.test.web.servlet.setup.MockMvcBuilders#standaloneSetup(Object...)}
	 * to initialize {@link MockMvc}.
	 */
	static ControllerSpec bindToController(Object... controllers) {
		return new StandaloneMockSpec(controllers);
	}

	/**
	 * Begin creating a {@link RestTestClient} by providing the {@link RouterFunction}
	 * instance(s) to handle requests with.
	 * <p>Internally this is delegated to and equivalent to using
	 * {@link org.springframework.test.web.servlet.setup.MockMvcBuilders#routerFunctions(RouterFunction[])}
	 * to initialize {@link MockMvc}.
	 */
	static RouterFunctionSpec bindToRouterFunction(RouterFunction<?>... routerFunctions) {
		return new RouterFunctionMockSpec(routerFunctions);
	}

	/**
	 * Begin creating a {@link RestTestClient} by providing a
	 * {@link WebApplicationContext} with Spring MVC infrastructure and
	 * controllers.
	 * <p>Internally this is delegated to and equivalent to using
	 * {@link org.springframework.test.web.servlet.setup.MockMvcBuilders#webAppContextSetup(WebApplicationContext)}
	 * to initialize {@code MockMvc}.
	 */
	static MockServerSpec<?> bindToApplicationContext(WebApplicationContext context) {
		return new ApplicationContextMockSpec(context);
	}



	/**
	 * Begin creating a {@link RestTestClient} by providing an already
	 * initialized {@link MockMvc} instance to use as the server.
	 */
	static RestTestClient.Builder bindTo(MockMvc mockMvc) {
		ClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mockMvc);
		return RestTestClient.bindToServer(requestFactory);
	}

	/**
	 * This server setup option allows you to connect to a live server through
	 * a client connector.
	 * <p><pre class="code">
	 * RestTestClient client = RestTestClient.bindToServer()
	 *         .baseUrl("http://localhost:8080")
	 *         .build();
	 * </pre>
	 * @return chained API to customize client config
	 */
	static Builder bindToServer() {
		return new DefaultRestTestClientBuilder();
	}

	/**
	 * A variant of {@link #bindToServer()} with a pre-configured request factory.
	 * @return chained API to customize client config
	 */
	static Builder bindToServer(ClientHttpRequestFactory requestFactory) {
		return new DefaultRestTestClientBuilder(RestClient.builder().requestFactory(requestFactory));
	}

	/**
	 * Specification for customizing controller configuration.
	 */
	interface ControllerSpec extends MockServerSpec<ControllerSpec> {
		/**
		 * Register {@link org.springframework.web.bind.annotation.ControllerAdvice}
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setControllerAdvice(Object...)}.
		 */
		ControllerSpec controllerAdvice(Object... controllerAdvice);

		/**
		 * Set the message converters to use.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setMessageConverters(HttpMessageConverter[])}.
		 */
		ControllerSpec messageConverters(HttpMessageConverter<?>... messageConverters);

		/**
		 * Provide a custom {@link Validator}.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setValidator(Validator)}.
		 */
		ControllerSpec validator(Validator validator);

		/**
		 * Provide a conversion service.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setConversionService(FormattingConversionService)}.
		 */
		ControllerSpec conversionService(FormattingConversionService conversionService);

		/**
		 * Add global interceptors.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#addInterceptors(HandlerInterceptor...)}.
		 */
		ControllerSpec interceptors(HandlerInterceptor... interceptors);

		/**
		 * Add interceptors for specific patterns.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#addMappedInterceptors(String[], HandlerInterceptor...)}.
		 */
		ControllerSpec mappedInterceptors(
				String @Nullable [] pathPatterns, HandlerInterceptor... interceptors);

		/**
		 * Set a ContentNegotiationManager.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setContentNegotiationManager(ContentNegotiationManager)}.
		 */
		ControllerSpec contentNegotiationManager(ContentNegotiationManager manager);

		/**
		 * Specify the timeout value for async execution.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setAsyncRequestTimeout(long)}.
		 */
		ControllerSpec asyncRequestTimeout(long timeout);

		/**
		 * Provide custom argument resolvers.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setCustomArgumentResolvers(HandlerMethodArgumentResolver...)}.
		 */
		ControllerSpec customArgumentResolvers(HandlerMethodArgumentResolver... argumentResolvers);

		/**
		 * Provide custom return value handlers.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setCustomReturnValueHandlers(HandlerMethodReturnValueHandler...)}.
		 */
		ControllerSpec customReturnValueHandlers(HandlerMethodReturnValueHandler... handlers);

		/**
		 * Set the HandlerExceptionResolver types to use.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setHandlerExceptionResolvers(HandlerExceptionResolver...)}.
		 */
		ControllerSpec handlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers);

		/**
		 * Set up view resolution.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setViewResolvers(ViewResolver...)}.
		 */
		ControllerSpec viewResolvers(ViewResolver... resolvers);

		/**
		 * Set up a single {@link ViewResolver} with a fixed view.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setSingleView(View)}.
		 */
		ControllerSpec singleView(View view);

		/**
		 * Provide the LocaleResolver to use.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setLocaleResolver(LocaleResolver)}.
		 */
		ControllerSpec localeResolver(LocaleResolver localeResolver);

		/**
		 * Provide a custom FlashMapManager.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setFlashMapManager(FlashMapManager)}.
		 */
		ControllerSpec flashMapManager(FlashMapManager flashMapManager);

		/**
		 * Enable URL path matching with parsed
		 * {@link org.springframework.web.util.pattern.PathPattern PathPatterns}.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setPatternParser(PathPatternParser)}.
		 */
		ControllerSpec patternParser(PathPatternParser parser);

		/**
		 * Configure placeholder values to use.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#addPlaceholderValue(String, String)}.
		 */
		ControllerSpec placeholderValue(String name, String value);

		/**
		 * Configure factory for a custom {@link RequestMappingHandlerMapping}.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setCustomHandlerMapping(Supplier)}.
		 */
		ControllerSpec customHandlerMapping(Supplier<RequestMappingHandlerMapping> factory);
	}

	/**
	 * Specification for configuring {@link MockMvc} to test one or more
	 * {@linkplain RouterFunction router functions}
	 * directly, and a simple facade around {@link RouterFunctionMockMvcBuilder}.
	 */
	interface RouterFunctionSpec extends MockServerSpec<RouterFunctionSpec> {

		/**
		 * Set the message converters to use.
		 * <p>This is delegated to
		 * {@link RouterFunctionMockMvcBuilder#setMessageConverters(HttpMessageConverter[])}.
		 */
		RouterFunctionSpec messageConverters(HttpMessageConverter<?>... messageConverters);

		/**
		 * Add global interceptors.
		 * <p>This is delegated to
		 * {@link RouterFunctionMockMvcBuilder#addInterceptors(HandlerInterceptor...)}.
		 */
		RouterFunctionSpec interceptors(HandlerInterceptor... interceptors);

		/**
		 * Add interceptors for specific patterns.
		 * <p>This is delegated to
		 * {@link RouterFunctionMockMvcBuilder#addMappedInterceptors(String[], HandlerInterceptor...)}.
		 */
		RouterFunctionSpec mappedInterceptors(
				String @Nullable [] pathPatterns, HandlerInterceptor... interceptors);

		/**
		 * Specify the timeout value for async execution.
		 * <p>This is delegated to
		 * {@link RouterFunctionMockMvcBuilder#setAsyncRequestTimeout(long)}.
		 */
		RouterFunctionSpec asyncRequestTimeout(long timeout);

		/**
		 * Set the HandlerExceptionResolver types to use.
		 * <p>This is delegated to
		 * {@link RouterFunctionMockMvcBuilder#setHandlerExceptionResolvers(HandlerExceptionResolver...)}.
		 */
		RouterFunctionSpec handlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers);

		/**
		 * Set up view resolution.
		 * <p>This is delegated to
		 * {@link RouterFunctionMockMvcBuilder#setViewResolvers(ViewResolver...)}.
		 */
		RouterFunctionSpec viewResolvers(ViewResolver... resolvers);

		/**
		 * Set up a single {@link ViewResolver} with a fixed view.
		 * <p>This is delegated to
		 * {@link RouterFunctionMockMvcBuilder#setSingleView(View)}.
		 */
		RouterFunctionSpec singleView(View view);

		/**
		 * Enable URL path matching with parsed
		 * {@link org.springframework.web.util.pattern.PathPattern PathPatterns}.
		 * <p>This is delegated to
		 * {@link RouterFunctionMockMvcBuilder#setPatternParser(PathPatternParser)}.
		 */
		RouterFunctionSpec patternParser(PathPatternParser parser);

	}


	/**
	 * Base specification for configuring {@link MockMvc}, and a simple facade
	 * around {@link ConfigurableMockMvcBuilder}.
	 *
	 * @param <B> a self reference to the builder type
	 */
	interface MockServerSpec<B extends MockServerSpec<B>> {
		/**
		 * Add a global filter.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#addFilters(Filter...)}.
		 */
		<T extends B> T filters(Filter... filters);

		/**
		 * Add a filter for specific URL patterns.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#addFilter(Filter, String...)}.
		 */
		<T extends B> T filter(Filter filter, String... urlPatterns);

		/**
		 * Define default request properties that should be merged into all
		 * performed requests such that input from the client request override
		 * the default properties defined here.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#defaultRequest(RequestBuilder)}.
		 */
		<T extends B> T defaultRequest(RequestBuilder requestBuilder);

		/**
		 * Define a global expectation that should <em>always</em> be applied to
		 * every response.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#alwaysExpect(ResultMatcher)}.
		 */
		<T extends B> T alwaysExpect(ResultMatcher resultMatcher);

		/**
		 * Whether to handle HTTP OPTIONS requests.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#dispatchOptions(boolean)}.
		 */
		<T extends B> T dispatchOptions(boolean dispatchOptions);

		/**
		 * Allow customization of {@code DispatcherServlet}.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#addDispatcherServletCustomizer(DispatcherServletCustomizer)}.
		 */
		<T extends B> T dispatcherServletCustomizer(DispatcherServletCustomizer customizer);

		/**
		 * Add a {@code MockMvcConfigurer} that automates MockMvc setup.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#apply(MockMvcConfigurer)}.
		 */
		<T extends B> T apply(MockMvcConfigurer configurer);


		/**
		 * Proceed to configure and build the test client.
		 */
		Builder configureClient();

		/**
		 * Shortcut to build the test client.
		 */
		RestTestClient build();
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
		 * Consume and decode the response body to {@code byte[]} and then apply
		 * assertions on the raw content (for example, isEmpty, JSONPath, etc.).
		 */
		BodyContentSpec expectBody();

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
		 * Assertions on the cookies of the response.
		 */
		CookieAssertions expectCookie();

		/**
		 * Assertions on the headers of the response.
		 */
		HeaderAssertions expectHeader();

		/**
		 * Apply multiple assertions to a response with the given
		 * {@linkplain RestTestClient.ResponseSpec.ResponseSpecConsumer consumers}, with the guarantee that
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
		 * Exit the chained flow in order to consume the response body
		 * externally.
		 */
		<T> EntityExchangeResult<T> returnResult(Class<T> elementClass);

		/**
		 * Alternative to {@link #returnResult(Class)} that accepts information
		 * about a target type with generics.
		 */
		<T> EntityExchangeResult<T> returnResult(ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * {@link Consumer} of a {@link RestTestClient.ResponseSpec}.
		 * @see RestTestClient.ResponseSpec#expectAll(RestTestClient.ResponseSpec.ResponseSpecConsumer...)
		 */
		@FunctionalInterface
		interface ResponseSpecConsumer extends Consumer<ResponseSpec> {
		}
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
		 * Access to response body assertions using a
		 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression
		 * to inspect a specific subset of the body.
		 * @param expression the JsonPath expression
		 */
		JsonPathAssertions jsonPath(String expression);

		/**
		 * Exit the chained API and return an {@code ExchangeResult} with the
		 * raw response content.
		 */
		EntityExchangeResult<byte[]> returnResult();
	}

	/**
	 * Spec for expectations on the response body decoded to a single Object.
	 *
	 * @param <S> a self reference to the spec type
	 * @param <B> the body type
	 */
	interface BodySpec<B, S extends BodySpec<B, S>> {
		/**
		 * Transform the extracted the body with a function, for example, extracting a
		 * property, and assert the mapped value with a {@link Matcher}.
		 */
		<T extends S, R> T value(Function<B, R> bodyMapper, Matcher<? super R> matcher);

		/**
		 * Assert the extracted body with a {@link Consumer}.
		 */
		<T extends S> T value(Consumer<B> consumer);

		/**
		 * Assert the exchange result with the given {@link Consumer}.
		 */
		<T extends S> T consumeWith(Consumer<EntityExchangeResult<B>> consumer);

		/**
		 * Exit the chained API and return an {@code EntityExchangeResult} with the
		 * decoded response content.
		 */
		EntityExchangeResult<B> returnResult();

		/**
		 * Assert the extracted body is equal to the given value.
		 */
		<T extends S> T isEqualTo(B expected);
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
		S uri(String uri, Object... uriVariables);

		/**
		 * Specify the URI for the request using a URI template and URI variables.
		 * <p>If a {@link UriBuilderFactory} was configured for the client (for example,
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
		 * {@linkplain HttpHeaders#remove(String) remove} values, or use any of the other
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
		 * Set the {@linkplain MediaType media type} of the body, as specified
		 * by the {@code Content-Type} header.
		 * @param contentType the content type
		 * @return the same instance
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		RequestBodySpec contentType(MediaType contentType);

		/**
		 * Set the body to the given {@code Object} value. This method invokes the
		 * {@link org.springframework.web.client.RestClient.RequestBodySpec#body(Object)} (Object)
		 * bodyValue} method on the underlying {@code RestClient}.
		 * @param body the value to write to the request body
		 * @return spec for further declaration of the request
		 */
		RequestHeadersSpec<?> body(Object body);
	}

		interface Builder {

			/**
			 * Apply the given {@code Consumer} to this builder instance.
			 * <p>This can be useful for applying pre-packaged customizations.
			 * @param builderConsumer the consumer to apply
			 */
			Builder apply(Consumer<Builder> builderConsumer);

			/**
			 * Add the given cookie to all requests.
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
			 * Add the given header to all requests that haven't added it.
			 * @param headerName the header name
			 * @param headerValues the header values
			 */
			Builder defaultHeader(String headerName, String... headerValues);

			/**
			 * Manipulate the default headers with the given consumer. The
			 * headers provided to the consumer are "live", so that the consumer can be used to
			 * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
			 * {@linkplain HttpHeaders#remove(String) remove} values, or use any of the other
			 * {@link HttpHeaders} methods.
			 * @param headersConsumer a function that consumes the {@code HttpHeaders}
			 * @return this builder
			 */
			Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer);

			/**
			 * Provide a pre-configured {@link UriBuilderFactory} instance as an
			 * alternative to and effectively overriding {@link #baseUrl(String)}.
			 */
			Builder uriBuilderFactory(UriBuilderFactory uriFactory);

			/**
			 * Build the {@link RestTestClient} instance.
			 */
			RestTestClient build();

			/**
			 * Configure a base URI as described in
			 * {@link RestClient#create(String)
			 * WebClient.create(String)}.
			 */
			Builder baseUrl(String baseUrl);
		}
}
