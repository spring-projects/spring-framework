/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.core.convert.ConversionService;

/**
 * A factory for creating common ConversionService configurations.
 * @author Keith Donald
 * @since 3.0
 */
public final class ConversionServiceFactory {

	private ConversionServiceFactory() {

	}

	/**
	 * Create a new default ConversionService prototype that can be safely modified.
	 */
	public static ConversionService createDefaultConversionService() {
		GenericConversionService conversionService = new GenericConversionService();
		conversionService.addGenericConverter(new ArrayToArrayConverter(conversionService));
		conversionService.addGenericConverter(new ArrayToCollectionConverter(conversionService));
		conversionService.addGenericConverter(new ArrayToMapConverter(conversionService));
		conversionService.addGenericConverter(new ArrayToObjectConverter(conversionService));
		conversionService.addGenericConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addGenericConverter(new CollectionToArrayConverter(conversionService));
		conversionService.addGenericConverter(new CollectionToMapConverter(conversionService));
		conversionService.addGenericConverter(new CollectionToObjectConverter(conversionService));
		conversionService.addGenericConverter(new MapToMapConverter(conversionService));
		conversionService.addGenericConverter(new MapToArrayConverter(conversionService));
		conversionService.addGenericConverter(new MapToCollectionConverter(conversionService));
		conversionService.addGenericConverter(new MapToObjectConverter(conversionService));
		conversionService.addGenericConverter(new ObjectToArrayConverter(conversionService));
		conversionService.addGenericConverter(new ObjectToCollectionConverter(conversionService));
		conversionService.addGenericConverter(new ObjectToMapConverter(conversionService));
		conversionService.addConverter(new StringToBooleanConverter());
		conversionService.addConverter(new StringToCharacterConverter());
		conversionService.addConverter(new StringToLocaleConverter());
		conversionService.addConverter(new NumberToCharacterConverter());
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		conversionService.addConverterFactory(new StringToEnumConverterFactory());
		conversionService.addConverterFactory(new NumberToNumberConverterFactory());
		conversionService.addConverterFactory(new CharacterToNumberFactory());
		conversionService.addConverter(new ObjectToStringConverter());
		conversionService.addGenericConverter(new ObjectToObjectGenericConverter());
		conversionService.addGenericConverter(new EntityConverter(conversionService));
		return conversionService;
	}
}
