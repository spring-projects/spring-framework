/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import org.jspecify.annotations.Nullable;

import org.springframework.http.client.reactive.ClientHttpConnector;
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
