/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.servlet.assertj;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import jakarta.servlet.DispatcherType;
import org.assertj.core.api.AssertProvider;

import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.http.HttpMessageContentConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.AbstractMockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@code MockMvcTester} provides support for testing Spring MVC applications
 * with {@link MockMvc} for server request handling using
 * {@linkplain org.assertj.core.api.Assertions AssertJ}.
 *
 * <p>A tester instance can be created from a {@link WebApplicationContext}:
 * <pre><code class="java">
 * // Create an instance with default settings
 * MockMvcTester mvc = MockMvcTester.from(applicationContext);
 *
 * // Create an instance with a custom Filter
 * MockMvcTester mvc = MockMvcTester.from(applicationContext,
 *         builder -> builder.addFilters(filter).build());
 * </code></pre>
 *
 * <p>A tester can be created in standalone mode by providing the controller
 * instances to include:<pre><code class="java">
 * // Create an instance for PersonController
 * MockMvcTester mvc = MockMvcTester.of(new PersonController());
 * </code></pre>
 *
 * <p>Simple, single-statement assertions can be done wrapping the request
 * builder in {@code assertThat()} provides access to assertions. For instance:
 * <pre><code class="java">
 * // perform a GET on /hi and assert the response body is equal to Hello
 * assertThat(mvc.get().uri("/hi")).hasStatusOk().hasBodyTextEqualTo("Hello");
 * </code></pre>
 *
 * <p>For more complex scenarios the {@linkplain MvcTestResult result} of the
 * exchange can be assigned in a variable to run multiple assertions:
 * <pre><code class="java">
 * // perform a POST on /save and assert the response body is empty
 * MvcTestResult result = mvc.post().uri("/save").exchange();
 * assertThat(result).hasStatus(HttpStatus.CREATED);
 * assertThat(result).body().isEmpty();
 * </code></pre>
 *
 * <p>If the request is processing asynchronously, {@code exchange} waits for
 * its completion, either using the
 * {@linkplain org.springframework.mock.web.MockAsyncContext#setTimeout default
 * timeout} or a given one. If you prefer to get the result of an
 * asynchronous request immediately, use {@code asyncExchange}:
 * <pre><code class="java">
 * // perform a POST on /save and assert an asynchronous request has started
 * assertThat(mvc.post().uri("/save").asyncExchange()).request().hasAsyncStarted();
 * </code></pre>
 *
 * <p>You can also perform requests using the static builders approach that
 * {@link MockMvc} uses. For instance:<pre><code class="java">
 * // perform a GET on /hi and assert the response body is equal to Hello
 * assertThat(mvc.perform(get("/hi")))
 *         .hasStatusOk().hasBodyTextEqualTo("Hello");
 * </code></pre>
 *
 * <p>Use this approach if you have a custom {@link RequestBuilder} implementation
 * that you'd like to integrate here. This approach is also invoking {@link MockMvc}
 * without any additional processing of asynchronous requests.
 *
 * <p>One main difference between {@link MockMvc} and {@code MockMvcTester} is
 * that an unresolved exception is not thrown directly when using
 * {@code MockMvcTester}. Rather an {@link MvcTestResult} is available with an
 * {@linkplain MvcTestResult#getUnresolvedException() unresolved exception}.
 * Both resolved and unresolved exceptions are considered a failure that can
 * be asserted as follows:
 * <pre><code class="java">
 * // perform a GET on /boom and assert the message for the the exception
 * assertThat(mvc.get().uri("/boom")).hasFailed()
 *         .failure().hasMessage("Test exception");
 * </code></pre>
 *
 * <p>Any attempt to access the result with an unresolved exception will
 * throw an {@link AssertionError}:
 * <pre><code class="java">
 * // throw an AssertionError with an unresolved exception
 * assertThat(mvc.get().uri("/boom")).hasStatus5xxServerError();
 * </code></pre>
 *
 * <p>{@code MockMvcTester} can be configured with a list of
 * {@linkplain HttpMessageConverter message converters} to allow the response
 * body to be deserialized, rather than asserting on the raw values.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.2
 */
public final class MockMvcTester {

	private final MockMvc mockMvc;

	@Nullable
	private final HttpMessageContentConverter contentConverter;


	private MockMvcTester(MockMvc mockMvc, @Nullable HttpMessageContentConverter contentConverter) {
		Assert.notNull(mockMvc, "mockMVC should not be null");
		this.mockMvc = mockMvc;
		this.contentConverter = contentConverter;
	}

	/**
	 * Create an instance that delegates to the given {@link MockMvc} instance.
	 * @param mockMvc the MockMvc instance to delegate calls to
	 */
	public static MockMvcTester create(MockMvc mockMvc) {
		return new MockMvcTester(mockMvc, null);
	}

	/**
	 * Create an instance using the given, fully initialized (i.e.,
	 * <em>refreshed</em>) {@link WebApplicationContext}. The given
	 * {@code customizations} are applied to the {@link DefaultMockMvcBuilder}
	 * that ultimately creates the underlying {@link MockMvc} instance.
	 * <p>If no further customization of the underlying {@link MockMvc} instance
	 * is required, use {@link #from(WebApplicationContext)}.
	 * @param applicationContext the application context to detect the Spring
	 * MVC infrastructure and application controllers from
	 * @param customizations a function that creates a {@link MockMvc}
	 * instance based on a {@link DefaultMockMvcBuilder}
	 * @see MockMvcBuilders#webAppContextSetup(WebApplicationContext)
	 */
	public static MockMvcTester from(WebApplicationContext applicationContext,
			Function<DefaultMockMvcBuilder, MockMvc> customizations) {

		DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(applicationContext);
		MockMvc mockMvc = customizations.apply(builder);
		return create(mockMvc);
	}

	/**
	 * Shortcut to create an instance using the given fully initialized (i.e.,
	 * <em>refreshed</em>) {@link WebApplicationContext}.
	 * <p>Consider using {@link #from(WebApplicationContext, Function)} if
	 * further customization of the underlying {@link MockMvc} instance is
	 * required.
	 * @param applicationContext the application context to detect the Spring
	 * MVC infrastructure and application controllers from
	 * @see MockMvcBuilders#webAppContextSetup(WebApplicationContext)
	 */
	public static MockMvcTester from(WebApplicationContext applicationContext) {
		return from(applicationContext, DefaultMockMvcBuilder::build);
	}

	/**
	 * Create an instance by registering one or more {@code @Controller} instances
	 * and configuring Spring MVC infrastructure programmatically.
	 * <p>This allows full control over the instantiation and initialization of
	 * controllers and their dependencies, similar to plain unit tests while
	 * also making it possible to test one controller at a time.
	 * @param controllers one or more {@code @Controller} instances or
	 * {@code @Controller} types to test; a type ({@code Class}) will be turned
	 * into an instance
	 * @param customizations a function that creates a {@link MockMvc} instance
	 * based on a {@link StandaloneMockMvcBuilder}, typically to configure the
	 * Spring MVC infrastructure
	 * @see MockMvcBuilders#standaloneSetup(Object...)
	 */
	public static MockMvcTester of(Collection<?> controllers,
			Function<StandaloneMockMvcBuilder, MockMvc> customizations) {

		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(controllers.toArray());
		return create(customizations.apply(builder));
	}

	/**
	 * Shortcut to create an instance by registering one or more {@code @Controller}
	 * instances.
	 * <p>The minimum infrastructure required by the
	 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
	 * to serve requests with annotated controllers is created. Consider using
	 * {@link #of(Collection, Function)} if additional configuration of the MVC
	 * infrastructure is required.
	 * @param controllers one or more {@code @Controller} instances or
	 * {@code @Controller} types to test; a type ({@code Class}) will be turned
	 * into an instance
	 * @see MockMvcBuilders#standaloneSetup(Object...)
	 */
	public static MockMvcTester of(Object... controllers) {
		return of(Arrays.asList(controllers), StandaloneMockMvcBuilder::build);
	}

	/**
	 * Return a new instance using the specified {@linkplain HttpMessageConverter
	 * message converters}.
	 * <p>If none are specified, only basic assertions on the response body can
	 * be performed. Consider registering a suitable JSON converter for asserting
	 * against JSON data structures.
	 * @param httpMessageConverters the message converters to use
	 * @return a new instance using the specified converters
	 */
	public MockMvcTester withHttpMessageConverters(Iterable<HttpMessageConverter<?>> httpMessageConverters) {
		return new MockMvcTester(this.mockMvc, HttpMessageContentConverter.of(httpMessageConverters));
	}

	/**
	 * Prepare an HTTP GET request.
	 * <p>The returned builder can be wrapped in {@code assertThat} to enable
	 * assertions on the result. For multi-statements assertions, use
	 * {@link MockMvcRequestBuilder#exchange() exchange()} to assign the
	 * result. To control the time to wait for asynchronous request to complete
	 * on a per-request basis, use
	 * {@link MockMvcRequestBuilder#exchange(Duration) exchange(Duration)}.
	 * @return a request builder for specifying the target URI
	 */
	public MockMvcRequestBuilder get() {
		return method(HttpMethod.GET);
	}

	/**
	 * Prepare an HTTP HEAD request.
	 * <p>The returned builder can be wrapped in {@code assertThat} to enable
	 * assertions on the result. For multi-statements assertions, use
	 * {@link MockMvcRequestBuilder#exchange() exchange()} to assign the
	 * result. To control the time to wait for asynchronous request to complete
	 * on a per-request basis, use
	 * {@link MockMvcRequestBuilder#exchange(Duration) exchange(Duration)}.
	 * @return a request builder for specifying the target URI
	 */
	public MockMvcRequestBuilder head() {
		return method(HttpMethod.HEAD);
	}

	/**
	 * Prepare an HTTP POST request.
	 * <p>The returned builder can be wrapped in {@code assertThat} to enable
	 * assertions on the result. For multi-statements assertions, use
	 * {@link MockMvcRequestBuilder#exchange() exchange()} to assign the
	 * result. To control the time to wait for asynchronous request to complete
	 * on a per-request basis, use
	 * {@link MockMvcRequestBuilder#exchange(Duration) exchange(Duration)}.
	 * @return a request builder for specifying the target URI
	 */
	public MockMvcRequestBuilder post() {
		return method(HttpMethod.POST);
	}

	/**
	 * Prepare an HTTP PUT request.
	 * <p>The returned builder can be wrapped in {@code assertThat} to enable
	 * assertions on the result. For multi-statements assertions, use
	 * {@link MockMvcRequestBuilder#exchange() exchange()} to assign the
	 * result. To control the time to wait for asynchronous request to complete
	 * on a per-request basis, use
	 * {@link MockMvcRequestBuilder#exchange(Duration) exchange(Duration)}.
	 * @return a request builder for specifying the target URI
	 */
	public MockMvcRequestBuilder put() {
		return method(HttpMethod.PUT);
	}

	/**
	 * Prepare an HTTP PATCH request.
	 * <p>The returned builder can be wrapped in {@code assertThat} to enable
	 * assertions on the result. For multi-statements assertions, use
	 * {@link MockMvcRequestBuilder#exchange() exchange()} to assign the
	 * result. To control the time to wait for asynchronous request to complete
	 * on a per-request basis, use
	 * {@link MockMvcRequestBuilder#exchange(Duration) exchange(Duration)}.
	 * @return a request builder for specifying the target URI
	 */
	public MockMvcRequestBuilder patch() {
		return method(HttpMethod.PATCH);
	}

	/**
	 * Prepare an HTTP DELETE request.
	 * <p>The returned builder can be wrapped in {@code assertThat} to enable
	 * assertions on the result. For multi-statements assertions, use
	 * {@link MockMvcRequestBuilder#exchange() exchange()} to assign the
	 * result. To control the time to wait for asynchronous request to complete
	 * on a per-request basis, use
	 * {@link MockMvcRequestBuilder#exchange(Duration) exchange(Duration)}.
	 * @return a request builder for specifying the target URI
	 */
	public MockMvcRequestBuilder delete() {
		return method(HttpMethod.DELETE);
	}

	/**
	 * Prepare an HTTP OPTIONS request.
	 * <p>The returned builder can be wrapped in {@code assertThat} to enable
	 * assertions on the result. For multi-statements assertions, use
	 * {@link MockMvcRequestBuilder#exchange() exchange()} to assign the
	 * result. To control the time to wait for asynchronous request to complete
	 * on a per-request basis, use
	 * {@link MockMvcRequestBuilder#exchange(Duration) exchange(Duration)}.
	 * @return a request builder for specifying the target URI
	 */
	public MockMvcRequestBuilder options() {
		return method(HttpMethod.OPTIONS);
	}

	/**
	 * Prepare a request for the specified {@code HttpMethod}.
	 * <p>The returned builder can be wrapped in {@code assertThat} to enable
	 * assertions on the result. For multi-statements assertions, use
	 * {@link MockMvcRequestBuilder#exchange() exchange()} to assign the
	 * result. To control the time to wait for asynchronous request to complete
	 * on a per-request basis, use
	 * {@link MockMvcRequestBuilder#exchange(Duration) exchange(Duration)}.
	 * @return a request builder for specifying the target URI
	 */
	public MockMvcRequestBuilder method(HttpMethod method) {
		return new MockMvcRequestBuilder(method);
	}

	/**
	 * Perform a request using the given {@link RequestBuilder} and return a
	 * {@link MvcTestResult result} that can be used with standard
	 * {@link org.assertj.core.api.Assertions AssertJ} assertions.
	 * <p>Use only this method if you need to provide a custom
	 * {@link RequestBuilder}. For regular cases, users should initiate the
	 * configuration of the request using one of the methods available on
	 * this instance, e.g. {@link #get()} for HTTP GET.
	 * <p>Contrary to {@link MockMvc#perform(RequestBuilder)}, this does not
	 * throw an exception if the request fails with an unresolved exception.
	 * Rather, the result provides the exception, if any. Assuming that a
	 * {@link MockMvcRequestBuilders#post(URI) POST} request against
	 * {@code /boom} throws an {@code IllegalStateException}, the following
	 * asserts that the invocation has indeed failed with the expected error
	 * message:
	 * <pre><code class="java">assertThat(mvc.post().uri("/boom")))
	 *       .failure().isInstanceOf(IllegalStateException.class)
	 *       .hasMessage("Expected");
	 * </code></pre>
	 * @param requestBuilder used to prepare the request to execute
	 * @return an {@link MvcTestResult} to be wrapped in {@code assertThat}
	 * @see MockMvc#perform(RequestBuilder)
	 * @see #method(HttpMethod)
	 */
	public MvcTestResult perform(RequestBuilder requestBuilder) {
		Object result = getMvcResultOrFailure(requestBuilder);
		if (result instanceof MvcResult mvcResult) {
			return new DefaultMvcTestResult(mvcResult, null, this.contentConverter);
		}
		else {
			return new DefaultMvcTestResult(null, (Exception) result, this.contentConverter);
		}
	}

	private Object getMvcResultOrFailure(RequestBuilder requestBuilder) {
		try {
			return this.mockMvc.perform(requestBuilder).andReturn();
		}
		catch (Exception ex) {
			return ex;
		}
	}

	/**
	 * Execute the request using the specified {@link RequestBuilder}. If the
	 * request is processing asynchronously, wait at most the given
	 * {@code timeToWait} duration. If not specified, then fall back on the
	 * timeout value associated with the async request, see
	 * {@link org.springframework.mock.web.MockAsyncContext#setTimeout}.
	 */
	MvcTestResult exchange(RequestBuilder requestBuilder, @Nullable Duration timeToWait) {
		MvcTestResult result = perform(requestBuilder);
		if (result.getUnresolvedException() == null) {
			if (result.getRequest().isAsyncStarted()) {
				// Wait for async result before dispatching
				long waitMs = (timeToWait != null ? timeToWait.toMillis() : -1);
				result.getMvcResult().getAsyncResult(waitMs);

				// Perform ASYNC dispatch
				RequestBuilder dispatchRequest = servletContext -> {
					MockHttpServletRequest request = result.getMvcResult().getRequest();
					request.setDispatcherType(DispatcherType.ASYNC);
					request.setAsyncStarted(false);
					return request;
				};
				return perform(dispatchRequest);
			}
		}
		return result;
	}


	/**
	 * A builder for {@link MockHttpServletRequest} that supports AssertJ.
	 */
	public final class MockMvcRequestBuilder extends AbstractMockHttpServletRequestBuilder<MockMvcRequestBuilder>
			implements AssertProvider<MvcTestResultAssert> {

		private final HttpMethod httpMethod;

		private MockMvcRequestBuilder(HttpMethod httpMethod) {
			super(httpMethod);
			this.httpMethod = httpMethod;
		}

		/**
		 * Enable file upload support using multipart.
		 * @return a {@link MockMultipartMvcRequestBuilder} with the settings
		 * configured thus far
		 */
		public MockMultipartMvcRequestBuilder multipart() {
			return new MockMultipartMvcRequestBuilder(this);
		}

		/**
		 * Execute the request. If the request is processing asynchronously,
		 * wait at most the given timeout value associated with the async request,
		 * see {@link org.springframework.mock.web.MockAsyncContext#setTimeout}.
		 * <p>For simple assertions, you can wrap this builder in
		 * {@code assertThat} rather than calling this method explicitly:
		 * <pre><code class="java">
		 * // These two examples are equivalent
		 * assertThat(mvc.get().uri("/greet")).hasStatusOk();
		 * assertThat(mvc.get().uri("/greet").exchange()).hasStatusOk();
		 * </code></pre>
		 * <p>For assertions on the original asynchronous request that might
		 * still be in progress, use {@link #asyncExchange()}.
		 * @see #exchange(Duration) to customize the timeout for async requests
		 */
		public MvcTestResult exchange() {
			return MockMvcTester.this.exchange(this, null);
		}

		/**
		 * Execute the request and wait at most the given {@code timeToWait}
		 * duration for the asynchronous request to complete. If the request
		 * is not asynchronous, the {@code timeToWait} is ignored.
		 * <p>For assertions on the original asynchronous request that might
		 * still be in progress, use {@link #asyncExchange()}.
		 * @see #exchange()
		 */
		public MvcTestResult exchange(Duration timeToWait) {
			return MockMvcTester.this.exchange(this, timeToWait);
		}

		/**
		 * Execute the request and do not attempt to wait for the completion of
		 * an asynchronous request. Contrary to {@link #exchange()}, this returns
		 * the original result that might still be in progress.
		 */
		public MvcTestResult asyncExchange() {
			return MockMvcTester.this.perform(this);
		}

		@Override
		public MvcTestResultAssert assertThat() {
			return new MvcTestResultAssert(exchange(), MockMvcTester.this.contentConverter);
		}
	}

	/**
	 * A builder for {@link MockMultipartHttpServletRequest} that supports AssertJ.
	 */
	public final class MockMultipartMvcRequestBuilder
			extends AbstractMockMultipartHttpServletRequestBuilder<MockMultipartMvcRequestBuilder>
			implements AssertProvider<MvcTestResultAssert> {

		private MockMultipartMvcRequestBuilder(MockMvcRequestBuilder currentBuilder) {
			super(currentBuilder.httpMethod);
			merge(currentBuilder);
		}

		/**
		 * Execute the request. If the request is processing asynchronously,
		 * wait at most the given timeout value associated with the async request,
		 * see {@link org.springframework.mock.web.MockAsyncContext#setTimeout}.
		 * <p>For simple assertions, you can wrap this builder in
		 * {@code assertThat} rather than calling this method explicitly:
		 * <pre><code class="java">
		 * // These two examples are equivalent
		 * assertThat(mvc.get().uri("/greet")).hasStatusOk();
		 * assertThat(mvc.get().uri("/greet").exchange()).hasStatusOk();
		 * </code></pre>
		 * <p>For assertions on the original asynchronous request that might
		 * still be in progress, use {@link #asyncExchange()}.
		 * @see #exchange(Duration) to customize the timeout for async requests
		 */
		public MvcTestResult exchange() {
			return MockMvcTester.this.exchange(this, null);
		}

		/**
		 * Execute the request and wait at most the given {@code timeToWait}
		 * duration for the asynchronous request to complete. If the request
		 * is not asynchronous, the {@code timeToWait} is ignored.
		 * <p>For assertions on the original asynchronous request that might
		 * still be in progress, use {@link #asyncExchange()}.
		 * @see #exchange()
		 */
		public MvcTestResult exchange(Duration timeToWait) {
			return MockMvcTester.this.exchange(this, timeToWait);
		}

		/**
		 * Execute the request and do not attempt to wait for the completion of
		 * an asynchronous request. Contrary to {@link #exchange()}, this returns
		 * the original result that might still be in progress.
		 */
		public MvcTestResult asyncExchange() {
			return MockMvcTester.this.perform(this);
		}

		@Override
		public MvcTestResultAssert assertThat() {
			return new MvcTestResultAssert(exchange(), MockMvcTester.this.contentConverter);
		}
	}

}
