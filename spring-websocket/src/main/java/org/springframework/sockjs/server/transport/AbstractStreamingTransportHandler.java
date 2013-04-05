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
import org.springframework.sockjs.SockJsHandler;
import org.springframework.sockjs.server.SockJsConfiguration;


/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractStreamingTransportHandler extends AbstractHttpSendingTransportHandler {


	@Override
	public StreamingHttpServerSession createSession(String sessionId, SockJsHandler handler, SockJsConfiguration config) {
		return new StreamingHttpServerSession(sessionId, handler, config);
	}

	@Override
	public void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpServerSession session) throws Exception {

		writePrelude(request, response);
		super.handleRequestInternal(request, response, session);
	}

	protected abstract void writePrelude(ServerHttpRequest request, ServerHttpResponse response)
			throws IOException;

	@Override
	protected void handleNewSession(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpServerSession session) throws IOException, Exception {

		super.handleNewSession(request, response, session);

		session.setCurrentRequest(request, response, getFrameFormat(request));
	}

}