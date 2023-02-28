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

package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * A default implementation of {@link TransportRequest}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
class DefaultTransportRequest implements TransportRequest {

	private static final Log logger = LogFactory.getLog(DefaultTransportRequest.class);


	private final SockJsUrlInfo sockJsUrlInfo;

	private final HttpHeaders handshakeHeaders;

	private final HttpHeaders httpRequestHeaders;

	private final Transport transport;

	private final TransportType serverTransportType;

	private final SockJsMessageCodec codec;

	@Nullable
	private Principal user;

	private long timeoutValue;

	@Nullable
	private TaskScheduler timeoutScheduler;

	private final List<Runnable> timeoutTasks = new ArrayList<>();

	@Nullable
	private DefaultTransportRequest fallbackRequest;


	public DefaultTransportRequest(SockJsUrlInfo sockJsUrlInfo,
			@Nullable HttpHeaders handshakeHeaders, @Nullable HttpHeaders httpRequestHeaders,
			Transport transport, TransportType serverTransportType, SockJsMessageCodec codec) {

		Assert.notNull(sockJsUrlInfo, "SockJsUrlInfo is required");
		Assert.notNull(transport, "Transport is required");
		Assert.notNull(serverTransportType, "TransportType is required");
		Assert.notNull(codec, "SockJsMessageCodec is required");
		this.sockJsUrlInfo = sockJsUrlInfo;
		this.handshakeHeaders = (handshakeHeaders != null ? handshakeHeaders : new HttpHeaders());
		this.httpRequestHeaders = (httpRequestHeaders != null ? httpRequestHeaders : new HttpHeaders());
		this.transport = transport;
		this.serverTransportType = serverTransportType;
		this.codec = codec;
	}


	@Override
	public SockJsUrlInfo getSockJsUrlInfo() {
		return this.sockJsUrlInfo;
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		return this.handshakeHeaders;
	}

	@Override
	public HttpHeaders getHttpRequestHeaders() {
		return this.httpRequestHeaders;
	}

	@Override
	public URI getTransportUrl() {
		return this.sockJsUrlInfo.getTransportUrl(this.serverTransportType);
	}

	public void setUser(Principal user) {
		this.user = user;
	}

	@Override
	@Nullable
	public Principal getUser() {
		return this.user;
	}

	@Override
	public SockJsMessageCodec getMessageCodec() {
		return this.codec;
	}

	public void setTimeoutValue(long timeoutValue) {
		this.timeoutValue = timeoutValue;
	}

	public void setTimeoutScheduler(TaskScheduler scheduler) {
		this.timeoutScheduler = scheduler;
	}

	@Override
	public void addTimeoutTask(Runnable runnable) {
		this.timeoutTasks.add(runnable);
	}

	public void setFallbackRequest(DefaultTransportRequest fallbackRequest) {
		this.fallbackRequest = fallbackRequest;
	}


	@Deprecated
	public void connect(WebSocketHandler handler,
			org.springframework.util.concurrent.SettableListenableFuture<WebSocketSession> future) {

		if (logger.isTraceEnabled()) {
			logger.trace("Starting " + this);
		}
		ListenableConnectCallback connectCallback = new ListenableConnectCallback(handler, future);
		scheduleConnectTimeoutTask(connectCallback);
		this.transport.connect(this, handler).addCallback(connectCallback);
	}

	public void connect(WebSocketHandler handler, CompletableFuture<WebSocketSession> future) {
		if (logger.isTraceEnabled()) {
			logger.trace("Starting " + this);
		}
		CompletableConnectCallback connectCallback = new CompletableConnectCallback(handler, future);
		scheduleConnectTimeoutTask(connectCallback);
		this.transport.connectAsync(this, handler).whenComplete(connectCallback);
	}


