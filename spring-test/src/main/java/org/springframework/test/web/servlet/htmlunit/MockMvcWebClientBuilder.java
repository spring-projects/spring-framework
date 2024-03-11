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

package org.springframework.test.web.servlet.htmlunit;

import org.htmlunit.WebClient;

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@code MockMvcWebClientBuilder} simplifies the creation of an HtmlUnit
 * {@link WebClient} that delegates to a {@link MockMvc} instance.
 *
 * <p>The {@code MockMvc} instance used by the builder may be
 * {@linkplain #mockMvcSetup supplied directly} or created transparently
 * from a {@link #webAppContextSetup WebApplicationContext}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 * @see #mockMvcSetup(MockMvc)
 * @see #webAppContextSetup(WebApplicationContext)
 * @see #webAppContextSetup(WebApplicationContext, MockMvcConfigurer)
 * @see #withDelegate(WebClient)
 * @see #build()
 */
public class MockMvcWebClientBuilder extends MockMvcWebConnectionBuilderSupport<MockMvcWebClientBuilder> {

	@Nullable
	private WebClient webClient;


	protected MockMvcWebClientBuilder(MockMvc mockMvc) {
		super(mockMvc);
	}

	protected MockMvcWebClientBuilder(WebApplicationContext context) {
		super(context);
	}

	protected MockMvcWebClientBuilder(WebApplicationContext context, MockMvcConfigurer configurer) {
		super(context, configurer);
	}


	/**
	 * Create a new {@code MockMvcWebClientBuilder} based on the supplied
	 * {@link MockMvc} instance.
	 * @param mockMvc the {@code MockMvc} instance to use; never {@code null}
	 * @return the MockMvcWebClientBuilder to customize
	 */
	public static MockMvcWebClientBuilder mockMvcSetup(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		return new MockMvcWebClientBuilder(mockMvc);
	}

	/**
	 * Create a new {@code MockMvcWebClientBuilder} based on the supplied
	 * {@link WebApplicationContext}.
	 * @param context the {@code WebApplicationContext} to create a {@link MockMvc}
	 * instance from; never {@code null}
	 * @return the MockMvcWebClientBuilder to customize
	 */
	public static MockMvcWebClientBuilder webAppContextSetup(WebApplicationContext context) {
		Assert.notNull(context, "WebApplicationContext must not be null");
		return new MockMvcWebClientBuilder(context);
	}

	/**
	 * Create a new {@code MockMvcWebClientBuilder} based on the supplied
	 * {@link WebApplicationContext} and {@link MockMvcConfigurer}.
	 * @param context the {@code WebApplicationContext} to create a {@link MockMvc}
	 * instance from; never {@code null}
	 * @param configurer the {@code MockMvcConfigurer} to apply; never {@code null}
	 * @return the MockMvcWebClientBuilder to customize
	 */
	public static MockMvcWebClientBuilder webAppContextSetup(WebApplicationContext context, MockMvcConfigurer configurer) {
		Assert.notNull(context, "WebApplicationContext must not be null");
		Assert.notNull(configurer, "MockMvcConfigurer must not be null");
		return new MockMvcWebClientBuilder(context, configurer);
	}

	/**
	 * Supply the {@code WebClient} that the client {@linkplain #build built}
	 * by this builder should delegate to when processing
	 * non-{@linkplain WebRequestMatcher matching} requests.
	 * @param webClient the {@code WebClient} to delegate to for requests
	 * that do not match; never {@code null}
	 * @return this builder for further customization
	 * @see #build()
	 */
	public MockMvcWebClientBuilder withDelegate(WebClient webClient) {
		Assert.notNull(webClient, "WebClient must not be null");
		webClient.setWebConnection(createConnection(webClient));
		this.webClient = webClient;
		return this;
	}

	/**
	 * Build the {@link WebClient} configured via this builder.
	 * <p>The returned client will use the configured {@link MockMvc} instance
	 * for processing any {@linkplain WebRequestMatcher matching} requests
	 * and a delegate {@code WebClient} for all other requests.
	 * <p>If a {@linkplain #withDelegate delegate} has been explicitly configured,
	 * it will be used; otherwise, a default {@code WebClient} will be configured
	 * as the delegate.
	 * @return the {@code WebClient} to use
	 * @see #withDelegate(WebClient)
	 */
	public WebClient build() {
		return (this.webClient != null ? this.webClient : withDelegate(new WebClient()).build());
	}

}
