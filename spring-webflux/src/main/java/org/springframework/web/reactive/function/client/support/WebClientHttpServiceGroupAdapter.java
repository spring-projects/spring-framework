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
