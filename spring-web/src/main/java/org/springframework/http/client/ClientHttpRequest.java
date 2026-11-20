/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpRequest;

/**
 * Represents a client-side HTTP request.
 * Created via an implementation of the {@link ClientHttpRequestFactory}.
 *
 * <p>A {@code ClientHttpRequest} can be {@linkplain #execute() executed},
 * receiving a {@link ClientHttpResponse} which can be read from.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see ClientHttpRequestFactory#createRequest(java.net.URI, HttpMethod)
 */
public interface ClientHttpRequest extends HttpRequest, HttpOutputMessage {

	/**
	 * Execute this request, resulting in a {@link ClientHttpResponse} that can be read.
	 * @return the response result of the execution
	 * @throws IOException in case of I/O errors
	 */
	ClientHttpResponse execute() throws IOException;

}
