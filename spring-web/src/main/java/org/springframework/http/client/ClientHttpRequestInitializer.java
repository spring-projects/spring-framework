/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import org.springframework.http.client.support.HttpAccessor;

/**
 * Callback interface for initializing a {@link ClientHttpRequest} prior to it
 * being used.
 *
 * <p>Typically used with {@link HttpAccessor} and subclasses such as
 * {@link org.springframework.web.client.RestTemplate RestTemplate} to apply
 * consistent settings or headers to each request.
 *
 * <p>Unlike {@link ClientHttpRequestInterceptor}, this interface can apply
 * customizations without needing to read the entire request body into memory.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see HttpAccessor#getClientHttpRequestInitializers()
 */
@FunctionalInterface
public interface ClientHttpRequestInitializer {

	/**
	 * Initialize the given client HTTP request.
	 * @param request the request to configure
	 */
	void initialize(ClientHttpRequest request);

}
