/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;

/**
 * Provides partial implementations of {@link SockJsSession} methods to send messages,
 * including heartbeat messages and to manage session state.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractServerSockJsSession extends AbstractSockJsSession {

	private final SockJsConfiguration sockJsConfig;

	private ScheduledFuture<?> heartbeatTask;


	public AbstractServerSockJsSession(String sessionId, SockJsConfiguration config, WebSocketHandler handler) {
		super(sessionId, handler);
		this.sockJsConfig = config;
	}

	protected SockJsConfiguration getSockJsConfig() {
		return this.sockJsConfig;
	}

	@Override
	public final synchronized void sendMessage(WebSocketMessage message) throws IOException {
		Assert.isTrue(!isClosed(), "Cannot send a message when session is closed");
		Assert.isInstanceOf(TextMessage.class, message, "Expected text message: " + message);
		sendMessageInternal(((TextMessage) message).getPayload());
	}

	protected abstract void sendMessageInternal(String message) throws IOException;


	@Override
	public void connectionClosedInternal(CloseStatus status) {
		updateLastActiveTime();
		cancelHeartbeat();
	}

	@Override
	public final synchronized void closeInternal(CloseStatus status) throws IOException {
		if (isActive()) {
			// TODO: deliver messages "in flight" before sending close frame
			try {
				// bypass writeFrame
				writeFrameInternal(SockJsFrame.closeFrame(status.getCode(), status.getReason()));
			}
			catch (Throwable ex) {
				logger.warn("Failed to send SockJS close frame: " + ex.getMessage());
			}
		}
		updateLastActiveTime();
		cancelHeartbeat();
		disconnect(status);
	}

	protected abstract void disconnect(CloseStatus status) throws IOException;

	/**
	 * For internal use within a TransportHandler and the (TransportHandler-specific)
	 * session sub-class.
	 */
	protected void writeFrame(SockJsFrame frame) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Preparing to write " + frame);
		}
		try {
			writeFrameInternal(frame);
		}
		catch (IOException ex) {
			if (ex instanceof EOFException || ex instanceof SocketException) {
				logger.warn("Client went away. Terminating connection");
			}
			else {
				logger.warn("Terminating connection due to failure to send message: " + ex.getMessage());
			}
			disconnect(CloseStatus.SERVER_ERROR);
			close(CloseStatus.SERVER_ERROR);
			throw ex;
		}
		catch (Throwable ex) {
			logger.warn("Terminating connection due to failure to send message: " + ex.getMessage());
			disconnect(CloseStatus.SERVER_ERROR);
			close(CloseStatus.SERVER_ERROR);
			throw new SockJsRuntimeException("Failed to write " + frame, ex);
		}
	}

	protected abstract void writeFrameInternal(SockJsFrame frame) throws Exception;

	public synchronized void sendHeartbeat() throws Exception {
		if (isActive()) {
			writeFrame(SockJsFrame.heartbeatFrame());
			scheduleHeartbeat();
		}
	}

	protected void scheduleHeartbeat() {
		Assert.notNull(getSockJsConfig().getTaskScheduler(), "heartbeatScheduler not configured");
		cancelHeartbeat();
		if (!isActive()) {
			return;
		}
		Date time = new Date(System.currentTimeMillis() + getSockJsConfig().getHeartbeatTime());
		this.heartbeatTask = getSockJsConfig().getTaskScheduler().schedule(new Runnable() {
			@Override
			public void run() {
				try {
					sendHeartbeat();
				}
				catch (Throwable t) {
					// ignore
				}
			}
		}, time);
		if (logger.isTraceEnabled()) {
			logger.trace("Scheduled heartbeat after " + getSockJsConfig().getHeartbeatTime()/1000 + " seconds");
		}
	}

	protected void cancelHeartbeat() {
		if ((this.heartbeatTask != null) && !this.heartbeatTask.isDone()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Cancelling heartbeat");
			}
			this.heartbeatTask.cancel(false);
		}
		this.heartbeatTask = null;
	}


}
