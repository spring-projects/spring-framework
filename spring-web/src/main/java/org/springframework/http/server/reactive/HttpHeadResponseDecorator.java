/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.function.BiFunction;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;

/**
 * {@link ServerHttpResponse} decorator for HTTP HEAD requests.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpHeadResponseDecorator extends ServerHttpResponseDecorator {

	public HttpHeadResponseDecorator(ServerHttpResponse delegate) {
		super(delegate);
	}


	/**
	 * Apply {@link Flux#reduce(Object, BiFunction) reduce} on the body, count
	 * the number of bytes produced, release data buffers without writing, and
	 * set the {@literal Content-Length} header.
	 */
	@Override
	public final Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		// After Reactor Netty #171 is fixed we can return without delegating
		return getDelegate().writeWith(
				Flux.from(body)
						.reduce(0, (current, buffer) -> {
							int next = current + buffer.readableByteCount();
							DataBufferUtils.release(buffer);
							return next;
						})
						.doOnNext(count -> getHeaders().setContentLength(count))
						.then(Mono.empty()));
	}

	/**
	 * Invoke {@link #setComplete()} without writing.
	 * <p>RFC 7302 allows HTTP HEAD response without content-length and it's not
	 * something that can be computed on a streaming response.
	 */
	@Override
	public final Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		// Not feasible to count bytes on potentially streaming response.
		// RFC 7302 allows HEAD without content-length.
		return setComplete();
	}

}
