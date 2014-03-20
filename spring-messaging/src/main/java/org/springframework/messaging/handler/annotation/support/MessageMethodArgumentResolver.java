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

package org.springframework.messaging.handler.annotation.support;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * A {@link HandlerMethodArgumentResolver} for {@link Message} parameters. Validates
 * that the generic type of the payload matches with the message value.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @since 4.0
 */
public class MessageMethodArgumentResolver implements HandlerMethodArgumentResolver {


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Message.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		// Validate the message type is assignable
		if (!parameter.getParameterType().isAssignableFrom(message.getClass())) {
			throw new MethodArgumentTypeMismatchException(message,
					"Could not resolve Message parameter: invalid message type:"
							+ "expected [" + message.getClass().getName() + "] but got ["
							+ parameter.getParameterType().getName() + "]");
		}

		// validate that the payload type matches
		Class<?> effectivePayloadType = getPayloadType(parameter);
		if (effectivePayloadType != null && !effectivePayloadType.isInstance(message.getPayload())) {
			throw new MethodArgumentTypeMismatchException(message,
					"Could not resolve Message parameter: invalid payload type: "
							+ "expected [" + effectivePayloadType.getName() + "] but got ["
							+ message.getPayload().getClass().getName() + "]");
		}
		return message;
	}

	private Class<?> getPayloadType(MethodParameter parameter) {
		ResolvableType resolvableType = ResolvableType
				.forType(parameter.getGenericParameterType()).as(Message.class);
		return resolvableType.getGeneric(0).resolve(Object.class);
	}

}
