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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.web.messaging.event.EventBus;

import reactor.util.Assert;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageReturnValueHandler implements ReturnValueHandler {

	private static Log logger = LogFactory.getLog(MessageReturnValueHandler.class);

	private final EventBus eventBus;


	public MessageReturnValueHandler(EventBus eventBus) {
		this.eventBus = eventBus;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> paramType = returnType.getParameterType();
		return Message.class.isAssignableFrom(paramType);

//		if (Message.class.isAssignableFrom(paramType)) {
//			return true;
//		}
//		else if (List.class.isAssignableFrom(paramType)) {
//			Type type = returnType.getGenericParameterType();
//			if (type instanceof ParameterizedType) {
//				Type genericType = ((ParameterizedType) type).getActualTypeArguments()[0];
//			}
//		}
//		return Message.class.isAssignableFrom(paramType);
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		Message<?> returnMessage = (Message<?>) returnValue;
		if (returnMessage == null) {
			return;
		}

		String replyTo = (String) message.getHeaders().getReplyChannel();
		Assert.notNull(replyTo, "Cannot reply to: " + message);

		if (logger.isTraceEnabled()) {
			logger.trace("Sending notification: " + message);
		}
		this.eventBus.send(replyTo, returnMessage);
 	}

}
