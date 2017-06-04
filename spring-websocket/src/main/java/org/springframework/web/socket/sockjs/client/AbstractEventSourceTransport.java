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

package org.springframework.web.socket.sockjs.client;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * Abstract base class for EventSource transport implementations to extend.
 *
 * @author Rossen Stoyanchev
 * @author Sebastian Lövdahl
 * @since 5.0
 */
public abstract class AbstractEventSourceTransport extends AbstractHttpTransport implements EventSourceTransport {


	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public List<TransportType> getTransportTypes() {
		return Collections.singletonList(TransportType.EVENT_SOURCE);
	}

	@Override
	public ListenableFuture<WebSocketSession> connect(TransportRequest request, WebSocketHandler handler) {
		SettableListenableFuture<WebSocketSession> connectFuture = new SettableListenableFuture<>();
		EventSourceClientSockJsSession session =
				new EventSourceClientSockJsSession(request, handler, this, connectFuture);
		request.addTimeoutTask(session.getTimeoutTask());

		URI receiveUrl = request.getTransportUrl();
		if (logger.isDebugEnabled()) {
			logger.debug("Starting EventSource session url=" + receiveUrl);
		}

		HttpHeaders handshakeHeaders = new HttpHeaders();
		handshakeHeaders.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
		handshakeHeaders.putAll(request.getHandshakeHeaders());

		connectInternal(request, handler, receiveUrl, handshakeHeaders, session, connectFuture);
		return connectFuture;
	}

	protected abstract void connectInternal(TransportRequest request,
			WebSocketHandler handler, URI receiveUrl, HttpHeaders handshakeHeaders,
			EventSourceClientSockJsSession session,
			SettableListenableFuture<WebSocketSession> connectFuture);


	protected void handleEventSourceFrame(ByteArrayOutputStream outputStream, EventSourceClientSockJsSession session) {
		byte[] bytes = outputStream.toByteArray();
		outputStream.reset();
		if (bytes.length > 0) {
			String content = new String(bytes, SockJsFrame.CHARSET);
			if (logger.isDebugEnabled()) {
				logger.debug("EventSource content received: " + content);
			}

			String eventData = "";
			String[] splittedContent = content.split("\n");
			for (String line : splittedContent) {
				if (line.startsWith("data:")) {
					// strip the field name and the colon
					int dataBeginIndex = content.indexOf(':') + 1;
					if (line.length() > dataBeginIndex) {
						String value = line.substring(dataBeginIndex);
						// if present, strip one single space immediately after the colon
						if (value.charAt(0) == ' ') {
							value = value.substring(1);
						}
						if (!value.isEmpty()) {
							eventData += (eventData.isEmpty() ? value : "\n" + value);
						}
					}
				}
			}

			if (!eventData.isEmpty()) {
				session.handleFrame(eventData);
			}
		}
	}


	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
