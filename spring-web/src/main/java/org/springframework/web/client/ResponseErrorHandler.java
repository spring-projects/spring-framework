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
