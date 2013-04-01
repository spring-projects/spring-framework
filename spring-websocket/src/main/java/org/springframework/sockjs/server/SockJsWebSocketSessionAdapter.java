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

package org.springframework.sockjs.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.sockjs.SockJsHandler;
import org.springframework.websocket.WebSocketSession;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsWebSocketSessionAdapter extends AbstractServerSession {

	private static Log logger = LogFactory.getLog(SockJsWebSocketSessionAdapter.class);

	private WebSocketSession webSocketSession;


	public SockJsWebSocketSessionAdapter(String sessionId, SockJsHandler delegate, SockJsConfiguration sockJsConfig) {
		super(sessionId, delegate, sockJsConfig);
	}

	public void setWebSocketSession(WebSocketSession webSocketSession) throws Exception {
		this.webSocketSession = webSocketSession;
		scheduleHeartbeat();
		connectionInitialized();
	}

	@Override
	public boolean isActive() {
		return (this.webSocketSession != null);
	}

	@Override
	public void sendMessageInternal(String message) {
		cancelHeartbeat();
		writeFrame(SockJsFrame.messageFrame(message));
		scheduleHeartbeat();
	}

	@Override
	protected void writeFrameInternal(SockJsFrame frame) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Write " + frame);
		}
		this.webSocketSession.sendText(frame.getContent());
	}

	@Override
	public void closeInternal() {
		this.webSocketSession.close();
		this.webSocketSession = null;
		updateLastActiveTime();
	}

	@Override
	protected void deactivate() {
		this.webSocketSession.close();
	}

}
