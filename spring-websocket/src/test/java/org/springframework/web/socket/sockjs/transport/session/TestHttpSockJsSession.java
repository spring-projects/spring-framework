/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;

/**
 * @author Rossen Stoyanchev
 */
public class TestHttpSockJsSession extends StreamingSockJsSession {

	private boolean active;

	private final List<SockJsFrame> sockJsFrames = new ArrayList<>();

	private CloseStatus closeStatus;

	private IOException exceptionOnWrite;

	private int numberOfLastActiveTimeUpdates;

	private boolean cancelledHeartbeat;

	private String subProtocol;


	public TestHttpSockJsSession(String sessionId, SockJsServiceConfig config,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		super(sessionId, config, wsHandler, attributes);
	}

	@Override
	protected byte[] getPrelude(ServerHttpRequest request) {
		return new byte[0];
	}

	@Override
	public String getAcceptedProtocol() {
		return this.subProtocol;
	}

	@Override
	public void setAcceptedProtocol(String protocol) {
		this.subProtocol = protocol;
	}

	public CloseStatus getCloseStatus() {
		return this.closeStatus;
	}

	@Override
	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public List<SockJsFrame> getSockJsFramesWritten() {
		return this.sockJsFrames;
	}

	public void setExceptionOnWrite(IOException exceptionOnWrite) {
		this.exceptionOnWrite = exceptionOnWrite;
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
	protected void writeFrameInternal(SockJsFrame frame) throws IOException {
		this.sockJsFrames.add(frame);
		if (this.exceptionOnWrite != null) {
			throw this.exceptionOnWrite;
		}
	}

	@Override
	protected void disconnect(CloseStatus status) {
		this.closeStatus = status;
	}

	@Override
	protected void flushCache() throws SockJsTransportFailureException {
	}

}
