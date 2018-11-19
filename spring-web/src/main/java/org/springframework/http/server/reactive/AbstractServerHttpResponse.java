/*
 * Copyright 2002-2018 the original author or authors.
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
	private enum State {NEW, COMMITTING, COMMITTED}


	private final Log logger = LogFactory.getLog(getClass());

	private final DataBufferFactory dataBufferFactory;

	@Nullable
	private Integer statusCode;

	private final HttpHeaders headers;

	private final MultiValueMap<String, ResponseCookie> cookies;

	private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

	private final List<Supplier<? extends Mono<Void>>> commitActions = new ArrayList<>(4);


	public AbstractServerHttpResponse(DataBufferFactory dataBufferFactory) {
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		this.dataBufferFactory = dataBufferFactory;
		this.headers = new HttpHeaders();
		this.cookies = new LinkedMultiValueMap<>();
	}


	@Override
	public final DataBufferFactory bufferFactory() {
		return this.dataBufferFactory;
	}

	@Override
	public boolean setStatusCode(@Nullable HttpStatus statusCode) {
		if (this.state.get() == State.COMMITTED) {
			if (logger.isTraceEnabled()) {
				logger.trace("HTTP response already committed. " +
						"Status not set to " + (statusCode != null ? statusCode.toString() : "null"));
			}
			return false;
		}
		else {
			this.statusCode = (statusCode != null ? statusCode.value() : null);
			return true;
		}
	}

	@Override
	@Nullable
	public HttpStatus getStatusCode() {
		return (this.statusCode != null ? HttpStatus.resolve(this.statusCode) : null);
	}

	/**
	 * Set the HTTP status code of the response.
	 * @param statusCode the HTTP status as an integer value
	 * @since 5.0.1
	 */
	public void setStatusCodeValue(@Nullable Integer statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Return the HTTP status code of the response.
	 * @return the HTTP status as an integer value
	 * @since 5.0.1
	 */
	@Nullable
	public Integer getStatusCodeValue() {
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
		return this.state.get() != State.NEW;
	}

	@Override
	public final Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return new ChannelSendOperator<>(body,
				writePublisher -> doCommit(() -> writeWithInternal(writePublisher)))
				.doOnError(t -> removeContentLength());
	}

	@Override
	public final Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return new ChannelSendOperator<>(body,
				writePublisher -> doCommit(() -> writeAndFlushWithInternal(writePublisher)))
				.doOnError(t -> removeContentLength());
	}

	private void removeContentLength() {
		if (!this.isCommitted()) {
			this.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
		}
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
		if (!this.state.compareAndSet(State.NEW, State.COMMITTING)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping doCommit (response already committed).");
			}
			return Mono.empty();
		}

		this.commitActions.add(() ->
				Mono.fromRunnable(() -> {
					applyStatusCode();
					applyHeaders();
					applyCookies();
					this.state.set(State.COMMITTED);
				}));

		if (writeAction != null) {
			this.commitActions.add(writeAction);
		}

		List<? extends Mono<Void>> actions = this.commitActions.stream()
				.map(Supplier::get).collect(Collectors.toList());

		return Flux.concat(actions).then();
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
	 * Apply header changes from {@link #getHeaders()} to the underlying response.
	 * This method is called once only.
	 */
	protected abstract void applyHeaders();

	/**
	 * Add cookies from {@link #getHeaders()} to the underlying response.
	 * This method is called once only.
	 */
	protected abstract void applyCookies();

}
