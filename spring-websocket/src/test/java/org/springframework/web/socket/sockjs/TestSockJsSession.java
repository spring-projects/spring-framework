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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;

/**
 * @author Rossen Stoyanchev
 */
public class TestSockJsSession extends AbstractSockJsSession {

	private boolean active;

	private final List<SockJsFrame> sockJsFramesWritten = new ArrayList<>();

	private CloseStatus status;

	private Exception exceptionOnWriteFrame;

	private int numberOfLastActiveTimeUpdates;

	private boolean cancelledHeartbeat;


	public TestSockJsSession(String sessionId, SockJsConfiguration config, WebSocketHandler handler) {
		super(sessionId, config, handler);
	}

	public CloseStatus getStatus() {
		return this.status;
	}

	@Override
	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public List<SockJsFrame> getSockJsFramesWritten() {
		return this.sockJsFramesWritten;
	}

	public void setExceptionOnWriteFrame(Exception exceptionOnWriteFrame) {
		this.exceptionOnWriteFrame = exceptionOnWriteFrame;
	}

	public int getNumberOfLastActiveTimeUpdates() {
		return this.numberOfLastActiveTimeUpdates;
	}

	public boolean didCancelHeartbeat() {
		return this.cancelledHeartbeat;
	}

	@Override
	protected void updateLastActiveTime() {
		this.numberOfLastActiveTimeUpdates++;
		super.updateLastActiveTime();
	}

	@Override
	protected void cancelHeartbeat() {
		this.cancelledHeartbeat = true;
		super.cancelHeartbeat();
	}

	@Override
	protected void sendMessageInternal(String message) throws IOException {
	}

	@Override
	protected void writeFrameInternal(SockJsFrame frame) throws Exception {
		this.sockJsFramesWritten.add(frame);
		if (this.exceptionOnWriteFrame != null) {
			throw this.exceptionOnWriteFrame;
		}
	}

	@Override
	protected void disconnect(CloseStatus status) throws IOException {
		this.status = status;
	}

}
