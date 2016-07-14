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
package org.springframework.http.server.reactive;

import java.net.URI;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Mock implementation of {@link ServerHttpRequest}.
 * @author Rossen Stoyanchev
 */
public class MockServerHttpRequest implements ServerHttpRequest {

	private HttpMethod httpMethod;

	private URI uri;

	private MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

	private HttpHeaders headers = new HttpHeaders();

	private MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

	private Flux<DataBuffer> body;


	public MockServerHttpRequest(HttpMethod httpMethod, URI uri) {
		this.httpMethod = httpMethod;
		this.uri = uri;
	}

	public MockServerHttpRequest(Publisher<DataBuffer> body, HttpMethod httpMethod,
			URI uri) {
		this.body = Flux.from(body);
		this.httpMethod = httpMethod;
		this.uri = uri;
	}


	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		return this.queryParams;
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		return this.cookies;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	public Mono<Void> writeWith(Publisher<DataBuffer> body) {
		this.body = Flux.from(body);
		return this.body.then();
	}
}
