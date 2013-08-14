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
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame.FrameFormat;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.session.AbstractHttpSockJsSession;
import org.springframework.web.socket.sockjs.transport.session.PollingSockJsSession;
import org.springframework.web.util.JavaScriptUtils;

/**
 * A TransportHandler that sends messages via JSONP polling.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class JsonpPollingTransportHandler extends AbstractHttpSendingTransportHandler {

	@Override
	public TransportType getTransportType() {
		return TransportType.JSONP;
	}

	@Override
	protected MediaType getContentType() {
		return new MediaType("application", "javascript", Charset.forName("UTF-8"));
	}

	@Override
	public PollingSockJsSession createSession(String sessionId, WebSocketHandler wsHandler,
			Map<String, Object> attributes) {

		return new PollingSockJsSession(sessionId, getSockJsServiceConfig(), wsHandler, attributes);
	}

	@Override
	public void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpSockJsSession sockJsSession) throws SockJsException {

		try {
			String callback = getCallbackParam(request);
			if (! StringUtils.hasText(callback)) {
				response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
				response.getBody().write("\"callback\" parameter required".getBytes("UTF-8"));
				return;
			}
		}
		catch (Throwable t) {
			sockJsSession.tryCloseWithSockJsTransportError(t, CloseStatus.SERVER_ERROR);
			throw new SockJsTransportFailureException("Failed to send error", sockJsSession.getId(), t);
		}

		super.handleRequestInternal(request, response, sockJsSession);
	}

	@Override
	protected FrameFormat getFrameFormat(ServerHttpRequest request) {

		// we already validated the parameter above..
		String callback = getCallbackParam(request);

		return new SockJsFrame.DefaultFrameFormat(callback + "(\"%s\");\r\n") {
			@Override
			protected String preProcessContent(String content) {
				return JavaScriptUtils.javaScriptEscape(content);
			}
		};
	}

}
