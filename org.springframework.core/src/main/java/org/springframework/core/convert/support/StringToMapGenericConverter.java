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

import org.springframework.core.convert.TypeDescriptor;

final class StringToMapGenericConverter implements GenericConverter {

	private final StringArrayToMapGenericConverter converter;
	
	public StringToMapGenericConverter(GenericConversionService conversionService) {
		this.converter = new StringArrayToMapGenericConverter(conversionService);
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		String string = (String) source;
		String[] properties = string.split(" ");
		return this.converter.convert(properties, TypeDescriptor.valueOf(String[].class), targetType);
	}

}