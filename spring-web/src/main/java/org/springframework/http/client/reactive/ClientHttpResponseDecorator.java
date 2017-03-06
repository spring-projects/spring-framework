/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.http.client.reactive;

import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Wraps another {@link ClientHttpResponse} and delegates all methods to it.
 * Sub-classes can override specific methods selectively.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ClientHttpResponseDecorator implements ClientHttpResponse {
	
	private final ClientHttpResponse delegate;


	public ClientHttpResponseDecorator(ClientHttpResponse delegate) {
		Assert.notNull(delegate, "ClientHttpResponse delegate is required.");
		this.delegate = delegate;
	}


	public ClientHttpResponse getDelegate() {
		return this.delegate;
	}


	// ServerHttpResponse delegation methods...


	@Override
	public HttpStatus getStatusCode() {
		return this.delegate.getStatusCode();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.delegate.getHeaders();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return this.delegate.getCookies();
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.delegate.getBody();
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
	}

}
