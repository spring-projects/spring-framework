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


/**
 * Default implementation of a conversion service. Will automatically register <i>from string</i>
 * converters for a number of standard Java types like Class, Number, Boolean and so on.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public class DefaultConversionService extends GenericConversionService {

	/**
	 * Create a new default conversion service, installing the default converters.
	 */
	public DefaultConversionService() {
		initDefaultConverters();
	}
	
	// subclassing hooks
	
	protected void initDefaultConverters() {
		addConverter(new StringToBooleanConverter());
		addConverter(new StringToCharacterConverter());
		addConverter(new StringToLocaleConverter());
		addConverter(new NumberToCharacterConverter());
		addConverter(new ObjectToStringConverter());
		addConverterFactory(new StringToNumberConverterFactory());
		addConverterFactory(new StringToEnumConverterFactory());
		addConverterFactory(new NumberToNumberConverterFactory());
		addConverterFactory(new CharacterToNumberFactory());
	}

}
