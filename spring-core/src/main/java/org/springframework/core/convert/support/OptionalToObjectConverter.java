/*
 * Copyright 2002-2025 the original author or authors.
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

import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Convert an {@link Optional} to an {@link Object} by unwrapping the {@code Optional},
 * using the {@link ConversionService} to convert the object contained in the
 * {@code Optional} (potentially {@code null}) to the target type.
 *
 * @author Sam Brannen
 * @since 7.0
 * @see ObjectToOptionalConverter
 */
final class OptionalToObjectConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;


	OptionalToObjectConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Set.of(new ConvertiblePair(Optional.class, Object.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType.getElementTypeDescriptor(), targetType, this.conversionService);
	}

	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		Optional<?> optional = (Optional<?>) source;
		Object unwrappedSource = optional.orElse(null);
		TypeDescriptor unwrappedSourceType = TypeDescriptor.forObject(unwrappedSource);
		return this.conversionService.convert(unwrappedSource, unwrappedSourceType, targetType);
	}

}
