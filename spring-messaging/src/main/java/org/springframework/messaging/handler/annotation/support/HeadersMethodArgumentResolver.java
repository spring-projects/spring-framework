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

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Resolves the following method parameters:
 * <ul>
 * <li>Parameters assignable to {@link Map} annotated with {@link Header @Headers}
 * <li>Parameters of type {@link MessageHeaders}
 * <li>Parameters assignable to {@link MessageHeaderAccessor}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HeadersMethodArgumentResolver implements HandlerMethodArgumentResolver {


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return ((parameter.hasParameterAnnotation(Headers.class) && Map.class.isAssignableFrom(paramType)) ||
				MessageHeaders.class.equals(paramType) ||
				MessageHeaderAccessor.class.isAssignableFrom(paramType));
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {

		Class<?> paramType = parameter.getParameterType();

		if (Map.class.isAssignableFrom(paramType)) {
			return message.getHeaders();
		}
		else if (MessageHeaderAccessor.class.equals(paramType)) {
			return new MessageHeaderAccessor(message);
		}
		else if (MessageHeaderAccessor.class.isAssignableFrom(paramType)) {
			Method factoryMethod = ClassUtils.getMethod(paramType, "wrap", Message.class);
			return ReflectionUtils.invokeMethod(factoryMethod, null, message);
		}
		else {
			throw new IllegalStateException("Unexpected method parameter type "
					+ paramType + "in method " + parameter.getMethod() + ". "
					+ "@Headers method arguments must be assignable to java.util.Map.");
		}
	}

}
