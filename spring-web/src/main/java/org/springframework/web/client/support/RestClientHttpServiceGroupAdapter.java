/*
 * Copyright 2002-present the original author or authors.
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
