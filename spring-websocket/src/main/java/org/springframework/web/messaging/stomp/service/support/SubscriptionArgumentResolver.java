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
import org.springframework.util.Assert;
import org.springframework.web.messaging.Subscription;
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
public class SubscriptionArgumentResolver extends AbstractPayloadSendingArgumentResolver {


	public SubscriptionArgumentResolver(Reactor reactor, List<MessageConverter<?>> converters) {
		super(reactor, converters);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Subscription.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, StompMessage message, Object replyTo) throws Exception {

		Assert.isTrue(StompCommand.SUBSCRIBE.equals(message.getCommand()), "Not a subscribe command");

		return new DefaultSubscription(message.getHeaders().getDestination(), replyTo);
	}


	private class DefaultSubscription implements Subscription {

		private final String destination;

		private final Object replyTo;


		public DefaultSubscription(String destination, Object replyTo) {
			this.destination = destination;
			this.replyTo = replyTo;
		}

		@Override
		public void reply(Object content) throws IOException, ContentTypeNotSupportedException {
			reply(content, null);
		}

		@Override
		public void reply(Object content, MediaType contentType)
				throws IOException, ContentTypeNotSupportedException {

			StompHeaders headers = new StompHeaders();
			headers.setDestination(this.destination);
			headers.setContentType(contentType);

			byte[] payload = convertToPayload(content, contentType);
			if (payload != null) {
				// TODO: set content-length
			}

			StompMessage message = new StompMessage(StompCommand.MESSAGE, headers, payload);

			getReactor().notify(this.replyTo, Event.wrap(message));
		}
	}

}
