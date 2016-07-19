/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.socket.sockjs.transport.handler;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.frame.DefaultSockJsFrameFormat;
import org.springframework.web.socket.sockjs.frame.SockJsFrameFormat;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.session.StreamingSockJsSession;

/**
 * A TransportHandler for sending messages via Server-Sent events:
 * <a href="http://dev.w3.org/html5/eventsource/">http://dev.w3.org/html5/eventsource/</a>.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class EventSourceTransportHandler extends AbstractHttpSendingTransportHandler {

	@Override
	public TransportType getTransportType() {
		return TransportType.EVENT_SOURCE;
	}

	@Override
	protected MediaType getContentType() {
		return new MediaType("text", "event-stream", StandardCharsets.UTF_8);
	}

	@Override
	public StreamingSockJsSession createSession(
			String sessionId, WebSocketHandler handler, Map<String, Object> attributes) {

		return new EventSourceStreamingSockJsSession(sessionId, getServiceConfig(), handler, attributes);
	}

	@Override
	protected SockJsFrameFormat getFrameFormat(ServerHttpRequest request) {
		return new DefaultSockJsFrameFormat("data: %s\r\n\r\n");
	}


	private class EventSourceStreamingSockJsSession extends StreamingSockJsSession {

		public EventSourceStreamingSockJsSession(String sessionId, SockJsServiceConfig config,
				WebSocketHandler wsHandler, Map<String, Object> attributes) {

			super(sessionId, config, wsHandler, attributes);
		}

		@Override
		protected byte[] getPrelude(ServerHttpRequest request) {
			return new byte[] { '\r', '\n' };
		}
	}

}
