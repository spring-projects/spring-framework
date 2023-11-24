/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Contract to encapsulate customizations to a {@link WebTestClient.Builder}.
 * Typically used by frameworks that wish to provide a shortcut for common
 * initialization.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see MockServerConfigurer
 */
public interface WebTestClientConfigurer {

	/**
	 * Use methods on {@link WebTestClient.Builder} to modify test client
	 * settings. For a mock WebFlux server, use {@link WebHttpHandlerBuilder}
	 * to customize server configuration. For a MockMvc server, mutate the
	 * {@link org.springframework.test.web.servlet.client.MockMvcHttpConnector}
	 * and set it on {@link WebTestClient.Builder}.
	 * @param builder the WebTestClient builder for test client changes
	 * @param httpHandlerBuilder for mock WebFlux server settings
	 * @param connector the connector in use
	 */
	void afterConfigurerAdded(WebTestClient.Builder builder,
			@Nullable WebHttpHandlerBuilder httpHandlerBuilder,
			@Nullable ClientHttpConnector connector);

}
