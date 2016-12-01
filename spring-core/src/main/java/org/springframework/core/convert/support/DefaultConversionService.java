/*
 * Copyright 2002-2016 the original author or authors.
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

import java.nio.charset.Charset;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.util.ClassUtils;

/**
 * A specialization of {@link GenericConversionService} configured by default with
 * converters appropriate for most environments.
 *
 * <p>Designed for direct instantiation but also exposes the static
 * {@link #addDefaultConverters(ConverterRegistry)} utility method for ad hoc use against any
 * {@code ConverterRegistry} instance.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public class DefaultConversionService extends GenericConversionService {

	/** Java 8's java.util.Optional class available? */
	private static final boolean javaUtilOptionalClassAvailable =
			ClassUtils.isPresent("java.util.Optional", DefaultConversionService.class.getClassLoader());

	/** Java 8's java.time package available? */
	private static final boolean jsr310Available =
			ClassUtils.isPresent("java.time.ZoneId", DefaultConversionService.class.getClassLoader());

	/** Java 8's java.util.stream.Stream class available? */
	private static final boolean streamAvailable = ClassUtils.isPresent(
			"java.util.stream.Stream", DefaultConversionService.class.getClassLoader());


	/**
	 * Create a new {@code DefaultConversionService} with the set of
	 * {@linkplain DefaultConversionService#addDefaultConverters(ConverterRegistry) default converters}.
	 */
	public DefaultConversionService() {
		addDefaultConverters(this);
	}


	// static utility methods

	/**
	 * Add converters appropriate for most environments.
	 * @param converterRegistry the registry of converters to add to (must also be castable to ConversionService,
	 * e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given ConverterRegistry could not be cast to a ConversionService
	 */
	public static void addDefaultConverters(ConverterRegistry converterRegistry) {
		addScalarConverters(converterRegistry);
		addCollectionConverters(converterRegistry);

		converterRegistry.addConverter(new ByteBufferConverter((ConversionService) converterRegistry));
		if (jsr310Available) {
			Jsr310ConverterRegistrar.registerJsr310Converters(converterRegistry);
		}

		converterRegistry.addConverter(new ObjectToObjectConverter());
		converterRegistry.addConverter(new IdToEntityConverter((ConversionService) converterRegistry));
		converterRegistry.addConverter(new FallbackObjectToStringConverter());
		if (javaUtilOptionalClassAvailable) {
			converterRegistry.addConverter(new ObjectToOptionalConverter((ConversionService) converterRegistry));
		}
	}

	/**
	 * Add collection converters.
	 * @param converterRegistry the registry of converters to add to (must also be castable to ConversionService,
	 * e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given ConverterRegistry could not be cast to a ConversionService
	 * @since 4.2.3
	 */
	public static void addCollectionConverters(ConverterRegistry converterRegistry) {
		ConversionService conversionService = (ConversionService) converterRegistry;

		converterRegistry.addConverter(new ArrayToCollectionConverter(conversionService));
		converterRegistry.addConverter(new CollectionToArrayConverter(conversionService));

		converterRegistry.addConverter(new ArrayToArrayConverter(conversionService));
		converterRegistry.addConverter(new CollectionToCollectionConverter(conversionService));
		converterRegistry.addConverter(new MapToMapConverter(conversionService));

		converterRegistry.addConverter(new ArrayToStringConverter(conversionService));
		converterRegistry.addConverter(new StringToArrayConverter(conversionService));

		converterRegistry.addConverter(new ArrayToObjectConverter(conversionService));
		converterRegistry.addConverter(new ObjectToArrayConverter(conversionService));

		converterRegistry.addConverter(new CollectionToStringConverter(conversionService));
		converterRegistry.addConverter(new StringToCollectionConverter(conversionService));

		converterRegistry.addConverter(new CollectionToObjectConverter(conversionService));
		converterRegistry.addConverter(new ObjectToCollectionConverter(conversionService));

		if (streamAvailable) {
			converterRegistry.addConverter(new StreamConverter(conversionService));
		}
	}


	// internal helpers

	private static void addScalarConverters(ConverterRegistry converterRegistry) {
		converterRegistry.addConverterFactory(new NumberToNumberConverterFactory());

		converterRegistry.addConverterFactory(new StringToNumberConverterFactory());
		converterRegistry.addConverter(Number.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToCharacterConverter());
		converterRegistry.addConverter(Character.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new NumberToCharacterConverter());
		converterRegistry.addConverterFactory(new CharacterToNumberFactory());

		converterRegistry.addConverter(new StringToBooleanConverter());
		converterRegistry.addConverter(Boolean.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverterFactory(new StringToEnumConverterFactory());
		converterRegistry.addConverter(Enum.class, String.class,
				new EnumToStringConverter((ConversionService) converterRegistry));

		converterRegistry.addConverter(new StringToLocaleConverter());
		converterRegistry.addConverter(Locale.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToCharsetConverter());
		converterRegistry.addConverter(Charset.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToCurrencyConverter());
		converterRegistry.addConverter(Currency.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToPropertiesConverter());
		converterRegistry.addConverter(new PropertiesToStringConverter());

		converterRegistry.addConverter(new StringToUUIDConverter());
		converterRegistry.addConverter(UUID.class, String.class, new ObjectToStringConverter());
	}


	/**
	 * Inner class to avoid a hard-coded dependency on Java 8's {@code java.time} package.
	 */
	private static final class Jsr310ConverterRegistrar {

		public static void registerJsr310Converters(ConverterRegistry converterRegistry) {
			converterRegistry.addConverter(new StringToTimeZoneConverter());
			converterRegistry.addConverter(new ZoneIdToTimeZoneConverter());
			converterRegistry.addConverter(new ZonedDateTimeToCalendarConverter());
		}
	}

}
