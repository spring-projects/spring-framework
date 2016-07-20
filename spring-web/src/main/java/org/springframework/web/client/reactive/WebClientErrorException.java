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
import org.springframework.http.converter.reactive.HttpMessageReader;

/**
 * Exception thrown when an HTTP 4xx is received.
 *
 * @author Brian Clozel
 * @since 5.0
 */
@SuppressWarnings("serial")
public class WebClientErrorException extends WebClientResponseException {


	/**
	 * Construct a new instance of {@code HttpClientErrorException} based on a
	 * {@link ClientHttpResponse} and {@link HttpMessageReader}s to optionally
	 * help decoding the response body
	 *
	 * @param response the HTTP response
	 * @param messageReaders the message converters that may decode the HTTP response body
	 */
	public WebClientErrorException(ClientHttpResponse response, List<HttpMessageReader<?>> messageReaders) {
		super(initMessage(response), response, messageReaders);
	}

	private static String initMessage(ClientHttpResponse response) {
		return response.getStatusCode().value() + " " + response.getStatusCode().getReasonPhrase();
	}

}
