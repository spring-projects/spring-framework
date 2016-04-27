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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


/**
 * Base class for {@link ServerHttpResponse} implementations.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractServerHttpResponse implements ServerHttpResponse {

	private static final int STATE_NEW = 1;

	private static final int STATE_COMMITTING = 2;

	private static final int STATE_COMMITTED = 3;

	private final HttpHeaders headers;

	private final MultiValueMap<String, ResponseCookie> cookies;

	private final AtomicInteger state = new AtomicInteger(STATE_NEW);

	private final List<Supplier<? extends Mono<Void>>> beforeCommitActions = new ArrayList<>(4);

	private final DataBufferAllocator allocator;

	public AbstractServerHttpResponse(DataBufferAllocator allocator) {
		Assert.notNull(allocator, "'allocator' must not be null");

		this.allocator = allocator;
		this.headers = new HttpHeaders();
		this.cookies = new LinkedMultiValueMap<String, ResponseCookie>();
	}

	@Override
	public final DataBufferAllocator allocator() {
		return this.allocator;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (STATE_COMMITTED == this.state.get()) {
			return HttpHeaders.readOnlyHttpHeaders(this.headers);
		}
		else {
			return this.headers;
		}
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		if (STATE_COMMITTED == this.state.get()) {
			return CollectionUtils.unmodifiableMultiValueMap(this.cookies);
		}
		return this.cookies;
	}

	@Override
	public Mono<Void> setBody(Publisher<DataBuffer> publisher) {
		return new ChannelSendOperator<>(publisher, writePublisher ->
						applyBeforeCommit().then(() -> setBodyInternal(writePublisher)));
	}

	protected Mono<Void> applyBeforeCommit() {
		Mono<Void> mono = Mono.empty();
		if (this.state.compareAndSet(STATE_NEW, STATE_COMMITTING)) {
			for (Supplier<? extends Mono<Void>> action : this.beforeCommitActions) {
				mono = mono.then(action);
			}
			mono = mono.otherwise(ex -> {
				// Ignore errors from beforeCommit actions
				return Mono.empty();
			});
			mono = mono.then(() -> {
				this.state.set(STATE_COMMITTED);
				writeHeaders();
				writeCookies();
				return Mono.empty();
			});
		}
		return mono;
	}

	/**
	 * Implement this method to apply header changes from {@link #getHeaders()}
	 * to the underlying response. This method is called once only.
	 */
	protected abstract void writeHeaders();

	/**
	 * Implement this method to add cookies from {@link #getHeaders()} to the
	 * underlying response. This method is called once only.
	 */
	protected abstract void writeCookies();

	/**
	 * Implement this method to write to the underlying the response.
	 * @param publisher the publisher to write with
	 */
	protected abstract Mono<Void> setBodyInternal(Publisher<DataBuffer> publisher);

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		Assert.notNull(action);
		this.beforeCommitActions.add(action);
	}

	@Override
	public Mono<Void> setComplete() {
		return applyBeforeCommit();
	}

}
