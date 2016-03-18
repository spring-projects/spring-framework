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

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import reactor.core.converter.RxJava1ObservableConverter;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferAllocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation for the RxNetty HTTP client
 *
 * @author Brian Clozel
 */
public class RxNettyClientHttpResponse implements ClientHttpResponse {

	private final HttpClientResponse<ByteBuf> response;

	private final HttpHeaders headers;

	private final MultiValueMap<String, ResponseCookie> cookies;

	private final NettyDataBufferAllocator allocator;


	public RxNettyClientHttpResponse(HttpClientResponse<ByteBuf> response,
			NettyDataBufferAllocator allocator) {
		Assert.notNull("'request', request must not be null");
		Assert.notNull(allocator, "'allocator' must not be null");
		this.allocator = allocator;
		this.response = response;
		this.headers = new HttpHeaders();
		this.response.headerIterator().forEachRemaining(e -> this.headers.set(e.getKey(), e.getValue()));
		this.cookies = initCookies(response);
	}

	private static MultiValueMap<String, ResponseCookie> initCookies(HttpClientResponse<ByteBuf> response) {
		MultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();
		response.getCookies().values().stream().flatMap(Collection::stream)
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
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.response.getStatus().code());
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return RxJava1ObservableConverter.from(this.response.getContent().map(allocator::wrap));
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return this.cookies;
	}

}
