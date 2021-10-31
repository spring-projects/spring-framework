/*
 * Copyright 2021-2021 the original author or authors.
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

package org.springframework.http.converter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.descriptors.PolymorphicKind;
import kotlinx.serialization.descriptors.SerialDescriptor;

import org.springframework.core.GenericTypeResolver;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;


/**
 * Abstract implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This converter can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 *
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Iain Henderson
 * @since 5.3
 */
public abstract class AbstractKotlinSerializationHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	/**
	 * Construct an {@code AbstractGenericHttpMessageConverter} with multiple supported media type.
	 * @param supportedMediaTypes the supported media types
	 */
	protected AbstractKotlinSerializationHttpMessageConverter(final MediaType... supportedMediaTypes) {
		super(supportedMediaTypes);
	}

	@Override
	protected boolean supports(final Class<?> clazz) {
		try {
			serializer(clazz);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	public boolean canRead(final Type type, @Nullable final Class<?> contextClass, @Nullable final MediaType mediaType) {
		try {
			serializer(GenericTypeResolver.resolveType(type, contextClass));
			return canRead(mediaType);
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	public boolean canWrite(@Nullable final Type type, final Class<?> clazz, @Nullable final MediaType mediaType) {
		try {
			serializer(type != null ? GenericTypeResolver.resolveType(type, clazz) : clazz);
			return canWrite(mediaType);
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	public final Object read(final Type type, @Nullable final Class<?> contextClass, final HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return decode(serializer(GenericTypeResolver.resolveType(type, contextClass)), inputMessage);
	}

	@Override
	protected final Object readInternal(final Class<?> clazz, final HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return decode(serializer(clazz), inputMessage);
	}

	protected abstract Object decode(final KSerializer<Object> serializer, final HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	@Override
	protected final void writeInternal(final Object object, @Nullable final Type type, final HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		encode(object, serializer(type != null ? type : object.getClass()), outputMessage);
	}

	protected abstract void encode(final Object object, final KSerializer<Object> serializer, final HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

	/**
	 * Tries to find a serializer that can marshall or unmarshall instances of the given type
	 * using kotlinx.serialization. If no serializer can be found, an exception is thrown.
	 * <p>Resolved serializers are cached and cached results are returned on successive calls.
	 * TODO Avoid relying on throwing exception when https://github.com/Kotlin/kotlinx.serialization/pull/1164 is fixed
	 * @param type the type to find a serializer for
	 * @return a resolved serializer for the given type
	 * @throws RuntimeException if no serializer supporting the given type can be found
	 */
	private KSerializer<Object> serializer(final Type type) {
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
