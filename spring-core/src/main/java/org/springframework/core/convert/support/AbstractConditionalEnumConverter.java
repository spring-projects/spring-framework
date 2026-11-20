/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.util.ClassUtils;

/**
 * A {@link ConditionalConverter} base implementation for enum-based converters.
 *
 * @author Stephane Nicoll
 * @since 4.3
 */
abstract class AbstractConditionalEnumConverter implements ConditionalConverter {

	private final ConversionService conversionService;


	protected AbstractConditionalEnumConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		for (Class<?> interfaceType : ClassUtils.getAllInterfacesForClassAsSet(sourceType.getType())) {
			if (this.conversionService.canConvert(TypeDescriptor.valueOf(interfaceType), targetType)) {
				return false;
			}
		}
		return true;
	}

}
