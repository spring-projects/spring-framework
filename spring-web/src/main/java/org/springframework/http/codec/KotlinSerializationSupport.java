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

package org.springframework.http.codec;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;

/**
 * Base class providing support methods for encoding and decoding with Kotlin
 * serialization.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @author Arjen Poutsma
 * @since 6.0
 * @param <T> the type of {@link SerialFormat}
 */
public abstract class KotlinSerializationSupport<T extends SerialFormat> {

	private final Map<Type, KSerializer<Object>> typeSerializerCache = new ConcurrentReferenceHashMap<>();

	private final Map<KType, KSerializer<Object>> kTypeSerializerCache = new ConcurrentReferenceHashMap<>();


	private final T format;

	private final List<MimeType> supportedMimeTypes;

	/**
	 * Creates a new instance of this support class with the given format
	 * and supported mime types.
	 */
	protected KotlinSerializationSupport(T format, MimeType... supportedMimeTypes) {
		this.format = format;
		this.supportedMimeTypes = Arrays.asList(supportedMimeTypes);
	}

	/**
	 * Returns the format.
	 */
	protected final T format() {
		return this.format;
	}

	/**
	 * Returns the supported mime types.
	 */
	protected final List<MimeType> supportedMimeTypes() {
		return this.supportedMimeTypes;
	}

	/**
	 * Indicates whether the given type can be serialized using Kotlin
	 * serialization.
	 * @param type the type to be serialized
	 * @param mimeType the mimetype to use (can be {@code null})
	 * @return {@code true} if {@code type} can be serialized; false otherwise
	 */
	protected final boolean canSerialize(ResolvableType type, @Nullable MimeType mimeType) {
		KSerializer<Object> serializer = serializer(type);
		if (serializer == null) {
			return false;
		}
		else {
			return (supports(mimeType) && !String.class.isAssignableFrom(type.toClass()) &&
					!ServerSentEvent.class.isAssignableFrom(type.toClass()));
		}

	}

	private boolean supports(@Nullable MimeType mimeType) {
		if (mimeType == null) {
			return true;
		}
		for (MimeType candidate : this.supportedMimeTypes) {
			if (candidate.isCompatibleWith(mimeType)) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Returns the serializer that can (de)serialize instances of the given
	 * type. If no serializer can be found, or if {@code resolvableType} is
	 * a <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic</a>
	 * type, {@code null} is returned.
	 * @param resolvableType the type to find a serializer for
	 * @return a resolved serializer for the given type, or {@code null}
	 */
	@Nullable
	protected final KSerializer<Object> serializer(ResolvableType resolvableType) {
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

	private static boolean hasPolymorphism(SerialDescriptor descriptor, Set<String> alreadyProcessed) {
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
