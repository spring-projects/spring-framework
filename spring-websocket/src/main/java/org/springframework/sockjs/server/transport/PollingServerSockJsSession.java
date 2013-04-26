/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.sockjs.server.transport;

import java.io.IOException;

import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.SockJsFrame;
import org.springframework.websocket.HandlerProvider;
import org.springframework.websocket.WebSocketHandler;


public class PollingServerSockJsSession extends AbstractHttpServerSockJsSession {

	public PollingServerSockJsSession(String sessionId, SockJsConfiguration sockJsConfig,
			HandlerProvider<WebSocketHandler<?>> handler) {

		super(sessionId, sockJsConfig, handler);
	}

	@Override
	protected void flushCache() throws IOException {
		cancelHeartbeat();
		String[] messages = getMessageCache().toArray(new String[getMessageCache().size()]);
		getMessageCache().clear();
		writeFrame(SockJsFrame.messageFrame(messages));
	}

	@Override
	protected void writeFrame(SockJsFrame frame) throws IOException {
		super.writeFrame(frame);
		resetRequest();
	}

}

