/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.handler;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Wraps a {@link org.springframework.web.socket.WebSocketSession} and guarantees
 * only one thread can send messages at a time.
 *
 * <p>If a send is slow, subsequent attempts to send more messages from a different
 * thread will fail to acquire the flush lock and the messages will be buffered
 * instead: At that time, the specified buffer-size limit and send-time limit will
 * be checked and the session closed if the limits are exceeded.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
public class ConcurrentWebSocketSessionDecorator extends WebSocketSessionDecorator {

	private static final Log logger = LogFactory.getLog(ConcurrentWebSocketSessionDecorator.class);


	private final Queue<WebSocketMessage<?>> buffer = new LinkedBlockingQueue<WebSocketMessage<?>>();

	private final AtomicInteger bufferSize = new AtomicInteger();

	private final int bufferSizeLimit;


	private volatile long sendStartTime;

	private final int sendTimeLimit;


	private volatile boolean limitExceeded;

	private volatile boolean shutdownInProgress;


	private final Lock flushLock = new ReentrantLock();

	private final Lock closeLock = new ReentrantLock();


	/**
	 * Create a new {@code ConcurrentWebSocketSessionDecorator}.
	 * @param delegate the {@code WebSocketSession} to delegate to
	 * @param sendTimeLimit the send-time limit (milliseconds)
	 * @param bufferSizeLimit the buffer-size limit (number of bytes)
	 */
	public ConcurrentWebSocketSessionDecorator(WebSocketSession delegate, int sendTimeLimit, int bufferSizeLimit) {
		super(delegate);
		this.sendTimeLimit = sendTimeLimit;
		this.bufferSizeLimit = bufferSizeLimit;
	}


	public int getBufferSize() {
		return this.bufferSize.get();
	}

	public long getTimeSinceSendStarted() {
		long start = this.sendStartTime;
		return (start > 0 ? (System.currentTimeMillis() - start) : 0);
	}


	public void sendMessage(WebSocketMessage<?> message) throws IOException {
		if (isDisabled()) {
			return;
		}

		this.buffer.add(message);
		this.bufferSize.addAndGet(message.getPayloadLength());

		do {
			if (!tryFlushMessageBuffer()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Another send already in progress, session id '" +
							getId() + "'" + ", in-progress send time " + getTimeSinceSendStarted() +
							" (ms)" + ", buffer size " + this.bufferSize + " bytes");
				}
				checkSessionLimits();
				break;
			}
		}
		while (!this.buffer.isEmpty() && !isDisabled());
	}

	private boolean isDisabled() {
		return (this.limitExceeded || this.shutdownInProgress);
	}

	private boolean tryFlushMessageBuffer() throws IOException {
		if (this.flushLock.tryLock()) {
			try {
				while (true) {
					WebSocketMessage<?> messageToSend = this.buffer.poll();
					if (messageToSend == null || isDisabled()) {
						break;
					}
					this.bufferSize.addAndGet(messageToSend.getPayloadLength() * -1);
					this.sendStartTime = System.currentTimeMillis();
					getDelegate().sendMessage(messageToSend);
					this.sendStartTime = 0;
				}
			}
			finally {
				this.sendStartTime = 0;
				flushLock.unlock();
			}
			return true;
		}
		return false;
	}

	private void checkSessionLimits() throws IOException {
		if (!isDisabled() && this.closeLock.tryLock()) {
			try {
				if (getTimeSinceSendStarted() > this.sendTimeLimit) {
					String errorMessage = "Message send time " + getTimeSinceSendStarted() +
							" (ms) exceeded the allowed limit " + this.sendTimeLimit;
					sessionLimitReached(errorMessage, CloseStatus.SESSION_NOT_RELIABLE);
				}
				else if (this.bufferSize.get() > this.bufferSizeLimit) {
					String errorMessage = "The send buffer size " + this.bufferSize.get() + " bytes for " +
							"session '" + getId() + " exceeded the allowed limit " + this.bufferSizeLimit;
					sessionLimitReached(errorMessage,
							(getTimeSinceSendStarted() >= 10000 ? CloseStatus.SESSION_NOT_RELIABLE : null));
				}
			}
			finally {
				this.closeLock.unlock();
			}
		}
	}

	private void sessionLimitReached(String reason, CloseStatus status) {
		this.limitExceeded = true;
		throw new SessionLimitExceededException(reason, status);
	}

	@Override
	public void close(CloseStatus status) throws IOException {
		this.shutdownInProgress = true;
		super.close(status);
	}


	@Override
	public String toString() {
		return getDelegate().toString();
	}

}
