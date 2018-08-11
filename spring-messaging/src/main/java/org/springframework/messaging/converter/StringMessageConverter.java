/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.converter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * A {@link MessageConverter} that supports MIME type "text/plain" with the
 * payload converted to and from a String.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StringMessageConverter extends AbstractMessageConverter {

	private final Charset defaultCharset;


	public StringMessageConverter() {
		this(StandardCharsets.UTF_8);
	}

	public StringMessageConverter(Charset defaultCharset) {
		super(new MimeType("text", "plain", defaultCharset));
		Assert.notNull(defaultCharset, "Default Charset must not be null");
		this.defaultCharset = defaultCharset;
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return (String.class == clazz);
	}

	@Override
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		Charset charset = getContentTypeCharset(getMimeType(message.getHeaders()));
		Object payload = message.getPayload();
		return (payload instanceof String ? payload : new String((byte[]) payload, charset));
	}

	@Override
	@Nullable
	protected Object convertToInternal(
			Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint) {

		if (byte[].class == getSerializedPayloadClass()) {
			Charset charset = getContentTypeCharset(getMimeType(headers));
			payload = ((String) payload).getBytes(charset);
		}
		return payload;
	}

	private Charset getContentTypeCharset(@Nullable MimeType mimeType) {
		if (mimeType != null && mimeType.getCharset() != null) {
			return mimeType.getCharset();
		}
		else {
			return this.defaultCharset;
		}
	}

}
