/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

/**
 * Calls {@link Enum#ordinal()} to convert a source Enum to an Integer.
 * This converter will not match enums with interfaces that can be converted.
 *
 * @author Yanming Zhou
 * @since 4.3
 */
final class EnumToIntegerConverter extends AbstractConditionalEnumConverter implements Converter<Enum<?>, Integer> {

	public EnumToIntegerConverter(ConversionService conversionService) {
		super(conversionService);
	}

	@Override
	public Integer convert(Enum<?> source) {
		return source.ordinal();
	}

}
