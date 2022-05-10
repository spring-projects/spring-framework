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

package org.springframework.http.client.reactive;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Base class for {@link ClientHttpRequest} implementations.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractClientHttpRequest implements ClientHttpRequest {

	/**
	 * COMMITTING -> COMMITTED is the period after doCommit is called but before
	 * the response status and headers have been applied to the underlying
	 * response during which time pre-commit actions can still make changes to
	 * the response status and headers.
	 */
	private enum State {NEW, COMMITTING, COMMITTED}


	private final HttpHeaders headers;

	private final MultiValueMap<String, HttpCookie> cookies;

	private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

	private final List<Supplier<? extends Publisher<Void>>> commitActions = new ArrayList<>(4);

	@Nullable
	private HttpHeaders readOnlyHeaders;


	public AbstractClientHttpRequest() {
		this(new HttpHeaders());
	}

	public AbstractClientHttpRequest(HttpHeaders headers) {
		Assert.notNull(headers, "HttpHeaders must not be null");
		this.headers = headers;
		this.cookies = new LinkedMultiValueMap<>();
	}


	@Override
	public HttpHeaders getHeaders() {
		if (this.readOnlyHeaders != null) {
			return this.readOnlyHeaders;
		}
		else if (State.COMMITTED.equals(this.state.get())) {
			this.readOnlyHeaders = initReadOnlyHeaders();
			return this.readOnlyHeaders;
		}
		else {
			return this.headers;
		}
	}

	/**
	 * Initialize the read-only headers after the request is committed.
	 * <p>By default, this method simply applies a read-only wrapper.
	 * Subclasses can do the same for headers from the native request.
	 * @since 5.3.15
	 */
	protected HttpHeaders initReadOnlyHeaders() {
		return HttpHeaders.readOnlyHttpHeaders(this.headers);
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		if (State.COMMITTED.equals(this.state.get())) {
			return CollectionUtils.unmodifiableMultiValueMap(this.cookies);
		}
		return this.cookies;
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		Assert.notNull(action, "Action must not be null");
		this.commitActions.add(action);
	}

	@Override
	public boolean isCommitted() {
		return (this.state.get() != State.NEW);
	}

	/**
	 * A variant of {@link #doCommit(Supplier)} for a request without body.
	 * @return a completion publisher
	 */
	protected Mono<Void> doCommit() {
		return doCommit(null);
	}

	/**
	 * Apply {@link #beforeCommit(Supplier) beforeCommit} actions, apply the
	 * request headers/cookies, and write the request body.
	 * @param writeAction the action to write the request body (may be {@code null})
	 * @return a completion publisher
	 */
	protected Mono<Void> doCommit(@Nullable Supplier<? extends Publisher<Void>> writeAction) {
		if (!this.state.compareAndSet(State.NEW, State.COMMITTING)) {
			return Mono.empty();
		}

		this.commitActions.add(() ->
				Mono.fromRunnable(() -> {
					applyHeaders();
					applyCookies();
					this.state.set(State.COMMITTED);
				}));

		if (writeAction != null) {
			this.commitActions.add(writeAction);
		}

		List<? extends Publisher<Void>> actions = this.commitActions.stream()
				.map(Supplier::get).collect(Collectors.toList());

		return Flux.concat(actions).then();
	}


	/**
	 * Apply header changes from {@link #getHeaders()} to the underlying request.
	 * This method is called once only.
	 */
	protected abstract void applyHeaders();

	/**
	 * Add cookies from {@link #getHeaders()} to the underlying request.
	 * This method is called once only.
	 */
	protected abstract void applyCookies();

}
