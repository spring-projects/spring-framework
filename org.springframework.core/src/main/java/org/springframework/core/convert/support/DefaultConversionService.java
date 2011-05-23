/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.Locale;

/**
 * A specialization of {@link GenericConversionService} configured by default with
 * converters appropriate for most applications.
 *
 * <p>Designed for direct instantiation but also exposes the static
 * {@link #addDefaultConverters} utility method for ad hoc use against any
 * {@code GenericConversionService} instance.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class DefaultConversionService extends GenericConversionService {

	/**
	 * Create a new {@code DefaultConversionService} with the set of
	 * {@linkplain DefaultConversionService#addDefaultConverters default converters}.
	 */
	public DefaultConversionService() {
		addDefaultConverters(this);
	}

	/**
	 * Add converters appropriate for most environments.
	 * @param conversionService the service to register default formatters against
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
		conversionService.addConverter(Boolean.class, String.class, new ObjectToStringConverter());

		conversionService.addConverter(new StringToCharacterConverter());
		conversionService.addConverter(Character.class, String.class, new ObjectToStringConverter());

		conversionService.addConverter(new StringToLocaleConverter());
		conversionService.addConverter(Locale.class, String.class, new ObjectToStringConverter());
		
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		conversionService.addConverter(Number.class, String.class, new ObjectToStringConverter());
		
		conversionService.addConverterFactory(new StringToEnumConverterFactory());
		conversionService.addConverter(Enum.class, String.class, new EnumToStringConverter());
		
		conversionService.addConverter(new NumberToCharacterConverter());
		conversionService.addConverterFactory(new CharacterToNumberFactory());

		conversionService.addConverterFactory(new NumberToNumberConverterFactory());

		conversionService.addConverter(new ObjectToObjectConverter());
		conversionService.addConverter(new IdToEntityConverter(conversionService));
		conversionService.addConverter(new FallbackObjectToStringConverter());
	}

}
