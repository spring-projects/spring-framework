/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.client.reactive;

import java.util.List;

import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;

/**
 * Strategy interface used by the {@link WebClient} to handle errors in
 * {@link ClientHttpResponse}s if needed.
 *
 * @author Brian Clozel
 * @see DefaultResponseErrorHandler
 * @since 5.0
 */
public interface ResponseErrorHandler {

	/**
	 * Handle the error in the given response.
	 * Implementations will typically inspect the
	 * {@link ClientHttpResponse#getStatusCode() HttpStatus} of the response and
	 * throw {@link WebClientException}s in case of errors.
	 */
	void handleError(ClientHttpResponse response, List<HttpMessageReader<?>> messageReaders);

}
