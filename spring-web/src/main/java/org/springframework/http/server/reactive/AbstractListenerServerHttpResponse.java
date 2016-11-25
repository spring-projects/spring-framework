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

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;

/**
 * Abstract base class for listener-based server responses, e.g. Servlet 3.1
 * and Undertow.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class AbstractListenerServerHttpResponse extends AbstractServerHttpResponse {

	private final AtomicBoolean writeCalled = new AtomicBoolean();


	public AbstractListenerServerHttpResponse(DataBufferFactory dataBufferFactory) {
		super(dataBufferFactory);
	}


	@Override
	protected final Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
		return writeAndFlushWithInternal(Mono.just(body));
	}

	@Override
	protected final Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		if (this.writeCalled.compareAndSet(false, true)) {
			Processor<? super Publisher<? extends DataBuffer>, Void> bodyProcessor = createBodyFlushProcessor();
			return Mono.from(subscriber -> {
				body.subscribe(bodyProcessor);
				bodyProcessor.subscribe(subscriber);
			});
		}
		else {
			return Mono.error(new IllegalStateException(
					"writeWith() or writeAndFlushWith() has already been called"));
		}
	}

	/**
	 * Abstract template method to create a {@code Processor<Publisher<DataBuffer>, Void>}
	 * that will write the response body with flushes to the underlying output. Called from
	 * {@link #writeAndFlushWithInternal(Publisher)}.
	 */
	protected abstract Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor();

}
