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

package org.springframework.mock.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.MockHttpOutputMessage;

/**
 * Mock implementation of {@link ClientHttpRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class MockClientHttpRequest extends MockHttpOutputMessage implements ClientHttpRequest {

	private URI uri;

	private HttpMethod httpMethod;

	private boolean executed = false;

	private ClientHttpResponse clientHttpResponse;


	/**
	 * Default constructor.
	 */
	public MockClientHttpRequest() {
	}

	/**
	 * Create an instance with the given HttpMethod and URI.
	 */
	public MockClientHttpRequest(HttpMethod httpMethod, URI uri) {
		this.httpMethod = httpMethod;
		this.uri = uri;
	}

	public URI getURI() {
		return this.uri;
	}

	public void setURI(URI uri) {
		this.uri = uri;
	}

	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	public void setMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	public void setResponse(ClientHttpResponse clientHttpResponse) {
		this.clientHttpResponse = clientHttpResponse;
	}

	public boolean isExecuted() {
		return this.executed;
	}

	/**
	 * Set the {@link #isExecuted() executed} flag to {@code true} and return the
	 * configured {@link #setResponse(ClientHttpResponse) response}.
	 * @see #executeInternal()
	 */
	public final ClientHttpResponse execute() throws IOException {
		this.executed = true;
		return executeInternal();
	}

	/**
	 * The default implementation returns the configured
	 * {@link #setResponse(ClientHttpResponse) response}.
	 *
	 * <p>Override this method to execute the request and provide a response,
	 * potentially different than the configured response.
	 */
	protected ClientHttpResponse executeInternal() throws IOException {
		return this.clientHttpResponse;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.httpMethod != null) {
			sb.append(this.httpMethod);
		}
		if (this.uri != null) {
			sb.append(" ").append(this.uri);
		}
		if (!getHeaders().isEmpty()) {
			sb.append(", headers : ").append(getHeaders());
		}
		if (sb.length() == 0) {
			sb.append("Not yet initialized");
		}
		return sb.toString();
	}

}
