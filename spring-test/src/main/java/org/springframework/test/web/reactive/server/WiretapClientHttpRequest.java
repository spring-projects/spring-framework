/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;

/**
 * Client HTTP request decorator that intercepts and saves content written to
 * the server.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class WiretapClientHttpRequest extends ClientHttpRequestDecorator {

	private static final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	private final DataBuffer buffer;

	private final MonoProcessor<byte[]> body = MonoProcessor.create();


	public WiretapClientHttpRequest(ClientHttpRequest delegate) {
		super(delegate);
		this.buffer = bufferFactory.allocateBuffer();
	}


	/**
	 * Return a "promise" with the request body content written to the server.
	 */
	public MonoProcessor<byte[]> getRecordedContent() {
		return this.body;
	}


	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> publisher) {
		return super.writeWith(
				Flux.from(publisher)
						.doOnNext(this::handleOnNext)
						.doOnError(this::handleError)
						.doOnCancel(this::handleOnComplete)
						.doOnComplete(this::handleOnComplete));
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> publisher) {
		return super.writeAndFlushWith(
				Flux.from(publisher)
						.map(p -> Flux.from(p).doOnNext(this::handleOnNext).doOnError(this::handleError))
						.doOnError(this::handleError)
						.doOnCancel(this::handleOnComplete)
						.doOnComplete(this::handleOnComplete));
	}

	@Override
	public Mono<Void> setComplete() {
		handleOnComplete();
		return super.setComplete();
	}

	private void handleOnNext(DataBuffer buffer) {
		this.buffer.write(buffer);
	}

	private void handleError(Throwable ex) {
		if (!this.body.isTerminated()) {
			this.body.onError(ex);
		}
	}

	private void handleOnComplete() {
		if (!this.body.isTerminated()) {
			byte[] bytes = new byte[this.buffer.readableByteCount()];
			this.buffer.read(bytes);
			this.body.onNext(bytes);
		}
	}

}
