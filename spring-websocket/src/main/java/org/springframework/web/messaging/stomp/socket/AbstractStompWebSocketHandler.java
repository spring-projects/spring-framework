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
package org.springframework.web.messaging.stomp.socket;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.stomp.StompHeaders;
import org.springframework.web.messaging.stomp.support.StompMessageConverter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.TextWebSocketHandlerAdapter;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractStompWebSocketHandler extends TextWebSocketHandlerAdapter {

	private final StompMessageConverter stompMessageConverter = new StompMessageConverter();

	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<String, WebSocketSession>();


	public StompMessageConverter getStompMessageConverter() {
		return this.stompMessageConverter;
	}

	protected WebSocketSession getWebSocketSession(String sessionId) {
		return this.sessions.get(sessionId);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		this.sessions.put(session.getId(), session);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
		try {
			String payload = textMessage.getPayload();
			Message<byte[]> message = this.stompMessageConverter.toMessage(payload, session.getId());

			// TODO: validate size limits
			// http://stomp.github.io/stomp-specification-1.2.html#Size_Limits

			handleStompMessage(session, message);

			// TODO: send RECEIPT message if incoming message has "receipt" header
			// http://stomp.github.io/stomp-specification-1.2.html#Header_receipt

		}
		catch (Throwable error) {
			sendErrorMessage(session, error);
		}
	}

	protected void sendErrorMessage(WebSocketSession session, Throwable error) {

		StompHeaders stompHeaders = StompHeaders.create(StompCommand.ERROR);
		stompHeaders.setMessage(error.getMessage());

		Message<byte[]> errorMessage = new GenericMessage<byte[]>(new byte[0], stompHeaders.toMessageHeaders());
		byte[] bytes = this.stompMessageConverter.fromMessage(errorMessage);

		try {
			session.sendMessage(new TextMessage(new String(bytes, Charset.forName("UTF-8"))));
		}
		catch (Throwable t) {
			// ignore
		}
	}

	protected abstract void handleStompMessage(WebSocketSession session, Message<byte[]> message);

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		this.sessions.remove(session.getId());
	}

}
