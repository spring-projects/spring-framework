/*
 * Copyright 2002-2015 the original author or authors.
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
import java.nio.ByteBuffer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * @author Rossen Stoyanchev
 */
public class MockServerHttpRequest implements ServerHttpRequest {

	private HttpMethod httpMethod;

	private URI uri;

	private HttpHeaders headers = new HttpHeaders();

	private Flux<ByteBuffer> body;


	public MockServerHttpRequest(HttpMethod httpMethod, URI uri) {
		this.httpMethod = httpMethod;
		this.uri = uri;
	}

	public MockServerHttpRequest(Publisher<ByteBuffer> body, HttpMethod httpMethod, URI uri) {
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

	public void setHeaders(HttpHeaders headers) {
		this.headers = headers;
	}

	@Override
	public Flux<ByteBuffer> getBody() {
		return this.body;
	}

	public void setBody(Publisher<ByteBuffer> body) {
		this.body = Flux.from(body);
	}
}
