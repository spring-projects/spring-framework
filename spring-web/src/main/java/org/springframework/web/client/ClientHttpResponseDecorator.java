/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.client;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;


/**
 * Wrap and delegate to an existing {@link ClientHttpResponse}.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
class ClientHttpResponseDecorator implements ClientHttpResponse {

	private final ClientHttpResponse delegate;


	public ClientHttpResponseDecorator(ClientHttpResponse delegate) {
		Assert.notNull(delegate, "ClientHttpResponse delegate is required");
		this.delegate = delegate;
	}


	/**
	 * Return the wrapped response.
	 */
	public ClientHttpResponse getDelegate() {
		return this.delegate;
	}


	@Override
	public HttpStatusCode getStatusCode() throws IOException {
		return this.delegate.getStatusCode();
	}

	@Override
	public String getStatusText() throws IOException {
		return this.delegate.getStatusText();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.delegate.getHeaders();
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.delegate.getBody();
	}

	@Override
	public void close() {
		this.delegate.close();
	}

}
