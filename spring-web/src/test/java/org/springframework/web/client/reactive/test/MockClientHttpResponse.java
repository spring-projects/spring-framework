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

package org.springframework.web.client.reactive.test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Mock implementation of {@link ClientHttpResponse}.
 * @author Brian Clozel
 */
public class MockClientHttpResponse implements ClientHttpResponse {

	private HttpStatus status;

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	private Flux<DataBuffer> body = Flux.empty();

	@Override
	public HttpHeaders getHeaders() {
		return headers;
	}

	public MockClientHttpResponse addHeader(String name, String value) {
		getHeaders().add(name, value);
		return this;
	}

	public MockClientHttpResponse setHeader(String name, String value) {
		getHeaders().set(name, value);
		return this;
	}

	@Override
	public HttpStatus getStatusCode() {
		return this.status;
	}

	public void setStatus(HttpStatus status) {
		this.status = status;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	public MockClientHttpResponse setBody(Publisher<DataBuffer> body) {
		this.body = Flux.from(body);
		return this;
	}

	public MockClientHttpResponse setBody(String body) {
		DataBuffer buffer = toDataBuffer(body, StandardCharsets.UTF_8);
		this.body = Flux.just(buffer);
		return this;
	}

	public MockClientHttpResponse setBody(String body, Charset charset) {
		DataBuffer buffer = toDataBuffer(body, charset);
		this.body = Flux.just(buffer);
		return this;
	}

	private DataBuffer toDataBuffer(String body, Charset charset) {
		byte[] bytes = body.getBytes(charset);
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		return new DefaultDataBufferFactory().wrap(byteBuffer);
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return this.cookies;
	}
}
