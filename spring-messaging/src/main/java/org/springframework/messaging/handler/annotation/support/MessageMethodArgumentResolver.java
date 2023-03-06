/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@code HandlerMethodArgumentResolver} for {@link Message} method arguments.
 * Validates that the generic type of the payload matches to the message value
 * or otherwise applies {@link MessageConverter} to convert to the expected
 * payload type.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MessageMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Nullable
	private final MessageConverter converter;


	/**
	 * Create a default resolver instance without message conversion.
	 */
	public MessageMethodArgumentResolver() {
		this(null);
	}

	/**
	 * Create a resolver instance with the given {@link MessageConverter}.
	 * @param converter the MessageConverter to use (may be {@code null})
	 * @since 4.3
	 */
	public MessageMethodArgumentResolver(@Nullable MessageConverter converter) {
		this.converter = converter;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Message.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		Class<?> targetMessageType = parameter.getParameterType();
		Class<?> targetPayloadType = getPayloadType(parameter, message);

		if (!targetMessageType.isAssignableFrom(message.getClass())) {
			throw new MethodArgumentTypeMismatchException(message, parameter, "Actual message type '" +
					ClassUtils.getDescriptiveType(message) + "' does not match expected type '" +
					ClassUtils.getQualifiedName(targetMessageType) + "'");
		}

		Object payload = message.getPayload();
		if (targetPayloadType.isInstance(payload)) {
			return message;
		}

		if (isEmptyPayload(payload)) {
			throw new MessageConversionException(message, "Cannot convert from actual payload type '" +
					ClassUtils.getDescriptiveType(payload) + "' to expected payload type '" +
					ClassUtils.getQualifiedName(targetPayloadType) + "' when payload is empty");
		}

		payload = convertPayload(message, parameter, targetPayloadType);
		return MessageBuilder.createMessage(payload, message.getHeaders());
	}

	/**
	 * Resolve the target class to convert the payload to.
	 * <p>By default this is the generic type declared in the {@code Message}
	 * method parameter but that can be overridden to select a more specific
	 * target type after also taking into account the "Content-Type", e.g.
	 * return {@code String} if target type is {@code Object} and
	 * {@code "Content-Type:text/**"}.
	 * @param parameter the target method parameter
	 * @param message the message being processed
	 * @return the target type to use
	 * @since 5.2
	 */
	protected Class<?> getPayloadType(MethodParameter parameter, Message<?> message) {
		Type genericParamType = parameter.getGenericParameterType();
		ResolvableType resolvableType = ResolvableType.forType(genericParamType).as(Message.class);
		return resolvableType.getGeneric().toClass();
	}

	/**
	 * Check if the given {@code payload} is empty.
	 * @param payload the payload to check (can be {@code null})
	 */
	protected boolean isEmptyPayload(@Nullable Object payload) {
		if (payload == null) {
			return true;
		}
		else if (payload instanceof byte[] bytes) {
			return bytes.length == 0;
		}
		else if (payload instanceof String text) {
			return !StringUtils.hasText(text);
		}
		else {
			return false;
		}
	}

	private Object convertPayload(Message<?> message, MethodParameter parameter, Class<?> targetPayloadType) {
		Object result = null;
		if (this.converter instanceof SmartMessageConverter smartConverter) {
			result = smartConverter.fromMessage(message, targetPayloadType, parameter);
		}
		else if (this.converter != null) {
			result = this.converter.fromMessage(message, targetPayloadType);
		}

		if (result == null) {
			throw new MessageConversionException(message, "No converter found from actual payload type '" +
					ClassUtils.getDescriptiveType(message.getPayload()) + "' to expected payload type '" +
					ClassUtils.getQualifiedName(targetPayloadType) + "'");
		}
		return result;
	}

}
