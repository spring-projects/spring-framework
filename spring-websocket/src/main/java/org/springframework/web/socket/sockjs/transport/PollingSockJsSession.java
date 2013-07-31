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

package org.springframework.web.socket.sockjs.transport;

import java.io.IOException;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsConfiguration;
import org.springframework.web.socket.sockjs.SockJsFrame;
import org.springframework.web.socket.sockjs.SockJsMessageCodec;

/**
 * A SockJS session for use with polling HTTP transports.
 *
 * @author Rossen Stoyanchev
 */
public class PollingSockJsSession extends AbstractHttpSockJsSession {

	public PollingSockJsSession(String sessionId, SockJsConfiguration config, WebSocketHandler handler) {
		super(sessionId, config, handler);
	}


	@Override
	protected void flushCache() throws IOException {

		cancelHeartbeat();
		String[] messages = getMessageCache().toArray(new String[getMessageCache().size()]);
		getMessageCache().clear();

		SockJsMessageCodec messageCodec = getSockJsConfig().getMessageCodecRequired();
		SockJsFrame frame = SockJsFrame.messageFrame(messageCodec, messages);
		writeFrame(frame);
	}

	@Override
	protected void writeFrame(SockJsFrame frame) throws IOException {
		super.writeFrame(frame);
		resetRequest();
	}

}

