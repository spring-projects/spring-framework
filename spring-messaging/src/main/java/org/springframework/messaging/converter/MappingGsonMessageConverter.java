/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.databind.JavaType;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * A GSON based {@link MessageConverter} implementation.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Marten Deinum
 * @since 5.1
 */
public class MappingGsonMessageConverter extends AbstractMessageConverter {

	/**
	 * The default encoding used for writing to text messages: UTF-8.
	 */
	private static final String DEFAULT_ENCODING = "UTF-8";

	private Gson gson;

	/**
	 * Construct a {@code MappingGsonMessageConverter} supporting
	 * the {@code application/json} MIME type with {@code UTF-8} character set.
	 */
	public MappingGsonMessageConverter() {
		super(new MimeType("application", "json", StandardCharsets.UTF_8));
		this.gson=new Gson();
	}

	/**
	 * Construct a {@code MappingJackson2MessageConverter} supporting
	 * one or more custom MIME types.
	 *
	 * @param supportedMimeTypes the supported MIME types
	 * @since 4.1.5
	 */
	public MappingGsonMessageConverter(MimeType... supportedMimeTypes) {
		super(Arrays.asList(supportedMimeTypes));
		this.gson=new Gson();
	}

	/**
	 * Set the {@code Gson} for this converter.
	 * If not set, a default {@link Gson Gson} instance will be created.
	 * <p>Setting a custom-configured {@code Gson} is one way to take further
	 * control of the JSON serialization process.
	 */
	public void setGson(Gson gson) {
		Assert.notNull(gson, "Gson must not be null");
		this.gson = gson;
	}

	/**
	 * Return the underlying {@code Gson} for this converter.
	 */
	public Gson getGson() {
		return this.gson;
	}

	@Override
	protected boolean canConvertFrom(Message<?> message, @Nullable Class<?> targetClass) {
		return targetClass != null && supportsMimeType(message.getHeaders());
	}

	@Override
	protected boolean canConvertTo(Object payload, @Nullable MessageHeaders headers) {
		return supportsMimeType(headers);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canConvertFrom/canConvertTo instead
		throw new UnsupportedOperationException();
	}

	@Override
	@Nullable
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		Object payload = message.getPayload();
		Charset encoding = getEncoding(getMimeType(message.getHeaders()));
		Type type = getSerializationType(conversionHint);

		if (payload instanceof byte[]) {
			try {
				if (type != null) {
					return this.gson.fromJson(new String((byte[]) payload, encoding), type);
				} else {
					return this.gson.fromJson(new String((byte[]) payload, encoding), targetClass);
				}
			}
			catch (JsonParseException ex) {
				throw new MessageConversionException(message, "Could not read JSON: " + ex.getMessage(), ex);
			}
		}
		else {
			if (type != null) {
				return this.gson.fromJson(payload.toString(), targetClass);
			}
			else {
				return this.gson.fromJson(payload.toString(), type);
			}
		}
	}

	@Override
	@Nullable
	protected Object convertToInternal(Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint) {

		Charset encoding = getEncoding(getMimeType(headers));
		Type type = getSerializationType(conversionHint);
		if (byte[].class == getSerializedPayloadClass()) {
			try (ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
					OutputStreamWriter writer = new OutputStreamWriter(out, encoding)) {
				if (type != null) {
					this.gson.toJson(payload, type, writer);
				}
				else {
					this.gson.toJson(payload, writer);
				}
				writer.flush();
				return out.toByteArray();
			}
			catch (IOException ex) {
				throw new MessageConversionException("Could not write JSON: " + ex.getMessage(), ex);
			}
		}
		else {
			if (type != null) {
				return this.gson.toJson(payload, type);
			}
			else {
				return this.gson.toJson(payload);
			}
		}
	}

	/**
	 * Determine the JSON encoding to use for the given content type.
	 * @param contentType the MIME type from the MessageHeaders, if any
	 * @return the JSON encoding to use (never {@code null})
	 */
	protected Charset getEncoding(@Nullable MimeType contentType) {
		if (contentType != null && (contentType.getCharset() != null)) {
			return contentType.getCharset();
		}
		return StandardCharsets.UTF_8;
	}

	/**
	 * Determine a serialization type based on the given conversion hint.
	 * @param conversionHint the conversion hint Object as passed into the
	 * converter for the current conversion attempt
	 * @return the serialization view class, or {@code null} if none
	 */
	@Nullable
	protected Type getSerializationType(@Nullable Object conversionHint) {
		if (conversionHint instanceof MethodParameter) {
			MethodParameter param = (MethodParameter) conversionHint;
			param = param.nestedIfOptional();
			if (Message.class.isAssignableFrom(param.getParameterType())) {
				param = param.nested();
			}
			Type genericParameterType = param.getNestedGenericParameterType();
			Class<?> contextClass = param.getContainingClass();
			return GenericTypeResolver.resolveType(genericParameterType, contextClass);
		}
		else if (conversionHint instanceof Class) {
			return (Class<?>) conversionHint;
		}
		return null;
	}
}
