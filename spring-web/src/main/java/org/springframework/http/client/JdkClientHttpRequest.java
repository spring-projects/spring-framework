/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * {@link ClientHttpRequest} implementation based on
 * JDK HTTP client.
 *
 * <p>Created via the {@link JdkClientHttpRequestFactory}.
 */
final class JdkClientHttpRequest extends AbstractBufferingClientHttpRequest {

	private final HttpClient httpClient;

	private final HttpMethod method;

	private final URI uri;

	private final boolean expectContinue;

	@Nullable
	private final Duration requestTimeout;

	JdkClientHttpRequest(HttpClient client, HttpMethod method, URI uri,
						 boolean expectContinue, @Nullable Duration requestTimeout) {
		this.httpClient = client;
		this.method = method;
		this.uri = uri;
		this.expectContinue = expectContinue;
		this.requestTimeout = requestTimeout;
	}


	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	@Deprecated
	public String getMethodValue() {
		return this.method.name();
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		HttpRequest.Builder builder = HttpRequest.newBuilder(this.uri);

		addHeaders(builder, headers);

		builder.method(this.method.name(), bufferedOutput.length == 0
				? HttpRequest.BodyPublishers.noBody()
				: HttpRequest.BodyPublishers.ofByteArray(bufferedOutput));

		if (expectContinue) {
			builder.expectContinue(true);
		}
		if (requestTimeout != null) {
			builder.timeout(requestTimeout);
		}

		HttpResponse<InputStream> response;
		try {
			response = this.httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return new JdkClientHttpResponse(response);
	}

	/**
	 * Add the given headers to the given HTTP request.
	 * @param builder the request builder to add the headers to
	 * @param headers the headers to add
	 */
	static void addHeaders(HttpRequest.Builder builder, HttpHeaders headers) {
		headers.forEach((headerName, headerValues) -> {
			if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)) {  // RFC 6265
				String headerValue = StringUtils.collectionToDelimitedString(headerValues, "; ");
				builder.header(headerName, headerValue);
			}
			else if (!HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName) &&
					!HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(headerName)) {
				for (String headerValue : headerValues) {
					builder.header(headerName, headerValue);
				}
			}
		});
	}

}
