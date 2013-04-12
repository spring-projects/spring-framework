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

package org.springframework.sockjs.server.transport;

import java.io.IOException;

import org.springframework.sockjs.SockJsHandler;
import org.springframework.sockjs.SockJsSessionSupport;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;


/**
 * A {@link WebSocketHandler} that merely delegates to a {@link SockJsHandler} without any
 * SockJS message framing. For use with raw WebSocket communication at SockJS path
 * "/websocket".
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketSockJsHandlerAdapter extends AbstractSockJsWebSocketHandler {


	public WebSocketSockJsHandlerAdapter(SockJsConfiguration sockJsConfig, SockJsHandler sockJsHandler) {
		super(sockJsConfig, sockJsHandler);
	}

	@Override
	protected SockJsSessionSupport createSockJsSession(WebSocketSession wsSession) throws Exception {
		return new WebSocketSessionAdapter(wsSession);
	}


	private class WebSocketSessionAdapter extends SockJsSessionSupport {

		private final WebSocketSession wsSession;


		public WebSocketSessionAdapter(WebSocketSession wsSession) throws Exception {
			super(String.valueOf(wsSession.hashCode()), getSockJsHandler());
			this.wsSession = wsSession;
			connectionInitialized();
		}

		@Override
		public boolean isActive() {
			return (!isClosed() && this.wsSession.isOpen());
		}

		@Override
		public void sendMessage(String message) throws IOException {
			this.wsSession.sendText(message);
		}

		public void close() {
			if (!isClosed()) {
				logger.debug("Closing session");
				super.close();
				this.wsSession.close();
			}
		}
	}

}
