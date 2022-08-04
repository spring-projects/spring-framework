/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.server;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Implementation of {@code ServerHttpResponse} that delegates all calls to a
 * given target {@code ServerHttpResponse}.
 *
 * @author Arjen Poutsma
 * @since 5.3.2
 */
public class DelegatingServerHttpResponse implements ServerHttpResponse {

	private final ServerHttpResponse delegate;

	/**
	 * Create a new {@code DelegatingServerHttpResponse}.
	 * @param delegate the response to delegate to
	 */
	public DelegatingServerHttpResponse(ServerHttpResponse delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * Returns the target response that this response delegates to.
	 * @return the delegate
	 */
	public ServerHttpResponse getDelegate() {
		return this.delegate;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		this.delegate.setStatusCode(status);
	}

	@Override
	public void flush() throws IOException {
		this.delegate.flush();
	}

	@Override
	public void close() {
		this.delegate.close();
	}

	@Override
	public OutputStream getBody() throws IOException {
		return this.delegate.getBody();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.delegate.getHeaders();
	}

}
