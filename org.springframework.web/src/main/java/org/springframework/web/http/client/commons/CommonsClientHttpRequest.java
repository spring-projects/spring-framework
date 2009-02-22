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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

import org.springframework.web.http.HttpHeaders;
import org.springframework.web.http.HttpMethod;
import org.springframework.web.http.client.AbstractClientHttpRequest;
import org.springframework.web.http.client.ClientHttpResponse;

/**
 * {@link org.springframework.web.http.client.ClientHttpRequest} implementation that uses Commons Http Client to execute
 * requests. Created via the {@link CommonsClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @see CommonsClientHttpRequestFactory#createRequest(java.net.URI, HttpMethod)
 */
final class CommonsClientHttpRequest extends AbstractClientHttpRequest {

	private final HttpClient httpClient;

	private final HttpMethodBase httpMethod;

	CommonsClientHttpRequest(HttpClient httpClient, HttpMethodBase httpMethod) {
		this.httpClient = httpClient;
		this.httpMethod = httpMethod;
	}

	public HttpMethod getMethod() {
		return HttpMethod.valueOf(httpMethod.getName());
	}

	@Override
	public ClientHttpResponse executeInternal(HttpHeaders headers, byte[] output) throws IOException {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			for (String headerValue : entry.getValue()) {
				httpMethod.addRequestHeader(headerName, headerValue);
			}
		}
		if (httpMethod instanceof EntityEnclosingMethod) {
			EntityEnclosingMethod entityEnclosingMethod = (EntityEnclosingMethod) httpMethod;
			RequestEntity requestEntity = new ByteArrayRequestEntity(output);
			entityEnclosingMethod.setRequestEntity(requestEntity);
		}
		httpClient.executeMethod(httpMethod);
		return new CommonsClientHttpResponse(httpMethod);
	}

}
