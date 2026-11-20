/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * A factory for common {@link org.springframework.core.convert.ConversionService}
 * configurations.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 */
public final class ConversionServiceFactory {

	private ConversionServiceFactory() {
	}


	/**
	 * Register the given Converter objects with the given target ConverterRegistry.
	 * @param converters the converter objects: implementing {@link Converter},
	 * {@link ConverterFactory}, or {@link GenericConverter}
	 * @param registry the target registry
	 */
	public static void registerConverters(@Nullable Set<?> converters, ConverterRegistry registry) {
		if (converters != null) {
			for (Object candidate : converters) {
				if (candidate instanceof GenericConverter genericConverter) {
					registry.addConverter(genericConverter);
				}
				else if (candidate instanceof Converter<?, ?> converter) {
					registry.addConverter(converter);
				}
				else if (candidate instanceof ConverterFactory<?, ?> converterFactory) {
					registry.addConverterFactory(converterFactory);
				}
				else {
					throw new IllegalArgumentException("Each converter object must implement one of the " +
							"Converter, ConverterFactory, or GenericConverter interfaces");
				}
			}
		}
	}

}
