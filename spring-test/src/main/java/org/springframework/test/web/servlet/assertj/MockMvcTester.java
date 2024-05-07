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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * Test Spring MVC applications with {@link MockMvc} for server request handling
 * using {@link org.assertj.core.api.Assertions AssertJ}.
 *
 * <p>A tester instance can be created from a {@link WebApplicationContext}:
 * <pre><code class='java'>
 * // Create an instance with default settings
 * MockMvcTester mvc = MockMvcTester.from(applicationContext);
 *
 * // Create an instance with a custom Filter
 * MockMvcTester mvc = MockMvcTester.from(applicationContext,
 *         builder -> builder.addFilters(filter).build());
 * </code></pre>
 *
 * <p>A tester can be created standalone by providing the controller(s) to
 * include in a standalone setup:<pre><code class='java'>
 * // Create an instance for PersonController
 * MockMvcTester mvc = MockMvcTester.of(new PersonController());
 * </code></pre>
 *
 * <p>Once a test instance is available, you can perform requests in
 * a similar fashion as with {@link MockMvc}, and wrapping the result in
 * {@code assertThat} provides access to assertions. For instance:
 * <pre><code class='java'>
 * // perform a GET on /hi and assert the response body is equal to Hello
 * assertThat(mvc.perform(get("/hi")))
 *         .hasStatusOk().hasBodyTextEqualTo("Hello");
 * </code></pre>
 *
 * <p>A main difference with {@link MockMvc} is that an unresolved exception
 * is not thrown directly. Rather an {@link MvcTestResult} is available
 * with an {@link MvcTestResult#getUnresolvedException() unresolved
 * exception}. You can assert that a request has failed unexpectedly:
 * <pre><code class='java'>
 * // perform a GET on /hi and assert the response body is equal to Hello
 * assertThat(mvc.perform(get("/boom")))
 *         .hasUnresolvedException())
 * 		   .withMessage("Test exception");
 * </code></pre>
 *
 * <p>{@link MockMvcTester} can be configured with a list of
 * {@linkplain HttpMessageConverter message converters} to allow the response
 * body to be deserialized, rather than asserting on the raw values.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.2
 */
public final class MockMvcTester {

	private static final MediaType JSON = MediaType.APPLICATION_JSON;

	private final MockMvc mockMvc;

	@Nullable
	private final GenericHttpMessageConverter<Object> jsonMessageConverter;


	private MockMvcTester(MockMvc mockMvc, @Nullable GenericHttpMessageConverter<Object> jsonMessageConverter) {
		Assert.notNull(mockMvc, "mockMVC should not be null");
		this.mockMvc = mockMvc;
		this.jsonMessageConverter = jsonMessageConverter;
	}

	/**
	 * Create a {@link MockMvcTester} instance that delegates to the given
	 * {@link MockMvc} instance.
	 * @param mockMvc the MockMvc instance to delegate calls to
	 */
	public static MockMvcTester create(MockMvc mockMvc) {
		return new MockMvcTester(mockMvc, null);
	}

	/**
	 * Create an {@link MockMvcTester} instance using the given, fully
	 * initialized (i.e., <em>refreshed</em>) {@link WebApplicationContext}. The
	 * given {@code customizations} are applied to the {@link DefaultMockMvcBuilder}
	 * that ultimately creates the underlying {@link MockMvc} instance.
	 * <p>If no further customization of the underlying {@link MockMvc} instance
	 * is required, use {@link #from(WebApplicationContext)}.
	 * @param applicationContext the application context to detect the Spring
	 * MVC infrastructure and application controllers from
	 * @param customizations a function that creates a {@link MockMvc}
	 * instance based on a {@link DefaultMockMvcBuilder}.
	 * @see MockMvcBuilders#webAppContextSetup(WebApplicationContext)
	 */
	public static MockMvcTester from(WebApplicationContext applicationContext,
			Function<DefaultMockMvcBuilder, MockMvc> customizations) {

		DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(applicationContext);
		MockMvc mockMvc = customizations.apply(builder);
		return create(mockMvc);
	}

	/**
	 * Shortcut to create an {@link MockMvcTester} instance using the given,
	 * fully initialized (i.e., <em>refreshed</em>) {@link WebApplicationContext}.
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
	 * Create an {@link MockMvcTester} instance by registering one or more
	 * {@code @Controller} instances and configuring Spring MVC infrastructure
	 * programmatically.
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
	 * Shortcut to create an {@link MockMvcTester} instance by registering one
	 * or more {@code @Controller} instances.
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
	 * Return a new {@link MockMvcTester} instance using the specified
	 * {@linkplain HttpMessageConverter message converters}.
	 * <p>If none are specified, only basic assertions on the response body can
	 * be performed. Consider registering a suitable JSON converter for asserting
	 * against JSON data structures.
	 * @param httpMessageConverters the message converters to use
	 * @return a new instance using the specified converters
	 */
	public MockMvcTester withHttpMessageConverters(Iterable<HttpMessageConverter<?>> httpMessageConverters) {
		return new MockMvcTester(this.mockMvc, findJsonMessageConverter(httpMessageConverters));
	}

	/**
	 * Perform a request and return a {@link MvcTestResult result} that can be
	 * used with standard {@link org.assertj.core.api.Assertions AssertJ} assertions.
	 * <p>Use static methods of {@link MockMvcRequestBuilders} to prepare the
	 * request, wrapping the invocation in {@code assertThat}. The following
	 * asserts that a {@linkplain MockMvcRequestBuilders#get(URI) GET} request
	 * against "/greet" has an HTTP status code 200 (OK) and a simple body:
	 * <pre><code class='java'>assertThat(mvc.perform(get("/greet")))
	 *       .hasStatusOk()
	 *       .body().asString().isEqualTo("Hello");
	 * </code></pre>
	 * <p>Contrary to {@link MockMvc#perform(RequestBuilder)}, this does not
	 * throw an exception if the request fails with an unresolved exception.
	 * Rather, the result provides the exception, if any. Assuming that a
	 * {@link MockMvcRequestBuilders#post(URI) POST} request against
	 * {@code /boom} throws an {@code IllegalStateException}, the following
	 * asserts that the invocation has indeed failed with the expected error
	 * message:
	 * <pre><code class='java'>assertThat(mvc.perform(post("/boom")))
	 *       .unresolvedException().isInstanceOf(IllegalStateException.class)
	 *       .hasMessage("Expected");
	 * </code></pre>
	 * @param requestBuilder used to prepare the request to execute;
	 * see static factory methods in
	 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders}
	 * @return an {@link MvcTestResult} to be wrapped in {@code assertThat}
	 * @see MockMvc#perform(RequestBuilder)
	 */
	public MvcTestResult perform(RequestBuilder requestBuilder) {
		Object result = getMvcResultOrFailure(requestBuilder);
		if (result instanceof MvcResult mvcResult) {
			return new DefaultMvcTestResult(mvcResult, null, this.jsonMessageConverter);
		}
		else {
			return new DefaultMvcTestResult(null, (Exception) result, this.jsonMessageConverter);
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

	@SuppressWarnings("unchecked")
	@Nullable
	private GenericHttpMessageConverter<Object> findJsonMessageConverter(
			Iterable<HttpMessageConverter<?>> messageConverters) {

		return StreamSupport.stream(messageConverters.spliterator(), false)
				.filter(GenericHttpMessageConverter.class::isInstance)
				.map(GenericHttpMessageConverter.class::cast)
				.filter(converter -> converter.canWrite(null, Map.class, JSON))
				.filter(converter -> converter.canRead(Map.class, JSON))
				.findFirst().orElse(null);
	}

}
