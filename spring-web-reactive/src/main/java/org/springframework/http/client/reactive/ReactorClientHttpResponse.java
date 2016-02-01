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

import java.nio.ByteBuffer;

import reactor.core.publisher.Flux;
import reactor.io.buffer.Buffer;
import reactor.io.net.http.HttpChannel;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * {@link ClientHttpResponse} implementation for the Reactor Net HTTP client
 *
 * @author Brian Clozel
 * @see reactor.io.net.http.HttpClient
 */
public class ReactorClientHttpResponse implements ClientHttpResponse {

	private final DataBufferAllocator allocator;

	private final HttpChannel<Buffer, ByteBuffer> channel;


	public ReactorClientHttpResponse(HttpChannel channel, DataBufferAllocator allocator) {
		this.allocator = allocator;
		this.channel = channel;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return Flux.from(channel.input()).map(b -> allocator.wrap(b.byteBuffer()));
	}

	@Override
	public HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		this.channel.responseHeaders().entries().stream().forEach(e -> headers.add(e.getKey(), e.getValue()));
		return headers;
	}

	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.channel.responseStatus().getCode());
	}

	@Override
	public String toString() {
		return "ReactorClientHttpResponse{" +
				"request=" + this.channel.method() + " " + this.channel.uri().toString() + "," +
				"status=" + getStatusCode() +
				'}';
	}
}
