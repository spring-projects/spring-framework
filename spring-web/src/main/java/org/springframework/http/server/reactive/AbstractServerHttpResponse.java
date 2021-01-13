/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Base class for {@link ServerHttpResponse} implementations.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractServerHttpResponse implements ServerHttpResponse {

	/**
	 * COMMITTING -> COMMITTED is the period after doCommit is called but before
	 * the response status and headers have been applied to the underlying
	 * response during which time pre-commit actions can still make changes to
	 * the response status and headers.
	 */
	private enum State {NEW, COMMITTING, COMMIT_ACTION_FAILED, COMMITTED}


	private final DataBufferFactory dataBufferFactory;

	@Nullable
	private Integer statusCode;

	private final HttpHeaders headers;

	private final MultiValueMap<String, ResponseCookie> cookies;

	private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

	private final List<Supplier<? extends Mono<Void>>> commitActions = new ArrayList<>(4);

	@Nullable
	private HttpHeaders readOnlyHeaders;


	public AbstractServerHttpResponse(DataBufferFactory dataBufferFactory) {
		this(dataBufferFactory, new HttpHeaders());
	}

	public AbstractServerHttpResponse(DataBufferFactory dataBufferFactory, HttpHeaders headers) {
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		Assert.notNull(headers, "HttpHeaders must not be null");
		this.dataBufferFactory = dataBufferFactory;
		this.headers = headers;
		this.cookies = new LinkedMultiValueMap<>();
	}


	@Override
	public final DataBufferFactory bufferFactory() {
		return this.dataBufferFactory;
	}

	@Override
	public boolean setStatusCode(@Nullable HttpStatus status) {
		if (this.state.get() == State.COMMITTED) {
			return false;
		}
		else {
			this.statusCode = (status != null ? status.value() : null);
			return true;
		}
	}

	@Override
	@Nullable
	public HttpStatus getStatusCode() {
		return (this.statusCode != null ? HttpStatus.resolve(this.statusCode) : null);
	}

	@Override
	public boolean setRawStatusCode(@Nullable Integer statusCode) {
		if (this.state.get() == State.COMMITTED) {
			return false;
		}
		else {
			this.statusCode = statusCode;
			return true;
		}
	}

	@Override
	@Nullable
	public Integer getRawStatusCode() {
		return this.statusCode;
	}

	/**
	 * Set the HTTP status code of the response.
	 * @param statusCode the HTTP status as an integer value
	 * @since 5.0.1
	 * @deprecated as of 5.2.4 in favor of {@link ServerHttpResponse#setRawStatusCode(Integer)}.
	 */
	@Deprecated
	public void setStatusCodeValue(@Nullable Integer statusCode) {
		if (this.state.get() != State.COMMITTED) {
			this.statusCode = statusCode;
		}
	}

	/**
	 * Return the HTTP status code of the response.
	 * @return the HTTP status as an integer value
	 * @since 5.0.1
	 * @deprecated as of 5.2.4 in favor of {@link ServerHttpResponse#getRawStatusCode()}.
	 */
	@Nullable
	@Deprecated
	public Integer getStatusCodeValue() {
		return this.statusCode;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.readOnlyHeaders != null) {
			return this.readOnlyHeaders;
		}
		else if (this.state.get() == State.COMMITTED) {
			this.readOnlyHeaders = HttpHeaders.readOnlyHttpHeaders(this.headers);
			return this.readOnlyHeaders;
		}
		else {
			return this.headers;
		}
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return (this.state.get() == State.COMMITTED ?
				CollectionUtils.unmodifiableMultiValueMap(this.cookies) : this.cookies);
	}

	@Override
	public void addCookie(ResponseCookie cookie) {
		Assert.notNull(cookie, "ResponseCookie must not be null");

		if (this.state.get() == State.COMMITTED) {
			throw new IllegalStateException("Can't add the cookie " + cookie +
					"because the HTTP response has already been committed");
		}
		else {
			getCookies().add(cookie.getName(), cookie);
		}
	}

	/**
	 * Return the underlying server response.
	 * <p><strong>Note:</strong> This is exposed mainly for internal framework
	 * use such as WebSocket upgrades in the spring-webflux module.
	 */
	public abstract <T> T getNativeResponse();


	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		this.commitActions.add(action);
	}

	@Override
	public boolean isCommitted() {
		State state = this.state.get();
		return (state != State.NEW && state != State.COMMIT_ACTION_FAILED);
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		// For Mono we can avoid ChannelSendOperator and Reactor Netty is more optimized for Mono.
		// We must resolve value first however, for a chance to handle potential error.
		if (body instanceof Mono) {
			return ((Mono<? extends DataBuffer>) body)
					.flatMap(buffer -> {
						touchDataBuffer(buffer);
						AtomicBoolean subscribed = new AtomicBoolean();
						return doCommit(
								() -> {
									try {
										return writeWithInternal(Mono.fromCallable(() -> buffer)
												.doOnSubscribe(s -> subscribed.set(true))
												.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release));
									}
									catch (Throwable ex) {
										return Mono.error(ex);
									}
								})
								.doOnError(ex -> DataBufferUtils.release(buffer))
								.doOnCancel(() -> {
									if (!subscribed.get()) {
										DataBufferUtils.release(buffer);
									}
								});
					})
					.doOnError(t -> getHeaders().clearContentHeaders())
					.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
		}
		else {
			return new ChannelSendOperator<>(body, inner -> doCommit(() -> writeWithInternal(inner)))
					.doOnError(t -> getHeaders().clearContentHeaders());
		}
	}

	@Override
	public final Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return new ChannelSendOperator<>(body, inner -> doCommit(() -> writeAndFlushWithInternal(inner)))
				.doOnError(t -> getHeaders().clearContentHeaders());
	}

	@Override
	public Mono<Void> setComplete() {
		return !isCommitted() ? doCommit(null) : Mono.empty();
	}

	/**
	 * A variant of {@link #doCommit(Supplier)} for a response without no body.
	 * @return a completion publisher
	 */
	protected Mono<Void> doCommit() {
		return doCommit(null);
	}

	/**
	 * Apply {@link #beforeCommit(Supplier) beforeCommit} actions, apply the
	 * response status and headers/cookies, and write the response body.
	 * @param writeAction the action to write the response body (may be {@code null})
	 * @return a completion publisher
	 */
	protected Mono<Void> doCommit(@Nullable Supplier<? extends Mono<Void>> writeAction) {
		Flux<Void> allActions = Flux.empty();
		if (this.state.compareAndSet(State.NEW, State.COMMITTING)) {
			if (!this.commitActions.isEmpty()) {
				allActions = Flux.concat(Flux.fromIterable(this.commitActions).map(Supplier::get))
						.doOnError(ex -> {
							if (this.state.compareAndSet(State.COMMITTING, State.COMMIT_ACTION_FAILED)) {
								getHeaders().clearContentHeaders();
							}
						});
			}
		}
		else if (this.state.compareAndSet(State.COMMIT_ACTION_FAILED, State.COMMITTING)) {
			// Skip commit actions
		}
		else {
			return Mono.empty();
		}

		allActions = allActions.concatWith(Mono.fromRunnable(() -> {
			applyStatusCode();
			applyHeaders();
			applyCookies();
			this.state.set(State.COMMITTED);
		}));

		if (writeAction != null) {
			allActions = allActions.concatWith(writeAction.get());
		}

		return allActions.then();
	}


	/**
	 * Write to the underlying the response.
	 * @param body the publisher to write with
	 */
	protected abstract Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body);

	/**
	 * Write to the underlying the response, and flush after each {@code Publisher<DataBuffer>}.
	 * @param body the publisher to write and flush with
	 */
	protected abstract Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body);

	/**
	 * Write the status code to the underlying response.
	 * This method is called once only.
	 */
	protected abstract void applyStatusCode();

	/**
	 * Invoked when the response is getting committed allowing sub-classes to
	 * make apply header values to the underlying response.
	 * <p>Note that most sub-classes use an {@link HttpHeaders} instance that
	 * wraps an adapter to the native response headers such that changes are
	 * propagated to the underlying response on the go. That means this callback
	 * is typically not used other than for specialized updates such as setting
	 * the contentType or characterEncoding fields in a Servlet response.
	 */
	protected abstract void applyHeaders();

	/**
	 * Add cookies from {@link #getHeaders()} to the underlying response.
	 * This method is called once only.
	 */
	protected abstract void applyCookies();

	/**
	 * Allow sub-classes to associate a hint with the data buffer if it is a
	 * pooled buffer and supports leak tracking.
	 * @param buffer the buffer to attach a hint to
	 * @since 5.3.2
	 */
	protected void touchDataBuffer(DataBuffer buffer) {
	}

}
