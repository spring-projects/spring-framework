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

package org.springframework.http.converter.json;

import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonConfiguration;
import kotlinx.serialization.json.JsonDecodingException;
import kotlinx.serialization.modules.EmptyModule;
import kotlinx.serialization.modules.SerialModule;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.KotlinSerializationResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter} that can read and write JSON using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This converter can be used to bind {@code @Serializable} Kotlin classes. It supports {@code application/json} and
 * {@code application/*+json} with various character sets, {@code UTF-8} being the default.
 *
 * @author Andreas Ahlenstorf
 */
public class KotlinSerializationJsonHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private final KotlinSerializationResolver resolver = new KotlinSerializationResolver();

	private final Json json;

	/**
	 * Construct a new {@code KotlinSerializationJsonHttpMessageConverter} with default configuration.
	 */
	public KotlinSerializationJsonHttpMessageConverter() {
		this(JsonConfiguration.getDefault(), EmptyModule.INSTANCE);
	}

	/**
	 * Construct a new {@code KotlinSerializationJsonHttpMessageConverter} with custom configuration.
	 */
	public KotlinSerializationJsonHttpMessageConverter(JsonConfiguration jsonConfiguration, SerialModule serialModule) {
		super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
		this.json = new Json(jsonConfiguration, serialModule);
	}

	@Override
	protected boolean supports(@NotNull Class<?> clazz) {
		try {
			this.resolver.resolve(clazz);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@NotNull
	@Override
	protected Object readInternal(
			@NotNull Class<?> clazz,
			@NotNull HttpInputMessage inputMessage
	) throws IOException, HttpMessageNotReadableException {
		MediaType contentType = inputMessage.getHeaders().getContentType();
		String jsonText = StreamUtils.copyToString(inputMessage.getBody(), getCharsetToUse(contentType));

		try {
			return this.json.parse(this.resolver.resolve(clazz), jsonText);
		} catch (JsonDecodingException ex) {
			throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex, inputMessage);
		}
	}

	@Override
	protected void writeInternal(
			@NotNull Object o,
			@NotNull HttpOutputMessage outputMessage
	) throws IOException, HttpMessageNotWritableException {
		try {
			String json = this.json.stringify(this.resolver.resolve(o.getClass()), o);
			MediaType contentType = outputMessage.getHeaders().getContentType();
			outputMessage.getBody().write(json.getBytes(getCharsetToUse(contentType)));
			outputMessage.getBody().flush();
		} catch (Exception ex) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

	private Charset getCharsetToUse(@Nullable MediaType contentType) {
		if (contentType != null && contentType.getCharset() != null) {
			return contentType.getCharset();
		}
		return DEFAULT_CHARSET;
	}
}
