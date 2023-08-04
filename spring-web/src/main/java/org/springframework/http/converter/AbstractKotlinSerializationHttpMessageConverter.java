/*
 * Copyright 2002-2023 the original author or authors.
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
import kotlinx.serialization.SerialFormat;
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
 * Abstract base class for {@link HttpMessageConverter} implementations that
 * use Kotlin serialization.
 *
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Iain Henderson
 * @author Arjen Poutsma
 * @since 6.0
 * @param <T> the type of {@link SerialFormat}
 */
public abstract class AbstractKotlinSerializationHttpMessageConverter<T extends SerialFormat> extends AbstractGenericHttpMessageConverter<Object> {

	private final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	private final T format;


	/**
	 * Construct an {@code AbstractKotlinSerializationHttpMessageConverter} with multiple supported media type and
	 * format.
	 * @param format the format
	 * @param supportedMediaTypes the supported media types
	 */
	protected AbstractKotlinSerializationHttpMessageConverter(T format, MediaType... supportedMediaTypes) {
		super(supportedMediaTypes);
		this.format = format;
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return serializer(clazz) != null;
	}

	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
		if (serializer(GenericTypeResolver.resolveType(type, contextClass)) != null) {
			return canRead(mediaType);
		}
		else {
			return false;
		}
	}

	@Override
	public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
		if (serializer(type != null ? GenericTypeResolver.resolveType(type, clazz) : clazz) != null) {
			return canWrite(mediaType);
		}
		else {
			return false;
		}
	}

	@Override
	public final Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		Type resolvedType = GenericTypeResolver.resolveType(type, contextClass);
		KSerializer<Object> serializer = serializer(resolvedType);
		if (serializer == null) {
			throw new HttpMessageNotReadableException("Could not find KSerializer for " + resolvedType, inputMessage);
		}
		return readInternal(serializer, this.format, inputMessage);
	}

	@Override
	protected final Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		KSerializer<Object> serializer = serializer(clazz);
		if (serializer == null) {
			throw new HttpMessageNotReadableException("Could not find KSerializer for " + clazz, inputMessage);
		}
		return readInternal(serializer, this.format, inputMessage);
	}

	/**
	 * Reads the given input message with the given serializer and format.
	 */
	protected abstract Object readInternal(KSerializer<Object> serializer, T format, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	@Override
	protected final void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		Type resolvedType = type != null ? type : object.getClass();
		KSerializer<Object> serializer = serializer(resolvedType);
		if (serializer == null) {
			throw new HttpMessageNotWritableException("Could not find KSerializer for " + resolvedType);
		}
		writeInternal(object, serializer, this.format, outputMessage);
	}

	/**
	 * Write the given object to the output message with the given serializer and format.
	 */
	protected abstract void writeInternal(Object object, KSerializer<Object> serializer, T format,
			HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException;

	/**
	 * Tries to find a serializer that can marshall or unmarshall instances of the given type
	 * using kotlinx.serialization. If no serializer can be found, {@code null} is returned.
	 * <p>Resolved serializers are cached and cached results are returned on successive calls.
	 * @param type the type to find a serializer for
	 * @return a resolved serializer for the given type, or {@code null}
	 */
	@Nullable
	private KSerializer<Object> serializer(Type type) {
		KSerializer<Object> serializer = this.serializerCache.get(type);
		if (serializer == null) {
			try {
				serializer = SerializersKt.serializerOrNull(this.format.getSerializersModule(), type);
			}
			catch (IllegalArgumentException ignored) {
			}
			if (serializer != null) {
				if (hasPolymorphism(serializer.getDescriptor(), new HashSet<>())) {
					return null;
				}
				this.serializerCache.put(type, serializer);
			}
		}
		return serializer;
	}

	private boolean hasPolymorphism(SerialDescriptor descriptor, Set<String> alreadyProcessed) {
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
