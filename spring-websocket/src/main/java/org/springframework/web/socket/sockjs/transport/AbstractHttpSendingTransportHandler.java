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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.AbstractSockJsSession;
import org.springframework.web.socket.sockjs.ConfigurableTransportHandler;
import org.springframework.web.socket.sockjs.SockJsConfiguration;
import org.springframework.web.socket.sockjs.SockJsFrame;
import org.springframework.web.socket.sockjs.SockJsFrame.FrameFormat;
import org.springframework.web.socket.sockjs.SockJsSessionFactory;
import org.springframework.web.socket.sockjs.TransportErrorException;

/**
 * Base class for HTTP-based transports that send messages over HTTP.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHttpSendingTransportHandler
		implements ConfigurableTransportHandler, SockJsSessionFactory {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private SockJsConfiguration sockJsConfig;


	@Override
	public void setSockJsConfiguration(SockJsConfiguration sockJsConfig) {
		this.sockJsConfig = sockJsConfig;
	}

	public SockJsConfiguration getSockJsConfig() {
		return this.sockJsConfig;
	}

	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler webSocketHandler, AbstractSockJsSession session) throws TransportErrorException {

		// Set content type before writing
		response.getHeaders().setContentType(getContentType());

		AbstractHttpSockJsSession httpServerSession = (AbstractHttpSockJsSession) session;
		handleRequestInternal(request, response, httpServerSession);
	}

	protected void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpSockJsSession httpServerSession) throws TransportErrorException {

		if (httpServerSession.isNew()) {
			logger.debug("Opening " + getTransportType() + " connection");
			httpServerSession.setInitialRequest(request, response, getFrameFormat(request));
		}
		else if (!httpServerSession.isActive()) {
			logger.debug("starting " + getTransportType() + " async request");
			httpServerSession.setLongPollingRequest(request, response, getFrameFormat(request));
		}
		else {
			try {
				logger.debug("another " + getTransportType() + " connection still open: " + httpServerSession);
				SockJsFrame closeFrame = SockJsFrame.closeFrameAnotherConnectionOpen();
				response.getBody().write(getFrameFormat(request).format(closeFrame).getContentBytes());
			}
			catch (IOException e) {
				throw new TransportErrorException("Failed to send SockJS close frame", e, httpServerSession.getId());
			}
		}
	}

	protected abstract MediaType getContentType();

	protected abstract FrameFormat getFrameFormat(ServerHttpRequest request);

}
