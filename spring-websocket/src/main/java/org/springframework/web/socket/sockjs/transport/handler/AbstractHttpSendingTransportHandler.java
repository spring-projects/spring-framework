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

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.SockJsProcessingException;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame.FrameFormat;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.session.AbstractHttpSockJsSession;

/**
 * Base class for HTTP-based transports that send messages over HTTP.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHttpSendingTransportHandler extends TransportHandlerSupport
		implements TransportHandler, SockJsSessionFactory {


	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler webSocketHandler, WebSocketSession session) throws SockJsProcessingException {

		// Set content type before writing
		response.getHeaders().setContentType(getContentType());

		AbstractHttpSockJsSession sockJsSession = (AbstractHttpSockJsSession) session;
		handleRequestInternal(request, response, sockJsSession);
	}

	protected void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpSockJsSession sockJsSession) throws SockJsProcessingException {

		if (sockJsSession.isNew()) {
			logger.debug("Opening " + getTransportType() + " connection");
			sockJsSession.setInitialRequest(request, response, getFrameFormat(request));
		}
		else if (!sockJsSession.isActive()) {
			logger.debug("starting " + getTransportType() + " async request");
			sockJsSession.setLongPollingRequest(request, response, getFrameFormat(request));
		}
		else {
			try {
				logger.debug("another " + getTransportType() + " connection still open: " + sockJsSession);
				SockJsFrame closeFrame = SockJsFrame.closeFrameAnotherConnectionOpen();
				response.getBody().write(getFrameFormat(request).format(closeFrame).getContentBytes());
			}
			catch (IOException e) {
				throw new SockJsProcessingException("Failed to send SockJS close frame", e, sockJsSession.getId());
			}
		}
	}

	protected abstract MediaType getContentType();

	protected abstract FrameFormat getFrameFormat(ServerHttpRequest request);

}
