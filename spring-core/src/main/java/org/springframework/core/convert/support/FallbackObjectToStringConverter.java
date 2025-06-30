/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.convert.support;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Simply calls {@link Object#toString()} to convert any supported object
 * to a {@link String}.
 *
 * <p>Supports {@link CharSequence}, {@link StringWriter}, and any class
 * with a String constructor or one of the following static factory methods:
 * {@code valueOf(String)}, {@code of(String)}, {@code from(String)}.
 *
 * <p>Used by the {@link DefaultConversionService} as a fallback if there
 * are no other explicit to-String converters registered.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see ObjectToObjectConverter
 */
final class FallbackObjectToStringConverter implements ConditionalGenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, String.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Class<?> sourceClass = sourceType.getObjectType();
		if (String.class == sourceClass) {
			// no conversion required
			return false;
		}
		return (CharSequence.class.isAssignableFrom(sourceClass) ||
				StringWriter.class.isAssignableFrom(sourceClass) ||
				ObjectToObjectConverter.hasConversionMethodOrConstructor(sourceClass, String.class));
	}

	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (source != null ? source.toString() : null);
	}

}
