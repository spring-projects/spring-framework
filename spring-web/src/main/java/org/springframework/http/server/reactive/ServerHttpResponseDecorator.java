/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Wraps another {@link ServerHttpResponse} and delegates all methods to it.
 * Sub-classes can override specific methods selectively.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServerHttpResponseDecorator implements ServerHttpResponse {

	private final ServerHttpResponse delegate;


	public ServerHttpResponseDecorator(ServerHttpResponse delegate) {
		Assert.notNull(delegate, "Delegate is required");
		this.delegate = delegate;
	}


	public ServerHttpResponse getDelegate() {
		return this.delegate;
	}


	// ServerHttpResponse delegation methods...

	@Override
	public boolean setStatusCode(@Nullable HttpStatus status) {
		return getDelegate().setStatusCode(status);
	}

	@Override
	public HttpStatus getStatusCode() {
		return getDelegate().getStatusCode();
	}

	@Override
	public boolean setRawStatusCode(@Nullable Integer value) {
		return getDelegate().setRawStatusCode(value);
	}

	@Override
	public Integer getRawStatusCode() {
		return getDelegate().getRawStatusCode();
	}

	@Override
	public HttpHeaders getHeaders() {
		return getDelegate().getHeaders();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return getDelegate().getCookies();
	}

	@Override
	public void addCookie(ResponseCookie cookie) {
		getDelegate().addCookie(cookie);
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return getDelegate().bufferFactory();
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		getDelegate().beforeCommit(action);
	}

	@Override
	public boolean isCommitted() {
		return getDelegate().isCommitted();
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return getDelegate().writeWith(body);
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return getDelegate().writeAndFlushWith(body);
	}

	@Override
	public Mono<Void> setComplete() {
		return getDelegate().setComplete();
	}


	/**
	 * Return the native response of the underlying server API, if possible,
	 * also unwrapping {@link ServerHttpResponseDecorator} if necessary.
	 * @param response the response to check
	 * @param <T> the expected native response type
	 * @throws IllegalArgumentException if the native response can't be obtained
	 * @since 5.3.3
	 */
	public static <T> T getNativeResponse(ServerHttpResponse response) {
		if (response instanceof AbstractServerHttpResponse) {
			return ((AbstractServerHttpResponse) response).getNativeResponse();
		}
		else if (response instanceof ServerHttpResponseDecorator) {
			return getNativeResponse(((ServerHttpResponseDecorator) response).getDelegate());
		}
		else {
			throw new IllegalArgumentException(
					"Can't find native response in " + response.getClass().getName());
		}
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
	}

}
