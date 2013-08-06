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

package org.springframework.web.socket.sockjs.transport.handler;

import java.nio.charset.Charset;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame.DefaultFrameFormat;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame.FrameFormat;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.session.PollingSockJsSession;

/**
 * A {@link TransportHandler} based on XHR (long) polling.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class XhrPollingTransportHandler extends AbstractHttpSendingTransportHandler {

	@Override
	public TransportType getTransportType() {
		return TransportType.XHR;
	}

	@Override
	protected MediaType getContentType() {
		return new MediaType("application", "javascript", Charset.forName("UTF-8"));
	}

	@Override
	protected FrameFormat getFrameFormat(ServerHttpRequest request) {
		return new DefaultFrameFormat("%s\n");
	}

	@Override
	public PollingSockJsSession createSession(String sessionId, WebSocketHandler handler) {
		return new PollingSockJsSession(sessionId, getSockJsServiceConfig(), handler);
	}

}
