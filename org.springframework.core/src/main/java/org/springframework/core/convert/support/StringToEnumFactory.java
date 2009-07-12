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
package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterInfo;

/**
 * A factory for String to enum converters.
 * @author Keith Donald
 * @since 3.0
 */
@SuppressWarnings("unchecked")
public class StringToEnumFactory implements ConverterFactory<String, Enum> {

	public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
		return new StringToEnum(targetType);
	}

	class StringToEnum<T extends Enum> implements Converter<String, T>, ConverterInfo {

		private Class<T> enumType;
		
		public StringToEnum(Class<T> enumType) {
			this.enumType = enumType;
		}

		public Class<String> getSourceType() {
			return String.class;
		}

		public Class<T> getTargetType() {
			return enumType;
		}

		public T convert(String source) throws Exception {
			return (T) Enum.valueOf(enumType, source);
		}
	}

}
