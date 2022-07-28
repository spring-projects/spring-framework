/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Exceptions that contain actual HTTP request data.
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
public class WebClientRequestException extends WebClientException {

	private static final long serialVersionUID = -5139991985321385005L;


	private final HttpMethod method;

	private final URI uri;

	private final HttpHeaders headers;


	/**
	 * Constructor for throwable.
	 */
	public WebClientRequestException(Throwable ex, HttpMethod method, URI uri, HttpHeaders headers) {
		super(ex.getMessage(), ex);

		this.method = method;
		this.uri = uri;
		this.headers = copy(headers);
	}

	/**
	 * Not all {@code HttpHeaders} implementations are serializable, so we
	 * make a copy to ensure that {@code WebClientResponseException} is.
	 */
	private static HttpHeaders copy(HttpHeaders headers) {
		HttpHeaders result = new HttpHeaders();
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			for (String value : entry.getValue()) {
				result.add(entry.getKey(), value);
			}
		}
		return result;
	}


	/**
	 * Return the HTTP request method.
	 */
	public HttpMethod getMethod() {
		return this.method;
	}

	/**
	 * Return the request URI.
	 */
	public URI getUri() {
		return this.uri;
	}

	/**
	 * Return the HTTP request headers.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

}
