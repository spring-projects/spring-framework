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

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializationException;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.json.Json;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StreamUtils;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter} that can read and write JSON using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This converter can be used to bind {@code @Serializable} Kotlin classes. It supports {@code application/json} and
 * {@code application/*+json} with various character sets, {@code UTF-8} being the default.
 *
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 * @since 5.3
 */
public class KotlinSerializationJsonHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	private final Json json;

	/**
	 * Construct a new {@code KotlinSerializationJsonHttpMessageConverter} with the default configuration.
	 */
	public KotlinSerializationJsonHttpMessageConverter() {
		this(Json.Default);
	}

	/**
	 * Construct a new {@code KotlinSerializationJsonHttpMessageConverter} with a custom configuration.
	 */
	public KotlinSerializationJsonHttpMessageConverter(Json json) {
		super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
		this.json = json;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		try {
			resolve(clazz);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		return this.read(clazz, null, inputMessage);
	}

	@Override
	public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		MediaType contentType = inputMessage.getHeaders().getContentType();
		String jsonText = StreamUtils.copyToString(inputMessage.getBody(), getCharsetToUse(contentType));
		try {
			// TODO Use stream based API when available
			return this.json.decodeFromString(resolve(type), jsonText);
		}
		catch (SerializationException ex) {
			throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex, inputMessage);
		}
	}

	@Override
	protected void writeInternal(Object o, HttpOutputMessage outputMessage) throws HttpMessageNotWritableException {
		try {
			this.writeInternal(o, o.getClass(), outputMessage);
		}
		catch (IOException ex) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected void writeInternal(Object o, @Nullable Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
		try {
			String json = this.json.encodeToString(resolve(type), o);
			MediaType contentType = outputMessage.getHeaders().getContentType();
			outputMessage.getBody().write(json.getBytes(getCharsetToUse(contentType)));
			outputMessage.getBody().flush();
		}
		catch (IOException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

	private Charset getCharsetToUse(@Nullable MediaType contentType) {
		if (contentType != null && contentType.getCharset() != null) {
			return contentType.getCharset();
		}
		return DEFAULT_CHARSET;
	}

	/**
	 * Tries to find a serializer that can marshall or unmarshall instances of the given type using
	 * kotlinx.serialization. If no serializer can be found, an exception is thrown.
	 * <p>
	 * Resolved serializers are cached and cached results are returned on successive calls.
	 *
	 * @param type to find a serializer for.
	 * @return resolved serializer for the given type.
	 * @throws RuntimeException if no serializer supporting the given type can be found.
	 */
	private KSerializer<Object> resolve(Type type) {
		KSerializer<Object> serializer = serializerCache.get(type);
		if (serializer == null) {
			serializer = SerializersKt.serializer(type);
			serializerCache.put(type, serializer);
		}
		return serializer;
	}
}
