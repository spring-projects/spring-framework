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

package org.springframework.web.messaging.stomp.service.support;

import java.io.IOException;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.web.messaging.MessageBroker;
import org.springframework.web.messaging.converter.ContentTypeNotSupportedException;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.stomp.StompHeaders;
import org.springframework.web.messaging.stomp.StompMessage;

import reactor.core.Reactor;
import reactor.fn.Event;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageBrokerArgumentResolver extends AbstractPayloadSendingArgumentResolver {


	public MessageBrokerArgumentResolver(Reactor reactor, List<MessageConverter<?>> converters) {
		super(reactor, converters);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return MessageBroker.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, StompMessage message, Object replyTo) throws Exception {
		return new DefaultMessageBroker(message.getSessionId());
	}


	private class DefaultMessageBroker implements MessageBroker {

		private final String sessionId;


		public DefaultMessageBroker(String sessionId) {
			this.sessionId = sessionId;
		}

		@Override
		public void send(String destination, Object content, MediaType contentType)
				throws IOException, ContentTypeNotSupportedException {

			StompHeaders headers = new StompHeaders();
			headers.setDestination(destination);
			headers.setContentType(contentType);

			byte[] payload = convertToPayload(content, contentType);
			if (payload != null) {
				// TODO: set content-length
			}

			StompMessage message = new StompMessage(StompCommand.SEND, headers, payload);
			message.setSessionId(this.sessionId);

			getReactor().notify(StompCommand.SEND, Event.wrap(message));
		}
	}

}
