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
import org.springframework.messaging.handler.annotation.MessageBody;
import org.springframework.messaging.handler.method.HandlerMethodArgumentResolver;
import org.springframework.messaging.support.converter.MessageConverter;
import org.springframework.util.Assert;


/**
 * TODO
 *
 * <p>This {@link HandlerMethodArgumentResolver} should be ordered last as it supports all
 * types and does not require the {@link MessageBody} annotation.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageBodyMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter<?> converter;


	public MessageBodyMethodArgumentResolver(MessageConverter<?> converter) {
		Assert.notNull(converter, "converter is required");
		this.converter = converter;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return true;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {

		Object arg = null;

		MessageBody annot = parameter.getParameterAnnotation(MessageBody.class);

		if (annot == null || annot.required()) {
			Class<?> sourceClass = message.getPayload().getClass();
			Class<?> targetClass = parameter.getParameterType();
			if (targetClass.isAssignableFrom(sourceClass)) {
				return message.getPayload();
			}
			else {
				// TODO: use content-type header
				return this.converter.fromMessage(message, targetClass);
			}
		}

		return arg;
	}

}