	private void scheduleConnectTimeoutTask(ListenableConnectCallback connectHandler) {
		if (this.timeoutScheduler != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Scheduling connect to time out after " + this.timeoutValue + " ms.");
			}
			Instant timeoutDate = Instant.now().plus(this.timeoutValue, ChronoUnit.MILLIS);
			this.timeoutScheduler.schedule(connectHandler, timeoutDate);
		}
		else if (logger.isTraceEnabled()) {
			logger.trace("Connect timeout task not scheduled (no TaskScheduler configured).");
		}
	}

	private void scheduleConnectTimeoutTask(CompletableConnectCallback connectHandler) {
		if (this.timeoutScheduler != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Scheduling connect to time out after " + this.timeoutValue + " ms.");
			}
			Instant timeoutDate = Instant.now().plus(this.timeoutValue, ChronoUnit.MILLIS);
			this.timeoutScheduler.schedule(connectHandler, timeoutDate);
		}
		else if (logger.isTraceEnabled()) {
			logger.trace("Connect timeout task not scheduled (no TaskScheduler configured).");
		}
	}


	@Override
	public String toString() {
		return "TransportRequest[url=" + getTransportUrl() + "]";
	}


	/**
	 * Updates the given (global) future based success or failure to connect for
	 * the entire SockJS request regardless of which transport actually managed
	 * to connect. Also implements {@code Runnable} to handle a scheduled timeout
	 * callback.
	 */
	@SuppressWarnings("deprecation")
	private class ListenableConnectCallback implements
			org.springframework.util.concurrent.ListenableFutureCallback<WebSocketSession>, Runnable {

		private final WebSocketHandler handler;

		private final org.springframework.util.concurrent.SettableListenableFuture<WebSocketSession> future;

		private final AtomicBoolean handled = new AtomicBoolean();

		public ListenableConnectCallback(WebSocketHandler handler,
				org.springframework.util.concurrent.SettableListenableFuture<WebSocketSession> future) {
			this.handler = handler;
			this.future = future;
		}

		@Override
		public void onSuccess(@Nullable WebSocketSession session) {
			if (this.handled.compareAndSet(false, true)) {
				this.future.set(session);
			}
			else if (logger.isErrorEnabled()) {
				logger.error("Connect success/failure already handled for " + DefaultTransportRequest.this);
			}
		}

		@Override
		public void onFailure(Throwable ex) {
			handleFailure(ex, false);
		}

		@Override
		public void run() {
			handleFailure(null, true);
		}

		private void handleFailure(@Nullable Throwable ex, boolean isTimeoutFailure) {
			if (this.handled.compareAndSet(false, true)) {
				if (isTimeoutFailure) {
					String message = "Connect timed out for " + DefaultTransportRequest.this;
					logger.error(message);
					ex = new SockJsTransportFailureException(message, getSockJsUrlInfo().getSessionId(), ex);
				}
				if (fallbackRequest != null) {
					logger.error(DefaultTransportRequest.this + " failed. Falling back on next transport.", ex);
					fallbackRequest.connect(this.handler, this.future);
				}
				else {
					logger.error("No more fallback transports after " + DefaultTransportRequest.this, ex);
					if (ex != null) {
						this.future.setException(ex);
					}
				}
				if (isTimeoutFailure) {
					try {
						for (Runnable runnable : timeoutTasks) {
							runnable.run();
						}
					}
					catch (Throwable ex2) {
						logger.error("Transport failed to run timeout tasks for " + DefaultTransportRequest.this, ex2);
					}
				}
			}
			else {
				logger.error("Connect success/failure events already took place for " +
						DefaultTransportRequest.this + ". Ignoring this additional failure event.", ex);
			}
		}
	}

	/**
	 * Updates the given (global) future based success or failure to connect for
	 * the entire SockJS request regardless of which transport actually managed
	 * to connect. Also implements {@code Runnable} to handle a scheduled timeout
	 * callback.
	 */
	private class CompletableConnectCallback
			implements Runnable, BiConsumer<WebSocketSession, Throwable> {

		private final WebSocketHandler handler;

		private final CompletableFuture<WebSocketSession> future;

		private final AtomicBoolean handled = new AtomicBoolean();

		public CompletableConnectCallback(WebSocketHandler handler, CompletableFuture<WebSocketSession> future) {
			this.handler = handler;
			this.future = future;
		}

		@Override
		public void accept(@Nullable WebSocketSession session, @Nullable Throwable throwable) {
			if (session != null) {
				if (this.handled.compareAndSet(false, true)) {
					this.future.complete(session);
				}
				else if (logger.isErrorEnabled()) {
					logger.error("Connect success/failure already handled for " + DefaultTransportRequest.this);
				}
			}
			else if (throwable != null) {
				handleFailure(throwable, false);
			}
		}

		@Override
		public void run() {
			handleFailure(null, true);
		}

		private void handleFailure(@Nullable Throwable ex, boolean isTimeoutFailure) {
			if (this.handled.compareAndSet(false, true)) {
				if (isTimeoutFailure) {
					String message = "Connect timed out for " + DefaultTransportRequest.this;
					logger.error(message);
					ex = new SockJsTransportFailureException(message, getSockJsUrlInfo().getSessionId(), ex);
				}
				if (fallbackRequest != null) {
					logger.error(DefaultTransportRequest.this + " failed. Falling back on next transport.", ex);
					fallbackRequest.connect(this.handler, this.future);
				}
				else {
					logger.error("No more fallback transports after " + DefaultTransportRequest.this, ex);
					if (ex != null) {
						this.future.completeExceptionally(ex);
					}
				}
				if (isTimeoutFailure) {
					try {
						for (Runnable runnable : timeoutTasks) {
							runnable.run();
						}
					}
					catch (Throwable ex2) {
						logger.error("Transport failed to run timeout tasks for " + DefaultTransportRequest.this, ex2);
					}
				}
			}
			else {
				logger.error("Connect success/failure events already took place for " +
						DefaultTransportRequest.this + ". Ignoring this additional failure event.", ex);
			}
		}
	}

}
