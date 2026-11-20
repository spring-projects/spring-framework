/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;

/**
 * Factory for {@link ClientHttpRequest} objects.
 * Requests are created by the {@link #createRequest(URI, HttpMethod)} method.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@FunctionalInterface
public interface ClientHttpRequestFactory {

	/**
	 * Create a new {@link ClientHttpRequest} for the specified URI and HTTP method.
	 * <p>The returned request can be written to, and then executed by calling
	 * {@link ClientHttpRequest#execute()}.
	 * @param uri the URI to create a request for
	 * @param httpMethod the HTTP method to execute
	 * @return the created request
	 * @throws IOException in case of I/O errors
	 */
	ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException;

}
