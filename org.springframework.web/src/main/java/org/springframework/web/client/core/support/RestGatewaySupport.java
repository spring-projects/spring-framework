/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.client.core.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.web.client.core.RestTemplate;
import org.springframework.web.http.client.ClientHttpRequestFactory;

/**
 * Convenient super class for application classes that need REST access.
 *
 * <p>Requires a {@link ClientHttpRequestFactory} or a {@link RestTemplate} instance to be set. It will create its own
 * JmsTemplate if a ConnectionFactory is passed in. A custom JmsTemplate instance can be created for a given
 * ConnectionFactory through overriding the <code>createJmsTemplate</code> method.
 *
 * @author Arjen Poutsma
 * @see #setRestTemplate(RestTemplate)
 * @see RestTemplate
 * @since 3.0
 */
public class RestGatewaySupport {

	/**
	 * Logger available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	private RestTemplate restTemplate;

	/**
	 * Constructs a new instance of the {@link RestGatewaySupport}, with default parameters.
	 *
	 * @see RestTemplate#RestTemplate()
	 */
	public RestGatewaySupport() {
		restTemplate = new RestTemplate();
	}

	/**
	 * Constructs a new instance of the {@link RestGatewaySupport}, with the given {@link ClientHttpRequestFactory}.
	 *
	 * @see RestTemplate#RestTemplate(ClientHttpRequestFactory
	 */
	public RestGatewaySupport(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "'requestFactory' must not be null");
		this.restTemplate = new RestTemplate(requestFactory);
	}

	/**
	 * Returns the {@link RestTemplate} for the gateway.
	 */
	public RestTemplate getRestTemplate() {
		return restTemplate;
	}

	/**
	 * Sets the {@link RestTemplate} for the gateway.
	 */
	public void setRestTemplate(RestTemplate restTemplate) {
		Assert.notNull(restTemplate, "'restTemplate' must not be null");
		this.restTemplate = restTemplate;
	}

}
