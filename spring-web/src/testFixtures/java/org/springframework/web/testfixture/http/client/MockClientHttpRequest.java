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

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Mock implementation of {@link ClientHttpRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 3.2
 */
public class MockClientHttpRequest extends MockHttpOutputMessage implements ClientHttpRequest {

	private HttpMethod httpMethod;

	private URI uri;

	@Nullable
	private ClientHttpResponse clientHttpResponse;

	private boolean executed = false;


	/**
	 * Create a {@code MockClientHttpRequest} with {@link HttpMethod#GET GET} as
	 * the HTTP request method and {@code "/"} as the {@link URI}.
	 */
	public MockClientHttpRequest() {
		this(HttpMethod.GET, URI.create("/"));
	}

	/**
	 * Create a {@code MockClientHttpRequest} with the given {@link HttpMethod},
	 * URI template, and URI template variable values.
	 * @since 6.0.3
	 */
	public MockClientHttpRequest(HttpMethod httpMethod, String uriTemplate, Object... vars) {
		this(httpMethod, UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(vars).encode().toUri());
	}

	/**
	 * Create a {@code MockClientHttpRequest} with the given {@link HttpMethod}
	 * and {@link URI}.
	 */
	public MockClientHttpRequest(HttpMethod httpMethod, URI uri) {
		this.httpMethod = httpMethod;
		this.uri = uri;
	}


	/**
	 * Set the HTTP method of the request.
	 */
	public void setMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	/**
	 * Set the URI of the request.
	 */
	public void setURI(URI uri) {
		this.uri = uri;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	/**
	 * Set the {@link ClientHttpResponse} to be used as the result of executing
	 * the this request.
	 * @see #execute()
	 */
	public void setResponse(ClientHttpResponse clientHttpResponse) {
		this.clientHttpResponse = clientHttpResponse;
	}

	/**
	 * Get the {@link #isExecuted() executed} flag.
	 * @see #execute()
	 */
	public boolean isExecuted() {
		return this.executed;
	}

	/**
	 * Set the {@link #isExecuted() executed} flag to {@code true} and return the
	 * configured {@link #setResponse(ClientHttpResponse) response}.
	 * @see #executeInternal()
	 */
	@Override
	public final ClientHttpResponse execute() throws IOException {
		this.executed = true;
		return executeInternal();
	}

	/**
	 * The default implementation returns the configured
	 * {@link #setResponse(ClientHttpResponse) response}.
	 * <p>Override this method to execute the request and provide a response,
	 * potentially different from the configured response.
	 */
	protected ClientHttpResponse executeInternal() throws IOException {
		Assert.state(this.clientHttpResponse != null, "No ClientHttpResponse");
		return this.clientHttpResponse;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.httpMethod).append(' ').append(this.uri);
		if (!getHeaders().isEmpty()) {
			sb.append(", headers: ").append(getHeaders());
		}
		return sb.toString();
	}

}
