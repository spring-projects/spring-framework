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

import java.util.Collection;

import reactor.core.publisher.Flux;
import reactor.io.netty.http.HttpInbound;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation for the Reactor Net HTTP client
 *
 * @author Brian Clozel
 * @see reactor.io.netty.http.HttpClient
 */
public class ReactorClientHttpResponse implements ClientHttpResponse {

	private final DataBufferAllocator allocator;

	private final HttpInbound channel;


	public ReactorClientHttpResponse(HttpInbound channel, DataBufferAllocator allocator) {
		this.allocator = allocator;
		this.channel = channel;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return channel.receive().map(b -> allocator.wrap(b.byteBuffer()));
	}

	@Override
	public HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		this.channel.responseHeaders().entries().stream().forEach(e -> headers.add(e.getKey(), e.getValue()));
		return headers;
	}

	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.channel.status().code());
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		MultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();
		this.channel.cookies().values().stream().flatMap(Collection::stream)
				.forEach(cookie -> {
					ResponseCookie responseCookie = ResponseCookie.from(cookie.name(), cookie.value())
							.domain(cookie.domain())
							.path(cookie.path())
							.maxAge(cookie.maxAge())
							.secure(cookie.isSecure())
							.httpOnly(cookie.isHttpOnly())
							.build();
					result.add(cookie.name(), responseCookie);
				});
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

	@Override
	public String toString() {
		return "ReactorClientHttpResponse{" +
				"request=" + this.channel.method().name() + " " + this.channel.uri() + "," +
				"status=" + getStatusCode() +
				'}';
	}
}
