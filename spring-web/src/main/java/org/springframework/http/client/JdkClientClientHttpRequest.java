/*
 * Copyright 2023-2023 the original author or authors.
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
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * {@link ClientHttpRequest} implementation based on the Java {@code HttpClient}.
 *
 * @author Marten Deinum
 * @since 6.1
 */
public class JdkClientClientHttpRequest extends AbstractBufferingClientHttpRequest {

	/*
	 * The JDK HttpRequest doesn't allow all headers to be set. The named headers are taken from the default
	 * implementation for HttpRequest.
	 */
	private static final List<String> DISALLOWED_HEADERS =
			List.of("connection", "content-length", "expect", "host", "upgrade");

	private final HttpClient client;
	private final URI uri;
	private final HttpMethod method;
	public JdkClientClientHttpRequest(HttpClient client, URI uri, HttpMethod method) {
		this.client = client;
		this.uri = uri;
		this.method = method;
	}

	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] content) throws IOException {

		HttpRequest.Builder builder = HttpRequest.newBuilder(this.uri)
				.method(getMethod().name(), HttpRequest.BodyPublishers.ofByteArray(content));

		addHeaders(headers, builder);
		HttpRequest request = builder.build();
		HttpResponse<InputStream> response;
		try {
			response = this.client.send(request, HttpResponse.BodyHandlers.ofInputStream());
		} catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Request interupted.", ex);
		}
		return new JdkClientClientHttpResponse(response);
	}

	private static void addHeaders(HttpHeaders headers, HttpRequest.Builder builder) {
		headers.forEach((headerName, headerValues) -> {
			if (!DISALLOWED_HEADERS.contains(headerName.toLowerCase())) {
				for (String headerValue : headerValues) {
					builder.header(headerName, headerValue);
				}
			}
		});
	}
}
