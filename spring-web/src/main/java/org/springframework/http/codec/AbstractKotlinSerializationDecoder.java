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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.descriptors.PolymorphicKind;
import kotlinx.serialization.descriptors.SerialDescriptor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;

/**
 * Decode a byte stream into Objects with
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This decoder can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 * It supports various Content Types.
 *
 * <p>Decoding streams is not supported yet, see
 * <a href="https://github.com/Kotlin/kotlinx.serialization/issues/1073">kotlinx.serialization/issues/1073</a>
 * related issue.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @since 5.3
 */
abstract class AbstractKotlinSerializationDecoder extends AbstractDecoder<Object> {

	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();


	public AbstractKotlinSerializationDecoder(final MimeType... supportedMineTypes) {
		super(supportedMineTypes);
	}

	/**
	 * Configure a limit on the number of bytes that can be buffered whenever
	 * the input stream needs to be aggregated. This can be a result of
	 * decoding to a single {@code DataBuffer},
	 * {@link java.nio.ByteBuffer ByteBuffer}, {@code byte[]},
	 * {@link org.springframework.core.io.Resource Resource}, {@code String}, etc.
	 * It can also occur when splitting the input stream, e.g. delimited text,
	 * in which case the limit applies to data buffered between delimiters.
	 * <p>By default this is set to 256K.
	 * @param byteCount the max number of bytes to buffer, or -1 for unlimited
	 */
	public abstract void setMaxInMemorySize(final int byteCount);

	/**
	 * Return the {@link #setMaxInMemorySize configured} byte count limit.
	 */
	public abstract int getMaxInMemorySize();


	@Override
	public boolean canDecode(final ResolvableType elementType, @Nullable final MimeType mimeType) {
		try {
			serializer(elementType.getType());
			return (super.canDecode(elementType, mimeType) && !CharSequence.class.isAssignableFrom(elementType.toClass()));
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	public Flux<Object> decode(final Publisher<DataBuffer> inputStream, final ResolvableType elementType,
								@Nullable final MimeType mimeType, @Nullable final Map<String, Object> hints) {

		return Flux.error(new UnsupportedOperationException());
	}

	@Override
	public abstract Mono<Object> decodeToMono(final Publisher<DataBuffer> inputStream, final ResolvableType elementType,
								@Nullable final MimeType mimeType, @Nullable final Map<String, Object> hints);

	/**
	 * Tries to find a serializer that can marshall or unmarshall instances of the given type
	 * using kotlinx.serialization. If no serializer can be found, an exception is thrown.
	 * <p>Resolved serializers are cached and cached results are returned on successive calls.
	 * TODO Avoid relying on throwing exception when https://github.com/Kotlin/kotlinx.serialization/pull/1164 is fixed
	 * @param type the type to find a serializer for
	 * @return a resolved serializer for the given type
	 * @throws RuntimeException if no serializer supporting the given type can be found
	 */
	protected KSerializer<Object> serializer(final Type type) {
		KSerializer<Object> serializer = serializerCache.get(type);
		if (serializer == null) {
			serializer = SerializersKt.serializer(type);
			if (hasPolymorphism(serializer.getDescriptor(), new HashSet<>())) {
				throw new UnsupportedOperationException("Open polymorphic serialization is not supported yet");
			}
			serializerCache.put(type, serializer);
		}
		return serializer;
	}

	private boolean hasPolymorphism(final SerialDescriptor descriptor, final Set<String> alreadyProcessed) {
		alreadyProcessed.add(descriptor.getSerialName());
		if (descriptor.getKind().equals(PolymorphicKind.OPEN.INSTANCE)) {
			return true;
		}
		for (int i = 0 ; i < descriptor.getElementsCount() ; i++) {
			SerialDescriptor elementDescriptor = descriptor.getElementDescriptor(i);
			if (!alreadyProcessed.contains(elementDescriptor.getSerialName()) && hasPolymorphism(elementDescriptor, alreadyProcessed)) {
				return true;
			}
		}
		return false;
	}

}
