/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;

/**
 * Common base class for plain JSON converters, e.g. Gson and JSON-B.
 *
 * @author Juergen Hoeller
 * @since 5.3
 * @see GsonMessageConverter
 * @see JsonbMessageConverter
 * @see #fromJson(Reader, Type)
 * @see #fromJson(String, Type)
 * @see #toJson(Object, Type)
 * @see #toJson(Object, Type, Writer)
 */
public abstract class AbstractJsonMessageConverter extends AbstractMessageConverter {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


	protected AbstractJsonMessageConverter() {
		super(new MimeType("application", "json"));
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return true;
	}

	@Override
	@Nullable
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		try {
			Type resolvedType = getResolvedType(targetClass, conversionHint);
			Object payload = message.getPayload();
			if (ClassUtils.isAssignableValue(targetClass, payload)) {
				return payload;
			}
			else if (payload instanceof byte[]) {
				return fromJson(getReader((byte[]) payload, message.getHeaders()), resolvedType);
			}
			else {
				// Assuming a text-based source payload
				return fromJson(payload.toString(), resolvedType);
			}
		}
		catch (Exception ex) {
			throw new MessageConversionException(message, "Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	@Nullable
	protected Object convertToInternal(Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint) {
		try {
			Type resolvedType = getResolvedType(payload.getClass(), conversionHint);
			if (byte[].class == getSerializedPayloadClass()) {
				ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
				Writer writer = getWriter(out, headers);
				toJson(payload, resolvedType, writer);
				writer.flush();
				return out.toByteArray();
			}
			else {
				// Assuming a text-based target payload
				return toJson(payload, resolvedType);
			}
		}
		catch (Exception ex) {
			throw new MessageConversionException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}


	private Reader getReader(byte[] payload, @Nullable MessageHeaders headers) {
		InputStream in = new ByteArrayInputStream(payload);
		return new InputStreamReader(in, getCharsetToUse(headers));
	}

	private Writer getWriter(ByteArrayOutputStream out, @Nullable MessageHeaders headers) {
		return new OutputStreamWriter(out, getCharsetToUse(headers));
	}

	private Charset getCharsetToUse(@Nullable MessageHeaders headers) {
		MimeType mimeType = getMimeType(headers);
		return (mimeType != null && mimeType.getCharset() != null ? mimeType.getCharset() : DEFAULT_CHARSET);
	}


	protected abstract Object fromJson(Reader reader, Type resolvedType);

	protected abstract Object fromJson(String payload, Type resolvedType);

	protected abstract void toJson(Object payload, Type resolvedType, Writer writer);

	protected abstract String toJson(Object payload, Type resolvedType);

}
