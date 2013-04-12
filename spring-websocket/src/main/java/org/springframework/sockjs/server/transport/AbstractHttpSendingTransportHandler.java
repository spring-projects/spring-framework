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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.sockjs.SockJsSessionFactory;
import org.springframework.sockjs.SockJsSessionSupport;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.SockJsFrame;
import org.springframework.sockjs.server.SockJsFrame.FrameFormat;
import org.springframework.sockjs.server.TransportHandler;

/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHttpSendingTransportHandler
		implements TransportHandler, SockJsSessionFactory<SockJsSessionSupport> {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final SockJsConfiguration sockJsConfig;


	public AbstractHttpSendingTransportHandler(SockJsConfiguration sockJsConfig) {
		this.sockJsConfig = sockJsConfig;
	}

	protected SockJsConfiguration getSockJsConfig() {
		return this.sockJsConfig;
	}

	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsSessionSupport session) throws Exception {

		// Set content type before writing
		response.getHeaders().setContentType(getContentType());

		AbstractHttpServerSession httpServerSession = (AbstractHttpServerSession) session;
		handleRequestInternal(request, response, httpServerSession);
	}

	protected void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpServerSession httpServerSession) throws Exception, IOException {

		if (httpServerSession.isNew()) {
			handleNewSession(request, response, httpServerSession);
		}
		else if (httpServerSession.isActive()) {
			logger.debug("another " + getTransportType() + " connection still open: " + httpServerSession);
			httpServerSession.writeFrame(response, SockJsFrame.closeFrameAnotherConnectionOpen());
		}
		else {
			logger.debug("starting " + getTransportType() + " async request");
			httpServerSession.setCurrentRequest(request, response, getFrameFormat(request));
		}
	}

	protected void handleNewSession(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpServerSession session) throws Exception {

		logger.debug("Opening " + getTransportType() + " connection");
		session.setFrameFormat(getFrameFormat(request));
		session.writeFrame(response, SockJsFrame.openFrame());
		session.connectionInitialized();
	}

	protected abstract MediaType getContentType();

	protected abstract FrameFormat getFrameFormat(ServerHttpRequest request);

}