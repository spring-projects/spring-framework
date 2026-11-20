/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Converts a Collection to a comma-delimited String.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class CollectionToStringConverter implements ConditionalGenericConverter {

	private static final String DELIMITER = ",";

	private final ConversionService conversionService;


	public CollectionToStringConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, String.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(
				sourceType.getElementTypeDescriptor(), targetType, this.conversionService);
	}

	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (!(source instanceof Collection<?> sourceCollection)) {
			return null;
		}
		if (sourceCollection.isEmpty()) {
			return "";
		}
		StringJoiner sj = new StringJoiner(DELIMITER);
		for (Object sourceElement : sourceCollection) {
			Object targetElement = this.conversionService.convert(
					sourceElement, sourceType.elementTypeDescriptor(sourceElement), targetType);
			sj.add(String.valueOf(targetElement));
		}
		return sj.toString();
	}

}
