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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Rossen Stoyanchev
 */
public class MockServerHttpResponse implements ServerHttpResponse {

	private HttpStatus status;

	private HttpHeaders headers = new HttpHeaders();

	private MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	private Publisher<DataBuffer> body;

	private DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();


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
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return this.cookies;
	}

	public Publisher<DataBuffer> getBody() {
		return this.body;
	}

	@Override
	public Mono<Void> writeWith(Publisher<DataBuffer> body) {
		this.body = body;
		return Flux.from(this.body).then();
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
		return this.dataBufferFactory;
	}

}
