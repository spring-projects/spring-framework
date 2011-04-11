/*
 * Copyright 2002-2011 the original author or authors.
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
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HTTP;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * {@link org.springframework.http.client.ClientHttpRequest} implementation that uses
 * Apache HTTPComponents HttpClient to execute requests.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequestFactory}.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @since 3.0
 * @see HttpComponentsClientHttpRequestFactory#createRequest(URI, HttpMethod)
 */
final class HttpComponentsClientHttpRequest extends AbstractBufferingClientHttpRequest {

	private final HttpClient httpClient;

	private final HttpUriRequest httpRequest;

	public HttpComponentsClientHttpRequest(HttpClient httpClient, HttpUriRequest httpRequest) {
		this.httpClient = httpClient;
		this.httpRequest = httpRequest;
	}

	public HttpMethod getMethod() {
		return HttpMethod.valueOf(httpRequest.getMethod());
	}

	public URI getURI() {
		return httpRequest.getURI();
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			if (!headerName.equalsIgnoreCase(HTTP.CONTENT_LEN) &&
					!headerName.equalsIgnoreCase(HTTP.TRANSFER_ENCODING)) {
				for (String headerValue : entry.getValue()) {
					httpRequest.addHeader(headerName, headerValue);
				}
			}
		}
		if (httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) httpRequest;
			HttpEntity requestEntity = new ByteArrayEntity(bufferedOutput);
			entityEnclosingRequest.setEntity(requestEntity);
		}
		HttpResponse httpResponse = httpClient.execute(httpRequest);
		return new HttpComponentsClientHttpResponse(httpResponse);

	}
}
