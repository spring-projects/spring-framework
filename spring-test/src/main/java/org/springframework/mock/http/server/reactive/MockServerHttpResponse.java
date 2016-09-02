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

package org.springframework.mock.http.server.reactive;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Mock implementation of {@link ServerHttpResponse}.
 * @author Rossen Stoyanchev
 */
public class MockServerHttpResponse implements ServerHttpResponse {

	private HttpStatus status;

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	private Publisher<DataBuffer> body;

	private Publisher<Publisher<DataBuffer>> bodyWithFlushes;

	private DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	@Override
	public boolean setStatusCode(HttpStatus status) {
		this.status = status;
		return true;
	}

	public HttpStatus getStatusCode() {
		return this.status;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return this.cookies;
	}

	public Publisher<DataBuffer> getBody() {
		return this.body;
	}

	public Publisher<Publisher<DataBuffer>> getBodyWithFlush() {
		return this.bodyWithFlushes;
	}

	@Override
	public Mono<Void> writeWith(Publisher<DataBuffer> body) {
		this.body = body;
		return Flux.from(this.body).then();
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<Publisher<DataBuffer>> body) {
		this.bodyWithFlushes = body;
		return Flux.from(this.bodyWithFlushes).then();
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
	}

	@Override
	public Mono<Void> setComplete() {
		return Mono.empty();
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return this.bufferFactory;
	}

}
