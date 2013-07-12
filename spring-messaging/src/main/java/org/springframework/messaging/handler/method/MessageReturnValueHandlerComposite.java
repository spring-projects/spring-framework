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

package org.springframework.messaging.handler.method;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageReturnValueHandlerComposite implements MessageReturnValueHandler {

	private final List<MessageReturnValueHandler> returnValueHandlers = new ArrayList<MessageReturnValueHandler>();


	/**
	 * Add the given {@link MessageReturnValueHandler}.
	 */
	public MessageReturnValueHandlerComposite addHandler(MessageReturnValueHandler returnValuehandler) {
		this.returnValueHandlers.add(returnValuehandler);
		return this;
	}

	/**
	 * Add the given {@link MessageReturnValueHandler}s.
	 */
	public MessageReturnValueHandlerComposite addHandlers(List<? extends MessageReturnValueHandler> handlers) {
		if (handlers != null) {
			for (MessageReturnValueHandler handler : handlers) {
				this.returnValueHandlers.add(handler);
			}
		}
		return this;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return getReturnValueHandler(returnType) != null;
	}

	private MessageReturnValueHandler getReturnValueHandler(MethodParameter returnType) {
		for (MessageReturnValueHandler handler : this.returnValueHandlers) {
			if (handler.supportsReturnType(returnType)) {
				return handler;
			}
		}
		return null;
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		MessageReturnValueHandler handler = getReturnValueHandler(returnType);
		Assert.notNull(handler, "Unknown return value type [" + returnType.getParameterType().getName() + "]");
		handler.handleReturnValue(returnValue, returnType, message);
	}

}
