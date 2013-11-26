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
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.method.HandlerMethodArgumentResolver;
import org.springframework.messaging.support.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * A resolver to extract and convert the payload of a message using a
 * {@link MessageConverter}.
 *
 * <p>This {@link HandlerMethodArgumentResolver} should be ordered last as it supports all
 * types and does not require the {@link Payload} annotation.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class PayloadArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;


	public PayloadArgumentResolver(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "converter must not be null");
		this.converter = messageConverter;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return true;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {

		Class<?> sourceClass = message.getPayload().getClass();
		Class<?> targetClass = parameter.getParameterType();

		if (ClassUtils.isAssignable(targetClass,sourceClass)) {
			return message.getPayload();
		}

		Payload annot = parameter.getParameterAnnotation(Payload.class);

		if (isEmptyPayload(message)) {
			if ((annot != null) && !annot.required()) {
				return null;
			}
		}

		if ((annot != null) && StringUtils.hasText(annot.value())) {
			throw new IllegalStateException("@Payload SpEL expressions not supported by this resolver.");
		}

		return this.converter.fromMessage(message, targetClass);
	}

	protected boolean isEmptyPayload(Message<?> message) {
		Object payload = message.getPayload();
		if (payload instanceof byte[]) {
			return ((byte[]) message.getPayload()).length == 0;
		}
		else if (payload instanceof String) {
			return ((String) payload).trim().equals("");
		}
		else {
			return false;
		}
	}

}
