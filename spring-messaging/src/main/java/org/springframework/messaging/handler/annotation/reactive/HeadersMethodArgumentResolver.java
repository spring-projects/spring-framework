/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.annotation.reactive;

import java.lang.reflect.Method;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.invocation.reactive.SyncHandlerMethodArgumentResolver;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.ReflectionUtils;

/**
 * Argument resolver for headers. Resolves the following method parameters:
 * <ul>
 * <li>{@link Headers @Headers} {@link Map}
 * <li>{@link MessageHeaders}
 * <li>{@link MessageHeaderAccessor}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class HeadersMethodArgumentResolver implements SyncHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return ((parameter.hasParameterAnnotation(Headers.class) && Map.class.isAssignableFrom(paramType)) ||
				MessageHeaders.class == paramType || MessageHeaderAccessor.class.isAssignableFrom(paramType));
	}

	@Override
	public @Nullable Object resolveArgumentValue(MethodParameter parameter, Message<?> message) {
		Class<?> paramType = parameter.getParameterType();
		if (Map.class.isAssignableFrom(paramType)) {
			return message.getHeaders();
		}
		else if (MessageHeaderAccessor.class == paramType) {
			MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
			return accessor != null ? accessor : new MessageHeaderAccessor(message);
		}
		else if (MessageHeaderAccessor.class.isAssignableFrom(paramType)) {
			MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
			if (accessor != null && paramType.isInstance(accessor)) {
				return accessor;
			}
			else {
				Method method = ReflectionUtils.findMethod(paramType, "wrap", Message.class);
				if (method == null) {
					throw new IllegalStateException(
							"Cannot create accessor of type " + paramType + " for message " + message);
				}
				return ReflectionUtils.invokeMethod(method, null, message);
			}
		}
		else {
			throw new IllegalStateException("Unexpected parameter of type " + paramType +
					" in method " + parameter.getMethod() + ". ");
		}
	}

}
