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

import reactor.core.publisher.Flux;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.client.reactive.ClientHttpResponseDecorator;

/**
 * Client HTTP response decorator that intercepts and saves the content read
 * from the server.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class WiretapClientHttpResponse extends ClientHttpResponseDecorator {

	private static final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	private final DataBuffer buffer;

	private final MonoProcessor<byte[]> body = MonoProcessor.create();


	public WiretapClientHttpResponse(ClientHttpResponse delegate) {
		super(delegate);
		this.buffer = bufferFactory.allocateBuffer();
	}


	/**
	 * Return a "promise" with the response body content read from the server.
	 */
	public MonoProcessor<byte[]> getRecordedContent() {
		return this.body;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return super.getBody()
				.doOnNext(buffer::write)
				.doOnError(body::onError)
				.doOnCancel(this::handleOnComplete)
				.doOnComplete(this::handleOnComplete);
	}

	private void handleOnComplete() {
		if (!this.body.isTerminated()) {
			byte[] bytes = new byte[this.buffer.readableByteCount()];
			this.buffer.read(bytes);
			this.body.onNext(bytes);
		}
	}

}
