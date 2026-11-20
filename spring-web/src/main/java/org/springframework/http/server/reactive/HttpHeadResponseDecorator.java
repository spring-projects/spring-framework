/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;

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
	 * Consume and release the body without writing.
	 * <p>If the headers contain neither Content-Length nor Transfer-Encoding,
	 * and the body is a {@link Mono}, count the bytes and set Content-Length.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public final Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		if (shouldSetContentLength() && body instanceof Mono) {
			return ((Mono<? extends DataBuffer>) body)
					.doOnSuccess(buffer -> {
						if (buffer != null) {
							getHeaders().setContentLength(buffer.readableByteCount());
							DataBufferUtils.release(buffer);
						}
						else {
							getHeaders().setContentLength(0);
						}
					})
					.then();
		}
		else {
			return Flux.from(body)
					.doOnNext(DataBufferUtils::release)
					.then();
		}
	}

	private boolean shouldSetContentLength() {
		return (getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH) == null &&
				getHeaders().getFirst(HttpHeaders.TRANSFER_ENCODING) == null);
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
