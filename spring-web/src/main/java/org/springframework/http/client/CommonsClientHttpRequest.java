/*
 * Copyright 2002-2012 the original author or authors.
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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.URIException;
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
 * @deprecated In favor of {@link HttpComponentsClientHttpRequest}
 */
@Deprecated
final class CommonsClientHttpRequest extends AbstractBufferingClientHttpRequest {

	private final HttpClient httpClient;

	private final HttpMethodBase httpMethod;


	CommonsClientHttpRequest(HttpClient httpClient, HttpMethodBase httpMethod) {
		this.httpClient = httpClient;
		this.httpMethod = httpMethod;
	}


	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.httpMethod.getName());
	}

	@Override
	public URI getURI() {
		try {
			return URI.create(this.httpMethod.getURI().getEscapedURI());
		}
		catch (URIException ex) {
			throw new IllegalStateException("Could not get HttpMethod URI: " + ex.getMessage(), ex);
		}
	}

	@Override
	public ClientHttpResponse executeInternal(HttpHeaders headers, byte[] output) throws IOException {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			for (String headerValue : entry.getValue()) {
				httpMethod.addRequestHeader(headerName, headerValue);
			}
		}
		if (this.httpMethod instanceof EntityEnclosingMethod) {
			EntityEnclosingMethod entityEnclosingMethod = (EntityEnclosingMethod) this.httpMethod;
			RequestEntity requestEntity = new ByteArrayRequestEntity(output);
			entityEnclosingMethod.setRequestEntity(requestEntity);
		}
		this.httpClient.executeMethod(this.httpMethod);
		return new CommonsClientHttpResponse(this.httpMethod);
	}

}
