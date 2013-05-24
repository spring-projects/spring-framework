/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.stomp.adapter;

import java.io.IOException;

import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.stomp.StompCommand;
import org.springframework.web.stomp.StompMessage;
import org.springframework.web.stomp.StompSession;
import org.springframework.web.stomp.support.StompMessageConverter;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketStompSession implements StompSession {

	private final String id;

	private WebSocketSession webSocketSession;

	private final StompMessageConverter messageConverter;


	public WebSocketStompSession(WebSocketSession webSocketSession, StompMessageConverter messageConverter) {
		Assert.notNull(webSocketSession, "webSocketSession is required");
		this.id = webSocketSession.getId();
		this.webSocketSession = webSocketSession;
		this.messageConverter = messageConverter;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public void sendMessage(StompMessage message) throws IOException {

		Assert.notNull(this.webSocketSession, "Cannot send message without active session");

		try {
			byte[] bytes = this.messageConverter.fromStompMessage(message);
			this.webSocketSession.sendMessage(new TextMessage(new String(bytes, StompMessage.CHARSET)));
		}
		finally {
			if (StompCommand.ERROR.equals(message.getCommand())) {
				this.webSocketSession.close(CloseStatus.PROTOCOL_ERROR);
				this.webSocketSession = null;
			}
		}
	}

}