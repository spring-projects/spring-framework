/*
 * Copyright 2002-2014 the original author or authors.
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
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * {@link org.springframework.http.client.ClientHttpRequest} implementation that uses
 * Apache HttpComponents HttpClient to execute requests.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequestFactory}.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @since 3.1
 * @see HttpComponentsClientHttpRequestFactory#createRequest(URI, HttpMethod)
 */
final class HttpComponentsClientHttpRequest extends AbstractBufferingClientHttpRequest {

	private final CloseableHttpClient httpClient;

	private final HttpUriRequest httpRequest;

	private final HttpContext httpContext;


	HttpComponentsClientHttpRequest(CloseableHttpClient httpClient, HttpUriRequest httpRequest, HttpContext httpContext) {
		this.httpClient = httpClient;
		this.httpRequest = httpRequest;
		this.httpContext = httpContext;
	}


	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.httpRequest.getMethod());
	}

	@Override
	public URI getURI() {
		return this.httpRequest.getURI();
	}


	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		addHeaders(this.httpRequest, headers);

		if (this.httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest entityEnclosingRequest =
					(HttpEntityEnclosingRequest) this.httpRequest;
			HttpEntity requestEntity = new ByteArrayEntity(bufferedOutput);
			entityEnclosingRequest.setEntity(requestEntity);
		}
		CloseableHttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);
		return new HttpComponentsClientHttpResponse(httpResponse);
	}


	/**
	 * Add the given headers to the given HTTP request.
	 * @param httpRequest the request to add the headers to
	 * @param headers the headers to add
	 */
	static void addHeaders(HttpUriRequest httpRequest, HttpHeaders headers) {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			if (!headerName.equalsIgnoreCase(HTTP.CONTENT_LEN) &&
					!headerName.equalsIgnoreCase(HTTP.TRANSFER_ENCODING)) {
				for (String headerValue : entry.getValue()) {
					httpRequest.addHeader(headerName, headerValue);
				}
			}
		}
	}

}
