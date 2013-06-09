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

package org.springframework.web.messaging.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.util.ClassUtils;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class CompositeMessageConverter implements MessageConverter {

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", CompositeMessageConverter.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", CompositeMessageConverter.class.getClassLoader());

	private final List<MessageConverter> converters;


	public CompositeMessageConverter(List<MessageConverter> converters) {
		if (converters == null) {
			this.converters = new ArrayList<MessageConverter>();
			this.converters.add(new ByteArrayMessageConverter());
			this.converters.add(new StringMessageConverter());
			if (jackson2Present) {
				this.converters.add(new MappingJackson2MessageConverter());
			}
		}
		else {
			this.converters = converters;
		}
	}

	@Override
	public boolean canConvertFromPayload(Class<?> clazz, MediaType contentType) {
		for (MessageConverter converter : this.converters) {
			if (converter.canConvertFromPayload(clazz, contentType)) {
				return true;

			}
		}
		return false;
	}

	@Override
	public Object convertFromPayload(Class<?> clazz, MediaType contentType, byte[] payload)
			throws IOException, ContentTypeNotSupportedException {

		for (MessageConverter converter : this.converters) {
			if (converter.canConvertFromPayload(clazz, contentType)) {
				return converter.convertFromPayload(clazz, contentType, payload);
			}
		}
		throw new ContentTypeNotSupportedException(contentType, clazz);
	}

	@Override
	public boolean canConvertToPayload(Class<?> clazz, MediaType mediaType) {
		for (MessageConverter converter : this.converters) {
			if (converter.canConvertToPayload(clazz, mediaType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public byte[] convertToPayload(Object content, MediaType contentType)
			throws IOException, ContentTypeNotSupportedException {

		for (MessageConverter converter : this.converters) {
			if (converter.canConvertToPayload(content.getClass(), contentType)) {
				return converter.convertToPayload(content, contentType);
			}
		}
		throw new ContentTypeNotSupportedException(contentType, content.getClass());
	}

}
