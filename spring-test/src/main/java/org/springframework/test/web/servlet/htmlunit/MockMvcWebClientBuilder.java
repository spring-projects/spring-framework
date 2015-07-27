/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.test.web.servlet.htmlunit;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import com.gargoylesoftware.htmlunit.WebClient;

/**
 * {@code MockMvcWebClientBuilder} simplifies the creation of a {@link WebClient}
 * that delegates to a {@link MockMvc} instance.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 */
public class MockMvcWebClientBuilder extends MockMvcWebConnectionBuilderSupport<MockMvcWebClientBuilder> {

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
	 * Create a new instance with the supplied {@link WebApplicationContext}.
	 * @param context the {@code WebApplicationContext} to create a {@link MockMvc}
	 * instance from; never {@code null}
	 * @return the MockMvcWebClientBuilder to customize
	 */
	public static MockMvcWebClientBuilder webAppContextSetup(WebApplicationContext context) {
		Assert.notNull(context, "WebApplicationContext must not be null");
		return new MockMvcWebClientBuilder(context);
	}

	/**
	 * Create a new instance with the supplied {@link WebApplicationContext}
	 * and {@link MockMvcConfigurer}.
	 * @param context the {@code WebApplicationContext} to create a {@link MockMvc}
	 * instance from; never {@code null}
	 * @param configurer the MockMvcConfigurer to apply; never {@code null}
	 * @return the MockMvcWebClientBuilder to customize
	 */
	public static MockMvcWebClientBuilder webAppContextSetup(WebApplicationContext context, MockMvcConfigurer configurer) {
		Assert.notNull(context, "WebApplicationContext must not be null");
		Assert.notNull(configurer, "MockMvcConfigurer must not be null");
		return new MockMvcWebClientBuilder(context, configurer);
	}

	/**
	 * Create a new instance with the supplied {@link MockMvc} instance.
	 * @param mockMvc the {@code MockMvc} instance to use; never {@code null}
	 * @return the MockMvcWebClientBuilder to customize
	 */
	public static MockMvcWebClientBuilder mockMvcSetup(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		return new MockMvcWebClientBuilder(mockMvc);
	}

	/**
	 * Create a {@link WebClient} that uses the configured {@link MockMvc}
	 * instance for any matching requests and a {@code WebClient} with all
	 * the default settings for any other requests.
	 * @return the {@code WebClient} to use
	 * @see #configureWebClient(WebClient)
	 */
	public WebClient createWebClient() {
		return configureWebClient(new WebClient());
	}

	/**
	 * Configure the supplied {@link WebClient} to use the configured
	 * {@link MockMvc} instance for any matching requests and the supplied
	 * {@code WebClient} for any other requests.
	 * @param webClient the WebClient to delegate to for requests that do not
	 * match; never {@code null}
	 * @return the WebClient to use
	 */
	public WebClient configureWebClient(WebClient webClient) {
		Assert.notNull(webClient, "webClient must not be null");
		webClient.setWebConnection(createConnection(webClient.getWebConnection()));
		return webClient;
	}

}