/*
 * Copyright 2002-2009 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * Convenient super class for application classes that need REST access.
 *
 * <p>Requires a {@link ClientHttpRequestFactory} or a {@link RestTemplate} instance to be set.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see #setRestTemplate
 * @see org.springframework.web.client.RestTemplate
 */
public class RestGatewaySupport {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private RestTemplate restTemplate;


	/**
	 * Construct a new instance of the {@link RestGatewaySupport}, with default parameters.
	 */
	public RestGatewaySupport() {
		this.restTemplate = new RestTemplate();
	}

	/**
	 * Construct a new instance of the {@link RestGatewaySupport}, with the given {@link ClientHttpRequestFactory}.
	 * @see RestTemplate#RestTemplate(ClientHttpRequestFactory)
	 */
	public RestGatewaySupport(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "'requestFactory' must not be null");
		this.restTemplate = new RestTemplate(requestFactory);
	}


	/**
	 * Sets the {@link RestTemplate} for the gateway.
	 */
	public void setRestTemplate(RestTemplate restTemplate) {
		Assert.notNull(restTemplate, "'restTemplate' must not be null");
		this.restTemplate = restTemplate;
	}

	/**
	 * Returns the {@link RestTemplate} for the gateway.
	 */
	public RestTemplate getRestTemplate() {
		return this.restTemplate;
	}

}
