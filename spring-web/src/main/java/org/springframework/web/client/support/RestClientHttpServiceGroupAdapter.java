/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client.support;

import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.registry.HttpServiceGroupAdapter;
import org.springframework.web.service.registry.HttpServiceGroupConfigurer;

/**
 * Adapter for groups backed by {@link RestClient}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
@SuppressWarnings("unused")
public class RestClientHttpServiceGroupAdapter implements HttpServiceGroupAdapter<RestClient.Builder> {

	@Override
	public RestClient.Builder createClientBuilder() {
		return RestClient.builder();
	}

	@Override
	public Class<? extends HttpServiceGroupConfigurer<RestClient.Builder>> getConfigurerType() {
		return RestClientHttpServiceGroupConfigurer.class;
	}

	@Override
	public HttpExchangeAdapter createExchangeAdapter(RestClient.Builder clientBuilder) {
		return RestClientAdapter.create(clientBuilder.build());
	}

}
