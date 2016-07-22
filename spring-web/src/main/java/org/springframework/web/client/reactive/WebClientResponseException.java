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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;

/**
 * Base class for exceptions associated with specific HTTP client response
 * status codes.
 *
 * @author Brian Clozel
 * @since 5.0
 */
@SuppressWarnings("serial")
public class WebClientResponseException extends WebClientException {

	private final ClientHttpResponse clientResponse;

	private final List<HttpMessageReader<?>> messageReaders;


	/**
	 * Construct a new instance of {@code WebClientResponseException} with the given response data
	 * @param message the given error message
	 * @param clientResponse the HTTP response
	 * @param messageReaders the message converters that maay decode the HTTP response body
	 */
	public WebClientResponseException(String message, ClientHttpResponse clientResponse,
			List<HttpMessageReader<?>> messageReaders) {
		super(message);
		this.clientResponse = clientResponse;
		this.messageReaders = messageReaders;
	}


	/**
	 * Return the HTTP status
	 */
	public HttpStatus getStatus() {
		return this.clientResponse.getStatusCode();
	}

	/**
	 * Return the HTTP response headers
	 */
	public HttpHeaders getResponseHeaders() {
		return this.clientResponse.getHeaders();
	}

	/**
	 * Perform an extraction of the response body into a higher level representation.
	 *
	 * <pre class="code">
	 * static imports: ResponseExtractors.*
	 *
	 * String responseBody = clientResponse.getResponseBody(as(String.class));
	 * </pre>
	 */
	public <T> T getResponseBody(BodyExtractor<T> extractor) {
		return extractor.extract(this.clientResponse, this.messageReaders);
	}
}
