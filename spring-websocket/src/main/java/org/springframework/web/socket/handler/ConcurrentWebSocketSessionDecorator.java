/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Wraps a {@link org.springframework.web.socket.WebSocketSession} and guarantees
 * only one thread can send messages at a time.
 *
 * <p>If a send is slow, subsequent attempts to send more messages from a different
 * thread will fail to acquire the flushLock and the messages will be buffered instead --
 * at that time the specified buffer size limit and send time limit will be checked
 * and the session closed if the limits are exceeded.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
public class ConcurrentWebSocketSessionDecorator extends WebSocketSessionDecorator {

	private static Log logger = LogFactory.getLog(ConcurrentWebSocketSessionDecorator.class);


	private final Queue<WebSocketMessage<?>> buffer = new LinkedBlockingQueue<WebSocketMessage<?>>();

	private final AtomicInteger bufferSize = new AtomicInteger();

	private final int bufferSizeLimit;


	private volatile long sendStartTime;

	private final int sendTimeLimit;


	private volatile boolean sessionLimitExceeded;


	private final Lock flushLock = new ReentrantLock();

	private final Lock closeLock = new ReentrantLock();


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

		if (this.sessionLimitExceeded) {
			return;
		}

		this.buffer.add(message);
		this.bufferSize.addAndGet(message.getPayloadLength());

		do {
			if (!tryFlushMessageBuffer()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Another send already in progress, session id '" +
							getId() + "'" + ", in-progress send time " + getTimeSinceSendStarted() +
							" (ms)" + ", buffer size " + this.bufferSize + " bytes");
				}
				checkSessionLimits();
				break;
			}
		}
		while (!this.buffer.isEmpty());
	}

	private boolean tryFlushMessageBuffer() throws IOException {
		if (this.flushLock.tryLock() && !this.sessionLimitExceeded) {
			try {
				while (true) {
					WebSocketMessage<?> messageToSend = this.buffer.poll();
					if (messageToSend == null) {
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
		if (this.closeLock.tryLock() && !this.sessionLimitExceeded) {
			try {
				if (getTimeSinceSendStarted() > this.sendTimeLimit) {
					sessionLimitReached(
							"Message send time " + getTimeSinceSendStarted() +
									" (ms) exceeded the allowed limit " + this.sendTimeLimit);
				}
				else if (this.bufferSize.get() > this.bufferSizeLimit) {
					sessionLimitReached(
							"The send buffer size " + this.bufferSize.get() + " bytes for " +
							"session '" + getId() + " exceeded the allowed limit " + this.bufferSizeLimit);
				}
			}
			finally {
				this.closeLock.unlock();
			}
		}
	}

	private void sessionLimitReached(String reason) {
		this.sessionLimitExceeded = true;
		throw new SessionLimitExceededException(reason);
	}

}
