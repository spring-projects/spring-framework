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

package org.springframework.web.messaging.stomp.service.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.web.messaging.MessageBody;
import org.springframework.web.messaging.converter.ContentTypeNotSupportedException;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.stomp.StompMessage;
import org.springframework.web.messaging.stomp.service.MessageMethodArgumentResolver;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageBodyArgumentResolver implements MessageMethodArgumentResolver {

	private final List<MessageConverter<?>> converters;


	public MessageBodyArgumentResolver(List<MessageConverter<?>> converters) {
		this.converters = (converters != null) ? converters : new ArrayList<MessageConverter<?>>();
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object resolveArgument(MethodParameter parameter, StompMessage message, Object replyTo)
			throws Exception {

		byte[] payload = message.getPayload();

		Class<?> parameterType = parameter.getParameterType();
		if (byte[].class.isAssignableFrom(parameterType)) {
			return payload;
		}

		Object arg = null;

		MessageBody annot = parameter.getParameterAnnotation(MessageBody.class);
		MediaType contentType = message.getHeaders().getContentType();

		if (annot == null || annot.required()) {
			for (MessageConverter converter : this.converters) {
				if (converter.canConvertFromPayload(parameterType, contentType)) {
					return converter.convertFromPayload(parameterType, contentType, payload);
				}
			}
			throw new ContentTypeNotSupportedException(contentType, parameterType);
		}

		return arg;
	}

}
