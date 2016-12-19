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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Base class for {@link ServerHttpResponse} implementations.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class AbstractServerHttpResponse implements ServerHttpResponse {

	/**
	 * COMMITTING -> COMMITTED is the period after doCommit is called but before
	 * the response status and headers have been applied to the underlying
	 * response during which time pre-commit actions can still make changes to
	 * the response status and headers.
	 */
	private enum State {NEW, COMMITTING, COMMITTED};


	private final Log logger = LogFactory.getLog(getClass());

	private final DataBufferFactory dataBufferFactory;

	private HttpStatus statusCode;

	private final HttpHeaders headers;

	private final MultiValueMap<String, ResponseCookie> cookies;

	private Function<String, String> urlEncoder = url -> url;

	private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

	private final List<Supplier<? extends Mono<Void>>> commitActions = new ArrayList<>(4);


	public AbstractServerHttpResponse(DataBufferFactory dataBufferFactory) {
		Assert.notNull(dataBufferFactory, "'dataBufferFactory' must not be null");
		this.dataBufferFactory = dataBufferFactory;
		this.headers = new HttpHeaders();
		this.cookies = new LinkedMultiValueMap<>();
	}


	@Override
	public final DataBufferFactory bufferFactory() {
		return this.dataBufferFactory;
	}

	@Override
	public boolean setStatusCode(HttpStatus statusCode) {
		Assert.notNull(statusCode);
		if (this.state.get() == State.COMMITTED) {
			if (logger.isDebugEnabled()) {
				logger.debug("Can't set the status " + statusCode.toString() +
						" because the HTTP response has already been committed");
			}
			return false;
		}
		else {
			this.statusCode = statusCode;
			return true;
		}
	}

	@Override
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}

	@Override
	public HttpHeaders getHeaders() {
		return (this.state.get() == State.COMMITTED ?
				HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return (this.state.get() == State.COMMITTED ?
				CollectionUtils.unmodifiableMultiValueMap(this.cookies) : this.cookies);
	}

	@Override
	public String encodeUrl(String url) {
		return (this.urlEncoder != null ? this.urlEncoder.apply(url) : url);
	}

	@Override
	public void registerUrlEncoder(Function<String, String> encoder) {
		this.urlEncoder = (this.urlEncoder != null ? this.urlEncoder.andThen(encoder) : encoder);
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		if (action != null) {
			this.commitActions.add(action);
		}
	}

	@Override
	public final Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return new ChannelSendOperator<>(body,
				writePublisher -> doCommit(() -> writeWithInternal(writePublisher)));
	}

	@Override
	public final Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return new ChannelSendOperator<>(body,
				writePublisher -> doCommit(() -> writeAndFlushWithInternal(writePublisher)));
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit();
	}

	/**
	 * A variant of {@link #doCommit(Supplier)} for a response without no body.
	 * @return a completion publisher
	 */
	protected Mono<Void> doCommit() {
		return (this.state.get() == State.NEW ? doCommit(null) : Mono.empty());
	}

	/**
	 * Apply {@link #beforeCommit(Supplier) beforeCommit} actions, apply the
	 * response status and headers/cookies, and write the response body.
	 * @param writeAction the action to write the response body or {@code null}
	 * @return a completion publisher
	 */
	protected Mono<Void> doCommit(Supplier<? extends Mono<Void>> writeAction) {
		if (!this.state.compareAndSet(State.NEW, State.COMMITTING)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping doCommit (response already committed).");
			}
			return Mono.empty();
		}

		this.commitActions.add(() -> {
			applyStatusCode();
			applyHeaders();
			applyCookies();
			this.state.set(State.COMMITTED);
			return Mono.empty();
		});

		if (writeAction != null) {
			this.commitActions.add(writeAction);
		}

		List<? extends Mono<Void>> actions = this.commitActions.stream()
				.map(Supplier::get).collect(Collectors.toList());

		return Flux.concat(actions).next();
	}


	/**
	 * Implement this method to write to the underlying the response.
	 * @param body the publisher to write with
	 */
	protected abstract Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body);

	/**
	 * Implement this method to write to the underlying the response, and flush after
	 * each {@code Publisher<DataBuffer>}.
	 * @param body the publisher to write and flush with
	 */
	protected abstract Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body);

	/**
	 * Implement this method to write the status code to the underlying response.
	 * This method is called once only.
	 */
	protected abstract void applyStatusCode();

	/**
	 * Implement this method to apply header changes from {@link #getHeaders()}
	 * to the underlying response. This method is called once only.
	 */
	protected abstract void applyHeaders();

	/**
	 * Implement this method to add cookies from {@link #getHeaders()} to the
	 * underlying response. This method is called once only.
	 */
	protected abstract void applyCookies();

}
