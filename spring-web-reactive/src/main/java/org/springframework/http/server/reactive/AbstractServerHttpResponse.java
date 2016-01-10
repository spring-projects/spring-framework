/*
 * Copyright 2002-2015 the original author or authors.
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

import java.nio.ByteBuffer;

import org.reactivestreams.Publisher;
import reactor.Flux;
import reactor.Mono;

import org.springframework.http.HttpHeaders;

/**
 * Base class for {@link ServerHttpResponse} implementations.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractServerHttpResponse implements ServerHttpResponse {

	private final HttpHeaders headers;

	private boolean headersWritten = false;


	protected AbstractServerHttpResponse() {
		this.headers = new HttpHeaders();
	}


	@Override
	public HttpHeaders getHeaders() {
		return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public Mono<Void> setBody(Publisher<ByteBuffer> publisher) {
		return Flux.from(publisher).lift(new WriteWithOperator<>(writeWithPublisher -> {
			writeHeaders();
			return setBodyInternal(writeWithPublisher);
		})).after();
	}

	/**
	 * Implement this method to write to the underlying the response.
	 * @param publisher the publisher to write with
	 */
	protected abstract Mono<Void> setBodyInternal(Publisher<ByteBuffer> publisher);

	@Override
	public void writeHeaders() {
		if (!this.headersWritten) {
			try {
				writeHeadersInternal();
			}
			finally {
				this.headersWritten = true;
			}
		}
	}

	/**
	 * Implement this method to apply header changes from {@link #getHeaders()}
	 * to the underlying response. This method is protected from being called
	 * more than once.
	 */
	protected abstract void writeHeadersInternal();

}
