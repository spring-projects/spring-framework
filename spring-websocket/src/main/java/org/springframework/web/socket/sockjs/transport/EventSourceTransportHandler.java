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
import java.nio.charset.Charset;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsConfiguration;
import org.springframework.web.socket.sockjs.SockJsFrame.DefaultFrameFormat;
import org.springframework.web.socket.sockjs.SockJsFrame.FrameFormat;
import org.springframework.web.socket.sockjs.TransportType;

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
		return new MediaType("text", "event-stream", Charset.forName("UTF-8"));
	}

	@Override
	public StreamingSockJsSession createSession(String sessionId, WebSocketHandler handler) {
		return new EventSourceStreamingSockJsSession(sessionId, getSockJsConfig(), handler);
	}

	@Override
	protected FrameFormat getFrameFormat(ServerHttpRequest request) {
		return new DefaultFrameFormat("data: %s\r\n\r\n");
	}


	private final class EventSourceStreamingSockJsSession extends StreamingSockJsSession {

		private EventSourceStreamingSockJsSession(String sessionId, SockJsConfiguration config, WebSocketHandler handler) {
			super(sessionId, config, handler);
		}

		@Override
		protected void writePrelude() throws IOException {
			getResponse().getBody().write('\r');
			getResponse().getBody().write('\n');
			getResponse().flush();
		}
	}

}
