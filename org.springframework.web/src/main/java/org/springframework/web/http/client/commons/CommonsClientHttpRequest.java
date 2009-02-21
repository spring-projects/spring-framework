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

package org.springframework.web.http.client.commons;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

import org.springframework.util.Assert;
import org.springframework.web.http.HttpHeaders;
import org.springframework.web.http.HttpMethod;
import org.springframework.web.http.client.ClientHttpRequest;
import org.springframework.web.http.client.ClientHttpResponse;

/**
 * {@link org.springframework.web.http.client.ClientHttpRequest} implementation that uses Commons Http Client to execute
 * requests. Created via the {@link CommonsClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @see CommonsClientHttpRequestFactory#createRequest(java.net.URI, HttpMethod)
 */
final class CommonsClientHttpRequest implements ClientHttpRequest {

	private final HttpClient httpClient;

	private final HttpMethodBase httpMethod;

	private final HttpHeaders headers = new HttpHeaders();

	private boolean headersWritten = false;

	private ByteArrayOutputStream bufferedOutput;

	CommonsClientHttpRequest(HttpClient httpClient, HttpMethodBase httpMethod) {
		this.httpClient = httpClient;
		this.httpMethod = httpMethod;
	}

	public HttpHeaders getHeaders() {
		return headers;
	}

	public OutputStream getBody() throws IOException {
		writeHeaders();
		Assert.isInstanceOf(EntityEnclosingMethod.class, httpMethod);
		this.bufferedOutput = new ByteArrayOutputStream();
		return bufferedOutput;
	}

	public HttpMethod getMethod() {
		return HttpMethod.valueOf(httpMethod.getName());
	}

	public ClientHttpResponse execute() throws IOException {
		writeHeaders();
		if (httpMethod instanceof EntityEnclosingMethod) {
			EntityEnclosingMethod entityEnclosingMethod = (EntityEnclosingMethod) httpMethod;
			RequestEntity requestEntity = new ByteArrayRequestEntity(bufferedOutput.toByteArray());
			entityEnclosingMethod.setRequestEntity(requestEntity);
		}
		httpClient.executeMethod(httpMethod);
		return new CommonsClientHttpResponse(httpMethod);
	}

	private void writeHeaders() {
		if (!headersWritten) {
			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				String headerName = entry.getKey();
				for (String headerValue : entry.getValue()) {
					httpMethod.addRequestHeader(headerName, headerValue);
				}
			}
			headersWritten = true;
		}
	}

}