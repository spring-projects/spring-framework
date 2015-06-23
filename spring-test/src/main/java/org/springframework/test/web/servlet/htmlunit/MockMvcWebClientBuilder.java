/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.test.web.servlet.htmlunit;

import com.gargoylesoftware.htmlunit.WebClient;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

/**
 * Simplifies creating a WebClient that delegates to a MockMvc instance.
 *
 * @author Rob Winch
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
	 * Creates a new instance with a WebApplicationContext.
	 *
	 * @param context the WebApplicationContext to use. Cannot be null.
	 * @return the MockMvcWebClientBuilder to customize
	 */
	public static MockMvcWebClientBuilder webAppContextSetup(WebApplicationContext context) {
		return new MockMvcWebClientBuilder(context);
	}

	/**
	 * Creates a new instance using a WebApplicationContext
	 * @param context the WebApplicationContext to create a MockMvc instance from.
	 * @param configurer the MockMvcConfigurer to apply
	 * Cannot be null.
	 * @return the MockMvcWebClientBuilder to use
	 */
	public static MockMvcWebClientBuilder webAppContextSetup(WebApplicationContext context, MockMvcConfigurer configurer) {
		return new MockMvcWebClientBuilder(context, configurer);
	}

	/**
	 * Creates a new instance with a MockMvc instance.
	 *
	 * @param mockMvc the MockMvc to use. Cannot be null.
	 * @return the MockMvcWebClientBuilder to customize
	 */
	public static MockMvcWebClientBuilder mockMvcSetup(MockMvc mockMvc) {
		return new MockMvcWebClientBuilder(mockMvc);
	}

	/**
	 * Creates a WebClient that uses the provided MockMvc for any matching requests and a
	 * WebClient with all the default settings for any other request.
	 *
	 * @return the WebClient to use
	 */
	public WebClient createWebClient() {
		return configureWebClient(new WebClient());
	}

	/**
	 * Creates a WebClient that uses the provided MockMvc for any matching requests and the
	 * provided WebClient for any other request.
	 *
	 * @param webClient The WebClient to delegate to for requests that do not match. Cannot be null.
	 *
	 * @return the WebClient to use
	 */
	public WebClient configureWebClient(WebClient webClient) {
		webClient.setWebConnection(createConnection(webClient.getWebConnection()));
		return webClient;
	}
}