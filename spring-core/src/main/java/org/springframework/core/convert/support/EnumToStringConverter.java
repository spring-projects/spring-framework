/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

/**
 * Calls {@link Enum#name()} to convert a source Enum to a String.
 * This converter will not match enums with interfaces that can be converted.
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @since 3.0
 */
final class EnumToStringConverter extends AbstractConditionalEnumConverter implements Converter<Enum<?>, String> {

	public EnumToStringConverter(ConversionService conversionService) {
		super(conversionService);
	}

	@Override
	public String convert(Enum<?> source) {
		return source.name();
	}

}
