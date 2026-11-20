/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.client.support;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.registry.HttpServiceGroupAdapter;
import org.springframework.web.service.registry.HttpServiceGroupConfigurer;

/**
 * Adapter for groups backed by {@link WebClient}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
@SuppressWarnings("unused")
public class WebClientHttpServiceGroupAdapter implements HttpServiceGroupAdapter<WebClient.Builder> {

	@Override
	public WebClient.Builder createClientBuilder() {
		return WebClient.builder();
	}

	@Override
	public Class<? extends HttpServiceGroupConfigurer<WebClient.Builder>> getConfigurerType() {
		return WebClientHttpServiceGroupConfigurer.class;
	}

	@Override
	public HttpExchangeAdapter createExchangeAdapter(WebClient.Builder clientBuilder) {
		return WebClientAdapter.create(clientBuilder.build());
	}

}
