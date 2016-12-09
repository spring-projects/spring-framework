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

package org.springframework.web.reactive.socket.adapter;

import java.io.IOException;
import java.net.URI;

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;
import org.springframework.http.server.reactive.AbstractListenerReadPublisher;
import org.springframework.http.server.reactive.AbstractListenerWriteProcessor;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Base class for Listener-based {@link WebSocketSession} adapters.
 *
 * @author Violeta Georgieva
 * @since 5.0
 */
public abstract class AbstractListenerWebSocketSessionSupport<T> extends WebSocketSessionSupport<T> {

	private final AtomicBoolean sendCalled = new AtomicBoolean();

	private final String id;

	private final URI uri;

	protected final WebSocketMessagePublisher webSocketMessagePublisher =
			new WebSocketMessagePublisher();

	protected volatile WebSocketMessageProcessor webSocketMessageProcessor;

	public AbstractListenerWebSocketSessionSupport(T delegate, String id, URI uri) {
		super(delegate);
		Assert.notNull(id, "'id' is required.");
		Assert.notNull(uri, "'uri' is required.");
		this.id = id;
		this.uri = uri;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public URI getUri() {
		return this.uri;
	}

	@Override
	public Flux<WebSocketMessage> receive() {
		return Flux.from(this.webSocketMessagePublisher);
	}

	@Override
	public Mono<Void> send(Publisher<WebSocketMessage> messages) {
		if (this.sendCalled.compareAndSet(false, true)) {
			this.webSocketMessageProcessor = new WebSocketMessageProcessor();
			return Mono.from(subscriber -> {
					messages.subscribe(this.webSocketMessageProcessor);
					this.webSocketMessageProcessor.subscribe(subscriber);
			});
		}
		else {
			return Mono.error(new IllegalStateException("send() has already been called"));
		}
	}

	protected void resumeReceives() {
		// no-op
	}

	protected void suspendReceives() {
		// no-op
	}

	protected abstract boolean writeInternal(WebSocketMessage message) throws IOException;

	/** Handle a message callback from the Servlet container */
	void handleMessage(Type type, WebSocketMessage message) {
		this.webSocketMessagePublisher.processWebSocketMessage(message);
	}

	/** Handle a error callback from the Servlet container */
	void handleError(Throwable ex) {
		this.webSocketMessagePublisher.onError(ex);
		if (this.webSocketMessageProcessor != null) {
			this.webSocketMessageProcessor.cancel();
			this.webSocketMessageProcessor.onError(ex);
		}
	}

	/** Handle a complete callback from the Servlet container */
	void handleClose(CloseStatus reason) {
		this.webSocketMessagePublisher.onAllDataRead();
		if (this.webSocketMessageProcessor != null) {
			this.webSocketMessageProcessor.cancel();
			this.webSocketMessageProcessor.onComplete();
		}
	}

	final class WebSocketMessagePublisher extends AbstractListenerReadPublisher<WebSocketMessage> {
		private volatile WebSocketMessage webSocketMessage;

		@Override
		protected void checkOnDataAvailable() {
			if (this.webSocketMessage != null) {
				onDataAvailable();
			}
		}

		@Override
		protected WebSocketMessage read() throws IOException {
			if (this.webSocketMessage != null) {
				WebSocketMessage result = this.webSocketMessage;
				this.webSocketMessage = null;
				resumeReceives();
				return result;
			}

			return null;
		}

		void processWebSocketMessage(WebSocketMessage webSocketMessage) {
			this.webSocketMessage = webSocketMessage;
			suspendReceives();
			onDataAvailable();
		}

		boolean canAccept() {
			return this.webSocketMessage == null;
		}
	}

	final class WebSocketMessageProcessor extends AbstractListenerWriteProcessor<WebSocketMessage> {
		private volatile boolean isReady = true;

		@Override
		protected boolean write(WebSocketMessage message) throws IOException {
			return writeInternal(message);
		}

		@Override
		protected void releaseData() {
			if (logger.isTraceEnabled()) {
				logger.trace("releaseBuffer: " + this.currentData);
			}
			this.currentData = null;
		}

		@Override
		protected boolean isDataEmpty(WebSocketMessage data) {
			return data.getPayload().readableByteCount() == 0;
		}

		@Override
		protected boolean isWritePossible() {
			if (this.isReady && this.currentData != null) {
				return true;
			}
			else {
				return false;
			}
		}

		void setReady(boolean ready) {
			this.isReady = ready;
		}

	}

}
