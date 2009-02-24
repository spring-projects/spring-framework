/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

/**
 * {@link org.springframework.http.client.ClientHttpResponse} implementation that uses
 * Apache Commons HttpClient to execute requests.
 *
 * <p>Created via the {@link CommonsClientHttpRequest}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see CommonsClientHttpRequest#execute()
 */
final class CommonsClientHttpResponse implements ClientHttpResponse {

	private final HttpMethod httpMethod;

	private HttpHeaders headers;

	CommonsClientHttpResponse(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(httpMethod.getStatusCode());
	}

	public String getStatusText() {
		return httpMethod.getStatusText();
	}

	public HttpHeaders getHeaders() {
		if (headers == null) {
			headers = new HttpHeaders();
			for (Header header : httpMethod.getResponseHeaders()) {
				headers.add(header.getName(), header.getValue());
			}
		}
		return headers;
	}

	public InputStream getBody() throws IOException {
		return httpMethod.getResponseBodyAsStream();
	}

	public void close() {
		httpMethod.releaseConnection();
	}

}