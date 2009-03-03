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
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * {@link org.springframework.http.client.ClientHttpRequest} implementation that uses
 * Apache Commons HttpClient to execute requests.
 *
 * <p>Created via the {@link CommonsClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @since 3.0
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
