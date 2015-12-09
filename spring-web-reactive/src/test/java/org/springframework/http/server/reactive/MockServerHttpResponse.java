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

import java.nio.ByteBuffer;

import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * @author Rossen Stoyanchev
 */
public class MockServerHttpResponse implements ServerHttpResponse {

	private HttpStatus status;

	private HttpHeaders headers = new HttpHeaders();

	private Publisher<ByteBuffer> body;


	@Override
	public void setStatusCode(HttpStatus status) {
		this.status = status;
	}

	public HttpStatus getStatus() {
		return this.status;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public Publisher<Void> writeHeaders() {
		return Publishers.empty();
	}

	@Override
	public Publisher<Void> setBody(Publisher<ByteBuffer> body) {
		this.body = body;
		return Publishers.empty();
	}

	public Publisher<ByteBuffer> getBody() {
		return this.body;
	}

}
