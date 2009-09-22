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

import static org.springframework.core.convert.support.ConversionUtils.invokeConverter;

import java.lang.reflect.Array;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.StringUtils;

final class StringToArrayGenericConverter implements GenericConverter {

	private final GenericConversionService conversionService;

	public StringToArrayGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		String string = (String) source;
		String[] fields = StringUtils.commaDelimitedListToStringArray(string);
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		if (sourceType.isAssignableTo(targetElementType)) {
			return fields;
		} else {
			Object target = Array.newInstance(targetElementType.getType(), fields.length);
			GenericConverter converter = this.conversionService.getConverter(sourceType, targetElementType);
			if (converter == null) {
				throw new ConverterNotFoundException(sourceType, targetElementType);
			}
			for (int i = 0; i < fields.length; i++) {
				Array.set(target, i, invokeConverter(converter, fields[i], sourceType, targetElementType));
			}
			return target;
		}
	}

}