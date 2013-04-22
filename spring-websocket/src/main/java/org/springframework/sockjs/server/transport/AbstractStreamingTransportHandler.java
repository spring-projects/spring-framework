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

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.websocket.WebSocketHandler;


/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractStreamingTransportHandler extends AbstractHttpSendingTransportHandler {


	@Override
	public StreamingServerSockJsSession createSession(String sessionId, WebSocketHandler webSocketHandler) {
		Assert.notNull(getSockJsConfig(), "This transport requires SockJsConfiguration");
		return new StreamingServerSockJsSession(sessionId, getSockJsConfig(), webSocketHandler);
	}

	@Override
	public void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpServerSockJsSession session) throws Exception {

		writePrelude(request, response);
		super.handleRequestInternal(request, response, session);
	}

	protected abstract void writePrelude(ServerHttpRequest request, ServerHttpResponse response)
			throws IOException;

	@Override
	protected void handleNewSession(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpServerSockJsSession session) throws IOException, Exception {

		super.handleNewSession(request, response, session);

		session.setCurrentRequest(request, response, getFrameFormat(request));
	}

}