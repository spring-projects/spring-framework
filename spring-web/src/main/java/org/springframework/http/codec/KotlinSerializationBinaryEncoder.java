/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.List;
import java.util.Map;

import kotlinx.serialization.BinaryFormat;
import kotlinx.serialization.KSerializer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * Abstract base class for {@link Encoder} implementations that defer to Kotlin
 * {@linkplain BinaryFormat binary serializers}.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @author Arjen Poutsma
 * @since 6.0
 * @param <T> the type of {@link BinaryFormat}
 */
public abstract class KotlinSerializationBinaryEncoder<T extends BinaryFormat> extends KotlinSerializationSupport<T>
		implements Encoder<Object> {

	// ByteArraySequence encoding needed for now, see https://github.com/Kotlin/kotlinx.serialization/issues/204 for more details
	private final ByteArrayEncoder byteArrayEncoder = new ByteArrayEncoder();

	protected KotlinSerializationBinaryEncoder(T format, MimeType... supportedMimeTypes) {
		super(format, supportedMimeTypes);
	}

	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return canSerialize(elementType, mimeType);
	}

	@Override
	public List<MimeType> getEncodableMimeTypes() {
		return supportedMimeTypes();
	}

	@Override
	public List<MimeType> getEncodableMimeTypes(ResolvableType elementType) {
		return supportedMimeTypes();
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
		if (inputStream instanceof Mono) {
			return Mono.from(inputStream)
					.map(value -> encodeValue(value, bufferFactory, elementType, mimeType, hints))
					.flux();
		}
		else {
			ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, elementType);
			return Flux.from(inputStream)
					.collectList()
					.map(list -> encodeValue(list, bufferFactory, listType, mimeType, hints))
					.flux();
		}
	}

	@Override
	public DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory, ResolvableType valueType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		KSerializer<Object> serializer = serializer(valueType);
		if (serializer == null) {
			throw new EncodingException("Could not find KSerializer for " + valueType);
		}
		byte[] bytes = format().encodeToByteArray(serializer, value);
		return this.byteArrayEncoder.encodeValue(bytes, bufferFactory, valueType, mimeType, null);
	}
}
