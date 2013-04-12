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

package org.springframework.sockjs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;


/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class SockJsSessionSupport implements SockJsSession {

	protected Log logger = LogFactory.getLog(this.getClass());

	private final String sessionId;

	private final SockJsHandler sockJsHandler;

	private State state = State.NEW;

	private long timeCreated = System.currentTimeMillis();

	private long timeLastActive = System.currentTimeMillis();


	/**
	 *
	 * @param sessionId
	 * @param sockJsHandler the recipient of SockJS messages
	 */
	public SockJsSessionSupport(String sessionId, SockJsHandler sockJsHandler) {
		Assert.notNull(sessionId, "sessionId is required");
		Assert.notNull(sockJsHandler, "sockJsHandler is required");
		this.sessionId = sessionId;
		this.sockJsHandler = sockJsHandler;
	}

	public String getId() {
		return this.sessionId;
	}

	public boolean isNew() {
		return State.NEW.equals(this.state);
	}

	public boolean isOpen() {
		return State.OPEN.equals(this.state);
	}

	public boolean isClosed() {
		return State.CLOSED.equals(this.state);
	}

	/**
	 * Polling and Streaming sessions periodically close the current HTTP request and
	 * wait for the next request to come through. During this "downtime" the session is
	 * still open but inactive and unable to send messages and therefore has to buffer
	 * them temporarily. A WebSocket session by contrast is stateful and remain active
	 * until closed.
	 */
	public abstract boolean isActive();

	/**
	 * Return the time since the session was last active, or otherwise if the
	 * session is new, the time since the session was created.
	 */
	public long getTimeSinceLastActive() {
		if (isNew()) {
			return (System.currentTimeMillis() - this.timeCreated);
		}
		else {
			return isActive() ? 0 : System.currentTimeMillis() - this.timeLastActive;
		}
	}

	/**
	 * Should be invoked whenever the session becomes inactive.
	 */
	protected void updateLastActiveTime() {
		this.timeLastActive = System.currentTimeMillis();
	}

	public void connectionInitialized() throws Exception {
		this.state = State.OPEN;
		this.sockJsHandler.newSession(this);
	}

	public void delegateMessages(String... messages) throws Exception {
		for (String message : messages) {
			this.sockJsHandler.handleMessage(this, message);
		}
	}

	public void delegateException(Throwable ex) {
		this.sockJsHandler.handleException(this, ex);
	}

	public void close() {
		this.state = State.CLOSED;
		this.sockJsHandler.sessionClosed(this);
	}

	public String toString() {
		return getClass().getSimpleName() + " [id=" + sessionId + "]";
	}


	private enum State { NEW, OPEN, CLOSED }

}
