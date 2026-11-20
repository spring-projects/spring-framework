/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;

/**
 * Abstract base class for listener-based server responses.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class AbstractListenerServerHttpResponse extends AbstractServerHttpResponse {

	private final AtomicBoolean writeCalled = new AtomicBoolean();


	public AbstractListenerServerHttpResponse(DataBufferFactory bufferFactory) {
		super(bufferFactory);
	}

	public AbstractListenerServerHttpResponse(DataBufferFactory bufferFactory, HttpHeaders headers) {
		super(bufferFactory, headers);
	}


	@Override
	protected final Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
		return writeAndFlushWithInternal(Mono.just(body));
	}

	@Override
	protected final Mono<Void> writeAndFlushWithInternal(
			Publisher<? extends Publisher<? extends DataBuffer>> body) {

		if (!this.writeCalled.compareAndSet(false, true)) {
			return Mono.error(new IllegalStateException(
					"writeWith() or writeAndFlushWith() has already been called"));
		}
		Processor<? super Publisher<? extends DataBuffer>, Void> processor = createBodyFlushProcessor();
		return Mono.from(subscriber -> {
			body.subscribe(processor);
			processor.subscribe(subscriber);
		});
	}

	/**
	 * Abstract template method to create a {@code Processor<Publisher<DataBuffer>, Void>}
	 * that will write the response body with flushes to the underlying output. Called from
	 * {@link #writeAndFlushWithInternal(Publisher)}.
	 */
	protected abstract Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor();

}
