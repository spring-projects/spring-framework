/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.codec;

import java.lang.reflect.Type;
import java.util.Map;

import kotlinx.serialization.BinaryFormat;
import kotlinx.serialization.KSerializer;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This encoder can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 * It supports various Content Types
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @since 5.3
 */
public abstract class KotlinSerializationBinaryEncoder extends AbstractKotlinSerializationEncoder {

	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	private final BinaryFormat format;

	// CharSequence encoding needed for now, see https://github.com/Kotlin/kotlinx.serialization/issues/204 for more details
	private final ByteArrayEncoder byteArrayEncoder = new ByteArrayEncoder();

	public KotlinSerializationBinaryEncoder(final BinaryFormat format, final MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
		this.format = format;
	}

	@Override
	public DataBuffer encodeValue(final Object value, final DataBufferFactory bufferFactory,
									final ResolvableType valueType, @Nullable final MimeType mimeType, @Nullable final Map<String, Object> hints) {

		byte[] bytes = this.format.encodeToByteArray(serializer(valueType.getType()), value);
		return this.byteArrayEncoder.encodeValue(bytes, bufferFactory, valueType, mimeType, null);
	}
}
