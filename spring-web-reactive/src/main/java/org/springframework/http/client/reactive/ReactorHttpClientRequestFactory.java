/*
 * Copyright 2002-2016 the original author or authors.
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

import java.net.URI;

import reactor.io.netty.http.HttpClient;

import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.core.io.buffer.DefaultDataBufferAllocator;
import org.springframework.core.io.buffer.NettyDataBufferAllocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Create a {@link ClientHttpRequest} for the Reactor Net HTTP client
 *
 * @author Brian Clozel
 */
public class ReactorHttpClientRequestFactory implements ClientHttpRequestFactory {

	private final HttpClient httpClient;

	public ReactorHttpClientRequestFactory() {
		this(reactor.io.netty.http.HttpClient.create());
	}

	protected ReactorHttpClientRequestFactory(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	public ClientHttpRequest createRequest(HttpMethod httpMethod, URI uri, HttpHeaders headers) {
		Assert.notNull(httpMethod, "HTTP method is required");
		Assert.notNull(uri, "request URI is required");
		Assert.notNull(headers, "request headers are required");

		return new ReactorClientHttpRequest(httpMethod, uri, this.httpClient, headers);
	}

}
