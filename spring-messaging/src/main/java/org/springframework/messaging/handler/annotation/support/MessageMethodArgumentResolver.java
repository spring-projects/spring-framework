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

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Type;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.ClassUtils;

/**
 * {@code HandlerMethodArgumentResolver} for {@link Message} method arguments.
 * Validates that the generic type of the payload matches to the message value.
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
		Class<?> paramType = parameter.getParameterType();
		if (!paramType.isAssignableFrom(message.getClass())) {
			String actual = ClassUtils.getQualifiedName(message.getClass());
			String expected = ClassUtils.getQualifiedName(paramType);
			throw new MethodArgumentTypeMismatchException(message, parameter, "The actual message type " +
					"[" + actual + "] does not match the expected type [" + expected + "]");
		}

		Class<?> targetPayloadType = getPayloadType(parameter);
		Object payload = message.getPayload();
		if (payload != null && !targetPayloadType.isInstance(payload)) {
			throw new MethodArgumentTypeMismatchException(message, parameter,
					"The expected Message<?> payload type [" + ClassUtils.getQualifiedName(targetPayloadType) +
					"] does not match the actual payload type [" + ClassUtils.getQualifiedName(payload.getClass()) + "]");
		}

		return message;
	}

	private Class<?> getPayloadType(MethodParameter parameter) {
		Type genericParamType = parameter.getGenericParameterType();
		ResolvableType resolvableType = ResolvableType.forType(genericParamType).as(Message.class);
		return resolvableType.getGeneric(0).resolve(Object.class);
	}

}
