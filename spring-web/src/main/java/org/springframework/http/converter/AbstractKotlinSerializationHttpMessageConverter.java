/*
 * Copyright 2002-2024 the original author or authors.
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
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import kotlin.reflect.KFunction;
import kotlin.reflect.KType;
import kotlin.reflect.full.KCallables;
import kotlin.reflect.jvm.ReflectJvmMapping;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerialFormat;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.descriptors.PolymorphicKind;
import kotlinx.serialization.descriptors.SerialDescriptor;

import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
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
public abstract class AbstractKotlinSerializationHttpMessageConverter<T extends SerialFormat> extends AbstractSmartHttpMessageConverter<Object> {

	private final Map<KType, KSerializer<Object>> kTypeSerializerCache = new ConcurrentReferenceHashMap<>();

	private final Map<Type, KSerializer<Object>> typeSerializerCache = new ConcurrentReferenceHashMap<>();

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
		return serializer(ResolvableType.forClass(clazz)) != null;
	}

	@Override
	public boolean canRead(ResolvableType type, @Nullable MediaType mediaType) {
		if (!ResolvableType.NONE.equals(type) && serializer(type) != null) {
			return canRead(mediaType);
		}
		else {
			return false;
		}
	}

	@Override
	public boolean canWrite(ResolvableType type, Class<?> clazz, @Nullable MediaType mediaType) {
		if (!ResolvableType.NONE.equals(type) && serializer(type) != null) {
			return canWrite(mediaType);
		}
		else {
			return false;
		}
	}

	@Override
	public final Object read(ResolvableType type, HttpInputMessage inputMessage, @Nullable Map<String, Object> hints)
			throws IOException, HttpMessageNotReadableException {

		KSerializer<Object> serializer = serializer(type);
		if (serializer == null) {
			throw new HttpMessageNotReadableException("Could not find KSerializer for " + type, inputMessage);
		}
		return readInternal(serializer, this.format, inputMessage);
	}

	/**
	 * Reads the given input message with the given serializer and format.
	 */
	protected abstract Object readInternal(KSerializer<Object> serializer, T format, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	@Override
	protected final void writeInternal(Object object, ResolvableType type, HttpOutputMessage outputMessage,
			@Nullable Map<String, Object> hints) throws IOException, HttpMessageNotWritableException {

		ResolvableType resolvableType = (ResolvableType.NONE.equals(type) ? ResolvableType.forInstance(object) : type);
		KSerializer<Object> serializer = serializer(resolvableType);
		if (serializer == null) {
			throw new HttpMessageNotWritableException("Could not find KSerializer for " + resolvableType);
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
	 * @param resolvableType the type to find a serializer for
	 * @return a resolved serializer for the given type, or {@code null}
	 */
	@Nullable
	private KSerializer<Object> serializer(ResolvableType resolvableType) {
		if (resolvableType.getSource() instanceof MethodParameter parameter) {
			Method method = parameter.getMethod();
			Assert.notNull(method, "Method must not be null");
			if (KotlinDetector.isKotlinType(method.getDeclaringClass())) {
				KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
				Assert.notNull(function, "Kotlin function must not be null");
				KType type = (parameter.getParameterIndex() == -1 ? function.getReturnType() :
						KCallables.getValueParameters(function).get(parameter.getParameterIndex()).getType());
				KSerializer<Object> serializer = this.kTypeSerializerCache.get(type);
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
						this.kTypeSerializerCache.put(type, serializer);
					}
				}
				return serializer;
			}
		}
		Type type = resolvableType.getType();
		KSerializer<Object> serializer = this.typeSerializerCache.get(type);
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
				this.typeSerializerCache.put(type, serializer);
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

	@Override
	protected boolean supportsRepeatableWrites(Object object) {
		return true;
	}
}
