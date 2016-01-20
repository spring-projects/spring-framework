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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rx.Stream;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;


/**
 * Base class for {@link ServerHttpResponse} implementations.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractServerHttpResponse implements ServerHttpResponse {

	private final HttpHeaders headers;

	private AtomicReference<State> state = new AtomicReference<>(State.NEW);

	private final List<Supplier<? extends Mono<Void>>> beforeCommitActions = new ArrayList<>(4);


	protected AbstractServerHttpResponse() {
		this.headers = new HttpHeaders();
	}


	@Override
	public HttpHeaders getHeaders() {
		if (State.COMITTED.equals(this.state.get())) {
			return HttpHeaders.readOnlyHttpHeaders(this.headers);
		}
		return this.headers;
	}

	@Override
	public Mono<Void> setBody(Publisher<DataBuffer> publisher) {
		return Flux.from(publisher)
				.lift(new WriteWithOperator<>(writePublisher ->
						applyBeforeCommit().after(() -> setBodyInternal(writePublisher))))
				.after();
	}

	private Mono<Void> applyBeforeCommit() {
		return Stream.defer(() -> {
			Mono<Void> mono = Mono.empty();
			if (this.state.compareAndSet(State.NEW, State.COMMITTING)) {
				for (Supplier<? extends Mono<Void>> action : this.beforeCommitActions) {
					mono = mono.after(() -> action.get());
				}
				mono = mono.otherwise(ex -> {
					// Ignore errors from beforeCommit actions
					return Mono.empty();
				});
				mono = mono.after(() -> {
					this.state.set(State.COMITTED);
					writeHeaders();
					writeCookies();
					return Mono.empty();
				});
			}
			return mono;
		}).after();
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


	private enum State { NEW, COMMITTING, COMITTED }

}
