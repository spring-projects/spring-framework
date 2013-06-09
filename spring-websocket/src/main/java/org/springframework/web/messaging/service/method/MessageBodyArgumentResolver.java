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

import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.web.messaging.annotation.MessageBody;
import org.springframework.web.messaging.converter.CompositeMessageConverter;
import org.springframework.web.messaging.converter.MessageConversionException;
import org.springframework.web.messaging.converter.MessageConverter;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageBodyArgumentResolver implements ArgumentResolver {

	private final MessageConverter converter;


	public MessageBodyArgumentResolver(List<MessageConverter> converters) {
		this.converter = new CompositeMessageConverter(converters);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return true;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {

		Object arg = null;

		MessageBody annot = parameter.getParameterAnnotation(MessageBody.class);
		MediaType contentType = (MediaType) message.getHeaders().get("content-type");

		if (annot == null || annot.required()) {
			Class<?> sourceType = message.getPayload().getClass();
			Class<?> parameterType = parameter.getParameterType();
			if (parameterType.isAssignableFrom(sourceType)) {
				return message.getPayload();
			}
			else if (byte[].class.equals(sourceType)) {
				return this.converter.convertFromPayload(parameterType, contentType, (byte[]) message.getPayload());
			}
			else {
				throw new MessageConversionException(message, "Unexpected payload type", null);
			}
		}

		return arg;
	}

}
