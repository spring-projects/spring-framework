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
 * {@link MockMvc} variant that tests Spring MVC exchanges and provide fluent
 * assertions using {@link org.assertj.core.api.Assertions AssertJ}.
 *
 * <p>A main difference with {@link MockMvc} is that an unresolved exception
 * is not thrown directly. Rather an {@link AssertableMvcResult} is available
 * with an {@link AssertableMvcResult#getUnresolvedException() unresolved
 * exception}.
 *
 * <p>{@link AssertableMockMvc} can be configured with a list of
 * {@linkplain HttpMessageConverter HttpMessageConverters} to allow response
 * body to be deserialized, rather than asserting on the raw values.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.2
 */
public final class AssertableMockMvc {

	private static final MediaType JSON = MediaType.APPLICATION_JSON;

	private final MockMvc mockMvc;

	@Nullable
	private final GenericHttpMessageConverter<Object> jsonMessageConverter;


	private AssertableMockMvc(MockMvc mockMvc, @Nullable GenericHttpMessageConverter<Object> jsonMessageConverter) {
		Assert.notNull(mockMvc, "mockMVC should not be null");
		this.mockMvc = mockMvc;
		this.jsonMessageConverter = jsonMessageConverter;
	}

	/**
	 * Create a new {@link AssertableMockMvc} instance that delegates to the
	 * given {@link MockMvc}.
	 * @param mockMvc the MockMvc instance to delegate calls to
	 */
	public static AssertableMockMvc create(MockMvc mockMvc) {
		return new AssertableMockMvc(mockMvc, null);
	}

	/**
	 * Create a {@link AssertableMockMvc} instance using the given, fully
	 * initialized (i.e., <em>refreshed</em>) {@link WebApplicationContext}. The
	 * given {@code customizations} are applied to the {@link DefaultMockMvcBuilder}
	 * that ultimately creates the underlying {@link MockMvc} instance.
	 * <p>If no further customization of the underlying {@link MockMvc} instance
	 * is required, use {@link #from(WebApplicationContext)}.
	 * @param applicationContext the application context to detect the Spring
	 * MVC infrastructure and application controllers from
	 * @param customizations the function that creates a {@link MockMvc}
	 * instance based on a {@link DefaultMockMvcBuilder}.
	 * @see MockMvcBuilders#webAppContextSetup(WebApplicationContext)
	 */
	public static AssertableMockMvc from(WebApplicationContext applicationContext,
			Function<DefaultMockMvcBuilder, MockMvc> customizations) {

		DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(applicationContext);
		MockMvc mockMvc = customizations.apply(builder);
		return create(mockMvc);
	}

	/**
	 * Shortcut to create a {@link AssertableMockMvc} instance using the given,
	 * fully initialized (i.e., <em>refreshed</em>) {@link WebApplicationContext}.
	 * <p>Consider using {@link #from(WebApplicationContext, Function)} if
	 * further customizations of the underlying {@link MockMvc} instance is
	 * required.
	 * @param applicationContext the application context to detect the Spring
	 * MVC infrastructure and application controllers from
	 * @see MockMvcBuilders#webAppContextSetup(WebApplicationContext)
	 */
	public static AssertableMockMvc from(WebApplicationContext applicationContext) {
		return from(applicationContext, DefaultMockMvcBuilder::build);
	}

	/**
	 * Create a {@link AssertableMockMvc} instance by registering one or more
	 * {@code @Controller} instances and configuring Spring MVC infrastructure
	 * programmatically.
	 * <p>This allows full control over the instantiation and initialization of
	 * controllers and their dependencies, similar to plain unit tests while
	 * also making it possible to test one controller at a time.
	 * @param controllers one or more {@code @Controller} instances to test
	 * (specified {@code Class} will be turned into instance)
	 * @param customizations the function that creates a {@link MockMvc}
	 * instance based on a {@link StandaloneMockMvcBuilder}, typically to
	 * configure the Spring MVC infrastructure
	 * @see MockMvcBuilders#standaloneSetup(Object...)
	 */
	public static AssertableMockMvc of(Collection<?> controllers,
			Function<StandaloneMockMvcBuilder, MockMvc> customizations) {

		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(controllers.toArray());
		return create(customizations.apply(builder));
	}

	/**
	 * Shortcut to create a {@link AssertableMockMvc} instance by registering
	 * one or more {@code @Controller} instances.
	 * <p>The minimum infrastructure required by the
	 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
	 * to serve requests with annotated controllers is created. Consider using
	 * {@link #of(Collection, Function)} if additional configuration of the MVC
	 * infrastructure is required.
	 * @param controllers one or more {@code @Controller} instances to test
	 * (specified {@code Class} will be turned into instance)
	 * @see MockMvcBuilders#standaloneSetup(Object...)
	 */
	public static AssertableMockMvc of(Object... controllers) {
		return of(Arrays.asList(controllers), StandaloneMockMvcBuilder::build);
	}

	/**
	 * Return a new {@link AssertableMockMvc} instance using the specified
	 * {@link HttpMessageConverter}. If none are specified, only basic assertions
	 * on the response body can be performed. Consider registering a suitable
	 * JSON converter for asserting data structure.
	 * @param httpMessageConverters the message converters to use
	 * @return a new instance using the specified converters
	 */
	public AssertableMockMvc withHttpMessageConverters(Iterable<HttpMessageConverter<?>> httpMessageConverters) {
		return new AssertableMockMvc(this.mockMvc, findJsonMessageConverter(httpMessageConverters));
	}

	/**
	 * Perform a request and return a type that can be used with standard
	 * {@link org.assertj.core.api.Assertions AssertJ} assertions.
	 * <p>Use static methods of {@link MockMvcRequestBuilders} to prepare the
	 * request, wrapping the invocation in {@code assertThat}. The following
	 * asserts that a {@linkplain MockMvcRequestBuilders#get(URI) GET} request
	 * against "/greet" has an HTTP status code 200 (OK), and a simple body:
	 * <pre><code class='java'>assertThat(mvc.perform(get("/greet")))
	 *       .hasStatusOk()
	 *       .body().asString().isEqualTo("Hello");
	 * </code></pre>
	 * <p>Contrary to {@link MockMvc#perform(RequestBuilder)}, this does not
	 * throw an exception if the request fails with an unresolved exception.
	 * Rather, the result provides the exception, if any. Assuming that a
	 * {@linkplain MockMvcRequestBuilders#post(URI) POST} request against
	 * {@code /boom} throws an {@code IllegalStateException}, the following
	 * asserts that the invocation has indeed failed with the expected error
	 * message:
	 * <pre><code class='java'>assertThat(mvc.perform(post("/boom")))
	 *       .unresolvedException().isInstanceOf(IllegalStateException.class)
	 *       .hasMessage("Expected");
	 * </code></pre>
	 * <p>
	 * @param requestBuilder used to prepare the request to execute;
	 * see static factory methods in
	 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders}
	 * @return an {@link AssertableMvcResult} to be wrapped in {@code assertThat}
	 * @see MockMvc#perform(RequestBuilder)
	 */
	public AssertableMvcResult perform(RequestBuilder requestBuilder) {
		Object result = getMvcResultOrFailure(requestBuilder);
		if (result instanceof MvcResult mvcResult) {
			return new DefaultAssertableMvcResult(mvcResult, null, this.jsonMessageConverter);
		}
		else {
			return new DefaultAssertableMvcResult(null, (Exception) result, this.jsonMessageConverter);
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
