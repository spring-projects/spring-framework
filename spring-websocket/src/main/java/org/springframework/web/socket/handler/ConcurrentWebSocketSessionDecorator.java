/*
 * Copyright 2002-2018 the original author or authors.
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
 * Wrap a {@link org.springframework.web.socket.WebSocketSession WebSocketSession}
 * to guarantee only one thread can send messages at a time.
 *
 * <p>If a send is slow, subsequent attempts to send more messages from other threads
 * will not be able to acquire the flush lock and messages will be buffered instead.
 * At that time, the specified buffer-size limit and send-time limit will be checked
 * and the session will be closed if the limits are exceeded.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0.3
 */
public class ConcurrentWebSocketSessionDecorator extends WebSocketSessionDecorator {

	private static final Log logger = LogFactory.getLog(ConcurrentWebSocketSessionDecorator.class);


	private final int sendTimeLimit;

	private final int bufferSizeLimit;

	private final Queue<WebSocketMessage<?>> buffer = new LinkedBlockingQueue<>();

	private final AtomicInteger bufferSize = new AtomicInteger();

	private volatile long sendStartTime;

	private volatile boolean limitExceeded;

	private volatile boolean closeInProgress;

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


	/**
	 * Return the configured send-time limit (milliseconds).
	 * @since 4.3.13
	 */
	public int getSendTimeLimit() {
		return this.sendTimeLimit;
	}

	/**
	 * Return the configured buffer-size limit (number of bytes).
	 * @since 4.3.13
	 */
	public int getBufferSizeLimit() {
		return this.bufferSizeLimit;
	}

	/**
	 * Return the current buffer size (number of bytes).
	 */
	public int getBufferSize() {
		return this.bufferSize.get();
	}

	/**
	 * Return the time (milliseconds) since the current send started,
	 * or 0 if no send is currently in progress.
	 */
	public long getTimeSinceSendStarted() {
		long start = this.sendStartTime;
		return (start > 0 ? (System.currentTimeMillis() - start) : 0);
	}


	@Override
	public void sendMessage(WebSocketMessage<?> message) throws IOException {
		if (shouldNotSend()) {
			return;
		}

		this.buffer.add(message);
		this.bufferSize.addAndGet(message.getPayloadLength());

		do {
			if (!tryFlushMessageBuffer()) {
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Another send already in progress: " +
							"session id '%s':, \"in-progress\" send time %d (ms), buffer size %d bytes",
							getId(), getTimeSinceSendStarted(), getBufferSize()));
				}
				checkSessionLimits();
				break;
			}
		}
		while (!this.buffer.isEmpty() && !shouldNotSend());
	}

	private boolean shouldNotSend() {
		return (this.limitExceeded || this.closeInProgress);
	}

	private boolean tryFlushMessageBuffer() throws IOException {
		if (this.flushLock.tryLock()) {
			try {
				while (true) {
					WebSocketMessage<?> message = this.buffer.poll();
					if (message == null || shouldNotSend()) {
						break;
					}
					this.bufferSize.addAndGet(message.getPayloadLength() * -1);
					this.sendStartTime = System.currentTimeMillis();
					getDelegate().sendMessage(message);
					this.sendStartTime = 0;
				}
			}
			finally {
				this.sendStartTime = 0;
				this.flushLock.unlock();
			}
			return true;
		}
		return false;
	}

	private void checkSessionLimits() {
		if (!shouldNotSend() && this.closeLock.tryLock()) {
			try {
				if (getTimeSinceSendStarted() > getSendTimeLimit()) {
					String format = "Message send time %d (ms) for session '%s' exceeded the allowed limit %d";
					String reason = String.format(format, getTimeSinceSendStarted(), getId(), getSendTimeLimit());
					limitExceeded(reason);
				}
				else if (getBufferSize() > getBufferSizeLimit()) {
					String format = "The send buffer size %d bytes for session '%s' exceeded the allowed limit %d";
					String reason = String.format(format, getBufferSize(), getId(), getBufferSizeLimit());
					limitExceeded(reason);
				}
			}
			finally {
				this.closeLock.unlock();
			}
		}
	}

	private void limitExceeded(String reason) {
		this.limitExceeded = true;
		throw new SessionLimitExceededException(reason, CloseStatus.SESSION_NOT_RELIABLE);
	}

	@Override
	public void close(CloseStatus status) throws IOException {
		this.closeLock.lock();
		try {
			if (this.closeInProgress) {
				return;
			}
			if (!CloseStatus.SESSION_NOT_RELIABLE.equals(status)) {
				try {
					checkSessionLimits();
				}
				catch (SessionLimitExceededException ex) {
					// Ignore
				}
				if (this.limitExceeded) {
					if (logger.isDebugEnabled()) {
						logger.debug("Changing close status " + status + " to SESSION_NOT_RELIABLE.");
					}
					status = CloseStatus.SESSION_NOT_RELIABLE;
				}
			}
			this.closeInProgress = true;
			super.close(status);
		}
		finally {
			this.closeLock.unlock();
		}
	}


	@Override
	public String toString() {
		return getDelegate().toString();
	}

}
