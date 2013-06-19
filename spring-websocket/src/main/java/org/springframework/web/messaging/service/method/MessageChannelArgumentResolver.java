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

package org.springframework.web.messaging.service.method;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.web.messaging.support.PubSubHeaderAccesssor;
import org.springframework.web.messaging.support.SessionMessageChannel;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("rawtypes")
public class MessageChannelArgumentResolver<M extends Message> implements ArgumentResolver<M> {

	private MessageChannel<M> messageBrokerChannel;


	public MessageChannelArgumentResolver(MessageChannel<M> messageBrokerChannel) {
		Assert.notNull(messageBrokerChannel, "messageBrokerChannel is required");
		this.messageBrokerChannel = messageBrokerChannel;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return MessageChannel.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, M message) throws Exception {
		Assert.notNull(this.messageBrokerChannel, "messageBrokerChannel is required");
		final String sessionId = PubSubHeaderAccesssor.wrap(message).getSessionId();
		return new SessionMessageChannel<M>(this.messageBrokerChannel, sessionId);
	}

}
