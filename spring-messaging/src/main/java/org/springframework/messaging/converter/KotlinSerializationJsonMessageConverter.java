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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Map;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.json.Json;

import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.FileCopyUtils;

/**
 * Implementation of {@link MessageConverter} that can read and write JSON
 * using <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This converter can be used to bind {@code @Serializable} Kotlin classes.
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
public class KotlinSerializationJsonMessageConverter extends AbstractJsonMessageConverter {

	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	private final Json json;


	/**
	 * Construct a new {@code KotlinSerializationJsonMessageConverter} with default configuration.
	 */
	public KotlinSerializationJsonMessageConverter() {
		this(Json.Default);
	}

	/**
	 * Construct a new {@code KotlinSerializationJsonMessageConverter} with the given delegate.
	 * @param json the Json instance to use
	 */
	public KotlinSerializationJsonMessageConverter(Json json) {
		this.json = json;
	}

	@Override
	protected Object fromJson(Reader reader, Type resolvedType) {
		try {
			return fromJson(FileCopyUtils.copyToString(reader), resolvedType);
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected Object fromJson(String payload, Type resolvedType) {
		return this.json.decodeFromString(serializer(resolvedType), payload);
	}

	@Override
	protected void toJson(Object payload, Type resolvedType, Writer writer) {
		try {
			writer.write(toJson(payload, resolvedType).toCharArray());
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected String toJson(Object payload, Type resolvedType) {
		return this.json.encodeToString(serializer(resolvedType), payload);
	}

	/**
	 * Tries to find a serializer that can marshall or unmarshall instances of the given type
	 * using kotlinx.serialization. If no serializer can be found, an exception is thrown.
	 * <p>Resolved serializers are cached and cached results are returned on successive calls.
	 * TODO Avoid relying on throwing exception when https://github.com/Kotlin/kotlinx.serialization/pull/1164 is fixed
	 * @param type the type to find a serializer for
	 * @return a resolved serializer for the given type
	 * @throws RuntimeException if no serializer supporting the given type can be found
	 */
	private KSerializer<Object> serializer(Type type) {
		KSerializer<Object> serializer = serializerCache.get(type);
		if (serializer == null) {
			serializer = SerializersKt.serializer(type);
			serializerCache.put(type, serializer);
		}
		return serializer;
	}
}
