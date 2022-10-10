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

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.protocol.HttpContext;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ClientHttpRequest} implementation based on
 * Apache HttpComponents HttpClient.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequestFactory}.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.1
 * @see HttpComponentsClientHttpRequestFactory#createRequest(URI, HttpMethod)
 */
final class HttpComponentsClientHttpRequest extends AbstractBufferingClientHttpRequest {

	private final HttpClient httpClient;

	private final ClassicHttpRequest httpRequest;

	private final HttpContext httpContext;


	HttpComponentsClientHttpRequest(HttpClient client, ClassicHttpRequest request, HttpContext context) {
		this.httpClient = client;
		this.httpRequest = request;
		this.httpContext = context;
	}


	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.httpRequest.getMethod());
	}

	@Override
	@Deprecated
	public String getMethodValue() {
		return this.httpRequest.getMethod();
	}

	@Override
	public URI getURI() {
		try {
			return this.httpRequest.getUri();
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
	}

	HttpContext getHttpContext() {
		return this.httpContext;
	}


	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		addHeaders(this.httpRequest, headers);

		ContentType contentType = ContentType.parse(headers.getFirst(HttpHeaders.CONTENT_TYPE));
		HttpEntity requestEntity = new ByteArrayEntity(bufferedOutput, contentType);
		this.httpRequest.setEntity(requestEntity);
		HttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);
		Assert.isInstanceOf(ClassicHttpResponse.class, httpResponse,
				"HttpResponse not an instance of ClassicHttpResponse");
		return new HttpComponentsClientHttpResponse((ClassicHttpResponse) httpResponse);
	}

	/**
	 * Add the given headers to the given HTTP request.
	 * @param httpRequest the request to add the headers to
	 * @param headers the headers to add
	 */
	static void addHeaders(ClassicHttpRequest httpRequest, HttpHeaders headers) {
		headers.forEach((headerName, headerValues) -> {
			if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)) {  // RFC 6265
				String headerValue = StringUtils.collectionToDelimitedString(headerValues, "; ");
				httpRequest.addHeader(headerName, headerValue);
			}
			else if (!HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName) &&
					!HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(headerName)) {
				for (String headerValue : headerValues) {
					httpRequest.addHeader(headerName, headerValue);
				}
			}
		});
	}

}
