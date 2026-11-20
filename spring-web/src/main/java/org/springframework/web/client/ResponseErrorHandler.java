/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Strategy interface used by the {@link RestTemplate} and {@link RestClient} to
 * determine whether a particular response has an error or not.
 *
 * <p>Note that {@code RestClient} also supports and recommends use of
 * {@link RestClient.ResponseSpec#onStatus status handlers}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface ResponseErrorHandler {

	/**
	 * Indicate whether the given response has any errors.
	 * <p>Implementations will typically inspect the
	 * {@link ClientHttpResponse#getStatusCode() HttpStatus} of the response.
	 * @param response the response to inspect
	 * @return {@code true} if the response indicates an error; {@code false} otherwise
	 * @throws IOException in case of I/O errors
	 */
	boolean hasError(ClientHttpResponse response) throws IOException;

	/**
	 * Handle the error in the given response.
	 * <p>This method is only called when {@link #hasError(ClientHttpResponse)}
	 * has returned {@code true}.
	 * @param url the request URL
	 * @param method the HTTP method
	 * @param response the response with the error
	 * @throws IOException in case of I/O errors
	 * @since 5.0
	 */
	default void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
	}

}
