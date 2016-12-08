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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.websocket.CloseReason;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;

import org.reactivestreams.Publisher;
import org.springframework.http.server.reactive.AbstractRequestBodyPublisher;
import org.springframework.http.server.reactive.AbstractResponseBodyProcessor;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring {@link WebSocketSession} adapter for Tomcat's
 * {@link javax.websocket.Session}.
 * 
 * @author Violeta Georgieva
 * @since 5.0
 */
public class TomcatWebSocketSession extends WebSocketSessionSupport<Session> {

	private final AtomicBoolean sendCalled = new AtomicBoolean();

	private final WebSocketMessagePublisher webSocketMessagePublisher =
			new WebSocketMessagePublisher();

	private final String id;

	private final URI uri;

	private volatile WebSocketMessageProcessor webSocketMessageProcessor;

	public TomcatWebSocketSession(Session session) {
		super(session);
		this.id = session.getId();
		this.uri = session.getRequestURI();
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

	@Override
	protected Mono<Void> closeInternal(CloseStatus status) {
		try {
			getDelegate().close(new CloseReason(CloseCodes.getCloseCode(status.getCode()), status.getReason()));
		}
		catch (IOException e) {
			return Mono.error(e);
		}
		return Mono.empty();
	}

	boolean canWebSocketMessagePublisherAccept() {
		return this.webSocketMessagePublisher.canAccept();
	}

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
	void handleClose(CloseReason reason) {
		this.webSocketMessagePublisher.onAllDataRead();
		if (this.webSocketMessageProcessor != null) {
			this.webSocketMessageProcessor.cancel();
			this.webSocketMessageProcessor.onComplete();
		}
	}

	private static final class WebSocketMessagePublisher extends AbstractRequestBodyPublisher<WebSocketMessage> {
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
				return result;
			}

			return null;
		}

		void processWebSocketMessage(WebSocketMessage webSocketMessage) {
			this.webSocketMessage = webSocketMessage;
			onDataAvailable();
		}

		boolean canAccept() {
			return this.webSocketMessage == null;
		}
	}

	private final class WebSocketMessageProcessor extends AbstractResponseBodyProcessor<WebSocketMessage> {
		private volatile boolean isReady = true;

		@Override
		protected boolean write(WebSocketMessage message) throws IOException {
			if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
				this.isReady = false;
				getDelegate().getAsyncRemote().sendText(
						new String(message.getPayload().asByteBuffer().array(), StandardCharsets.UTF_8),
						new WebSocketMessageSendHandler());
			}
			else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
				this.isReady = false;
				getDelegate().getAsyncRemote().sendBinary(message.getPayload().asByteBuffer(),
						new WebSocketMessageSendHandler());
			}
			else if (WebSocketMessage.Type.PING.equals(message.getType())) {
				getDelegate().getAsyncRemote().sendPing(message.getPayload().asByteBuffer());
			}
			else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
				getDelegate().getAsyncRemote().sendPong(message.getPayload().asByteBuffer());
			}
			else {
				throw new IllegalArgumentException("Unexpected message type: " + message.getType());
			}
			return true;
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

		private final class WebSocketMessageSendHandler implements SendHandler {

			@Override
			public void onResult(SendResult result) {
				if (result.isOK()) {
					isReady = true;
					webSocketMessageProcessor.onWritePossible();
				}
				else {
					webSocketMessageProcessor.cancel();
					webSocketMessageProcessor.onError(result.getException());
				}
			}

		}

	}

}
