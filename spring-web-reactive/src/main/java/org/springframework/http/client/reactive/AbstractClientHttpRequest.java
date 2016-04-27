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

package org.springframework.http.client.reactive;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Base class for {@link ClientHttpRequest} implementations.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public abstract class AbstractClientHttpRequest implements ClientHttpRequest {

	private final HttpHeaders headers;

	private final MultiValueMap<String, HttpCookie> cookies;

	private AtomicReference<State> state = new AtomicReference<>(State.NEW);

	private final List<Supplier<? extends Mono<Void>>> beforeCommitActions = new ArrayList<>(4);

	public AbstractClientHttpRequest(HttpHeaders httpHeaders) {
		if (httpHeaders == null) {
			this.headers = new HttpHeaders();
		}
		else {
			this.headers = httpHeaders;
		}
		this.cookies = new LinkedMultiValueMap<>();
	}

	@Override
	public HttpHeaders getHeaders() {
		if (State.COMITTED.equals(this.state.get())) {
			return HttpHeaders.readOnlyHttpHeaders(this.headers);
		}
		return this.headers;
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		if (State.COMITTED.equals(this.state.get())) {
			return CollectionUtils.unmodifiableMultiValueMap(this.cookies);
		}
		return this.cookies;
	}

	protected Mono<Void> applyBeforeCommit() {
		Mono<Void> mono = Mono.empty();
		if (this.state.compareAndSet(State.NEW, State.COMMITTING)) {
			for (Supplier<? extends Mono<Void>> action : this.beforeCommitActions) {
				mono = mono.then(() -> action.get());
			}
			return mono
					.otherwise(ex -> {
						// Ignore errors from beforeCommit actions
						return Mono.empty();
					})
					.then(() -> {
						this.state.set(State.COMITTED);
						//writeHeaders();
						//writeCookies();
						return Mono.empty();
					});
		}
		return mono;
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		Assert.notNull(action);
		this.beforeCommitActions.add(action);
	}

	private enum State {NEW, COMMITTING, COMITTED}
}
