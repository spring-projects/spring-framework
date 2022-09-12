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

package org.springframework.web.testfixture.http.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Mock implementation of {@link ClientHttpRequest}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class MockClientHttpRequest implements ClientHttpRequest {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private final HttpHeaders headers = new HttpHeaders();

	private HttpMethod httpMethod;

	private URI uri;

	private final ByteArrayOutputStream body = new ByteArrayOutputStream(1024);

	@Nullable
	private ClientHttpResponse clientHttpResponse;

	private boolean executed = false;


	public MockClientHttpRequest() {
		this.httpMethod = HttpMethod.GET;
		try {
			this.uri = new URI("/");
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	public MockClientHttpRequest(HttpMethod httpMethod, String urlTemplate, Object... vars) {
		this.httpMethod = httpMethod;
		this.uri = UriComponentsBuilder.fromUriString(urlTemplate).buildAndExpand(vars).encode().toUri();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public OutputStream getBody() throws IOException {
		return this.body;
	}

	public byte[] getBodyAsBytes() {
		return this.body.toByteArray();
	}

	public String getBodyAsString() {
		return getBodyAsString(DEFAULT_CHARSET);
	}

	public String getBodyAsString(Charset charset) {
		return StreamUtils.copyToString(this.body, charset);
	}

	public void setMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	@Deprecated
	public String getMethodValue() {
		return this.httpMethod.name();
	}

	public void setURI(URI uri) {
		this.uri = uri;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	public void setResponse(ClientHttpResponse clientHttpResponse) {
		this.clientHttpResponse = clientHttpResponse;
	}

	public boolean isExecuted() {
		return this.executed;
	}

	@Override
	public final ClientHttpResponse execute() throws IOException {
		this.executed = true;
		return executeInternal();
	}

	protected ClientHttpResponse executeInternal() throws IOException {
		Assert.state(this.clientHttpResponse != null, "No ClientHttpResponse");
		return this.clientHttpResponse;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.httpMethod);
		sb.append(' ').append(this.uri);
		if (!getHeaders().isEmpty()) {
			sb.append(", headers: ").append(getHeaders());
		}
		if (sb.length() == 0) {
			sb.append("Not yet initialized");
		}
		return sb.toString();
	}

}
