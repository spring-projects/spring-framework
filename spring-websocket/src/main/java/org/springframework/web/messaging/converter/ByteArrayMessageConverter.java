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

import org.springframework.http.MediaType;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ByteArrayMessageConverter implements MessageConverter {

	@Override
	public boolean canConvertFromPayload(Class<?> clazz, MediaType contentType) {
		return byte[].class.equals(clazz);
	}

	@Override
	public Object convertFromPayload(Class<?> clazz, MediaType contentType, byte[] payload) {
		return payload;
	}

	@Override
	public boolean canConvertToPayload(Class<?> clazz, MediaType mediaType) {
		return byte[].class.equals(clazz);
	}

	@Override
	public byte[] convertToPayload(Object content, MediaType contentType) {
		return (byte[]) content;
	}

}
