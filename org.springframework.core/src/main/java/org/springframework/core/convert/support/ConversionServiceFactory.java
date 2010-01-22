/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert.support;

import java.util.Set;

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
 * @since 3.0
 */
public abstract class ConversionServiceFactory {

	/**
	 * Create a new default ConversionService instance that can be safely modified.
	 */
	public static GenericConversionService createDefaultConversionService() {
		GenericConversionService conversionService = new GenericConversionService();
		addDefaultConverters(conversionService);
		return conversionService;
	}

	/**
	 * Populate the given ConversionService instance with all applicable default converters.
	 */
	public static void addDefaultConverters(GenericConversionService conversionService) {
		conversionService.addConverter(new ArrayToCollectionConverter(conversionService));
		conversionService.addConverter(new CollectionToArrayConverter(conversionService));

		conversionService.addConverter(new ArrayToStringConverter(conversionService));
		conversionService.addConverter(new StringToArrayConverter(conversionService));

		conversionService.addConverter(new ArrayToObjectConverter(conversionService));
		conversionService.addConverter(new ObjectToArrayConverter(conversionService));

		conversionService.addConverter(new CollectionToStringConverter(conversionService));
		conversionService.addConverter(new StringToCollectionConverter(conversionService));

		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		conversionService.addConverter(new ObjectToCollectionConverter(conversionService));
		
		conversionService.addConverter(new ArrayToArrayConverter(conversionService));
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverter(new MapToMapConverter(conversionService));

		conversionService.addConverter(new PropertiesToStringConverter());
		conversionService.addConverter(new StringToPropertiesConverter());

		conversionService.addConverter(new StringToBooleanConverter());
		conversionService.addConverter(new StringToCharacterConverter());		
		conversionService.addConverter(new StringToLocaleConverter());
		conversionService.addConverterFactory(new StringToNumberConverterFactory());	
		conversionService.addConverterFactory(new StringToEnumConverterFactory());

		conversionService.addConverter(new NumberToCharacterConverter());
		conversionService.addConverterFactory(new CharacterToNumberFactory());

		conversionService.addConverterFactory(new NumberToNumberConverterFactory());
		
		conversionService.addConverter(new ObjectToStringConverter());
		conversionService.addConverter(new ObjectToObjectConverter());
		conversionService.addConverter(new IdToEntityConverter(conversionService));
	}

	/**
	 * Register the given converter objects with the given target registry.
	 * @param converters the converter objects: implementing {@link Converter},
	 * {@link ConverterFactory}, or {@link GenericConverter}
	 * @param registry the target registry to register with
	 */
	public static void registerConverters(Set<?> converters, ConverterRegistry registry) {
		if (converters != null) {
			for (Object converter : converters) {
				if (converter instanceof GenericConverter) {
					registry.addConverter((GenericConverter) converter);
				}
				else if (converter instanceof Converter<?, ?>) {
					registry.addConverter((Converter<?, ?>) converter);
				}
				else if (converter instanceof ConverterFactory<?, ?>) {
					registry.addConverterFactory((ConverterFactory<?, ?>) converter);
				}
				else {
					throw new IllegalArgumentException("Each converter object must implement one of the " +
							"Converter, ConverterFactory, or GenericConverter interfaces");
				}
			}
		}
	}

}
