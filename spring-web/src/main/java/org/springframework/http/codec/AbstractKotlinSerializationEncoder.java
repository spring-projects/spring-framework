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
import java.util.List;
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
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This encoder can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 * It supports various Content Types.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @since 5.3
 */
abstract class AbstractKotlinSerializationEncoder extends AbstractEncoder<Object> {

	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	public AbstractKotlinSerializationEncoder(final MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
	}

	@Override
	public boolean canEncode(final ResolvableType elementType, @Nullable final MimeType mimeType) {
		try {
			serializer(elementType.getType());
			return (super.canEncode(elementType, mimeType) && !String.class.isAssignableFrom(elementType.toClass()) &&
					!ServerSentEvent.class.isAssignableFrom(elementType.toClass()));
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	public Flux<DataBuffer> encode(final Publisher<?> inputStream, final DataBufferFactory bufferFactory,
									final ResolvableType elementType, @Nullable final MimeType mimeType, @Nullable final Map<String, Object> hints) {

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
	public abstract DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory,
			ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

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
