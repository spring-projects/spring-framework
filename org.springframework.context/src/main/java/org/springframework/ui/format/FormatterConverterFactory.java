/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.ui.format;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * A factory for adapting formatting and parsing logic in a {@link Formatter} to the {@link Converter} contract.
 * @author Keith Donald
 */
public class FormatterConverterFactory {

	/**
	 * Register converter adapters for the formatter.
	 * An adapter will be registered for formatting to String as well as parsing from String.
	 * @param <T> The type of formatter
	 * @param formatter the formatter
	 * @param registry the converter registry
	 */
	public static <T> void add(Formatter<T> formatter,
			ConverterRegistry registry) {
		registry.add(new FormattingConverter<T>(formatter));
		registry.add(new ParsingConverter<T>(formatter));
	}

	/**
	 * Remove the Formatter/converter adapters previously registered for the formatted type.
	 * @param <T> the formatted type
	 * @param formattedType the formatted type
	 * @param registry the converter registry
	 */
	public static <T> void remove(Class<T> formattedType,
			ConverterRegistry registry) {
		registry.removeConverter(formattedType, String.class);
		registry.removeConverter(String.class, formattedType);
	}

}
