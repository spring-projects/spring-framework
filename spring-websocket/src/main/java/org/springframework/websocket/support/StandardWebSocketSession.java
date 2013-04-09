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

package org.springframework.websocket.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.websocket.WebSocketSession;


/**
 * A {@link WebSocketSession} that delegates to a {@link javax.websocket.Session}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketSession implements WebSocketSession {

	private static Log logger = LogFactory.getLog(StandardWebSocketSession.class);

	private javax.websocket.Session session;


	public StandardWebSocketSession(javax.websocket.Session session) {
		this.session = session;
	}

	@Override
	public boolean isOpen() {
		return ((this.session != null) && this.session.isOpen());
	}

	@Override
	public void sendText(String text) throws Exception {
		logger.trace("Sending text message: " + text);
		// TODO: check closed
		this.session.getBasicRemote().sendText(text);
	}

	@Override
	public void close() {
		// TODO: delegate with code and reason
		this.session = null;
	}

	@Override
	public void close(int code, String reason) {
		this.session = null;
	}

}
