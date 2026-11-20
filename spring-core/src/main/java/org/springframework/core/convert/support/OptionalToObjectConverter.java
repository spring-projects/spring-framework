/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
