/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * Convenient superclass for application classes that need REST access.
 *
 * <p>Requires a {@link ClientHttpRequestFactory} or a {@link RestTemplate} instance to be set.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see #setRestTemplate
 * @see org.springframework.web.client.RestTemplate
 */
public class RestGatewaySupport {

	/** Logger available to subclasses. */
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
