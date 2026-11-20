/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format;

import org.springframework.core.convert.converter.Converter;

/**
 * Registers {@link Converter Converters} and {@link Formatter Formatters} with
 * a FormattingConversionService through the {@link FormatterRegistry} SPI.
 *
 * @author Keith Donald
 * @since 3.1
 */
public interface FormatterRegistrar {

	/**
	 * Register Formatters and Converters with a FormattingConversionService
	 * through a FormatterRegistry SPI.
	 * @param registry the FormatterRegistry instance to use.
	 */
	void registerFormatters(FormatterRegistry registry);

}
