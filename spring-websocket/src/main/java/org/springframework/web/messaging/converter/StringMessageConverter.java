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
import java.nio.charset.Charset;

import org.springframework.http.MediaType;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StringMessageConverter extends AbstractMessageConverter<String> {

	private static final Charset UTF_8 = Charset.forName("UTF-8");


	public StringMessageConverter() {
		super(MediaType.TEXT_PLAIN);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return String.class.equals(clazz);
	}

	@Override
	protected String convertFromPayloadInternal(Class<? extends String> clazz, MediaType contentType,
			byte[] payload) throws IOException {

		return new String(payload, UTF_8);
	}

	@Override
	protected byte[] convertToPayloadInternal(String content, MediaType contentType) throws IOException {
		return content.getBytes(UTF_8);
	}

}
