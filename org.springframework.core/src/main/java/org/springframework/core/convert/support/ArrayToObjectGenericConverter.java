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

import java.lang.reflect.Array;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;

class ArrayToObjectGenericConverter implements GenericConverter {

	private GenericConversionService conversionService;

	public ArrayToObjectGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}
	
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		int length = Array.getLength(source);
		if (length == 0) {
			return null;
		} else {
			TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
			if (sourceElementType.isAssignableTo(targetType)) {
				return Array.get(source, 0);
			} else {
				GenericConverter converter = conversionService.getConverter(sourceElementType, targetType);
				if (converter == null) {
					throw new ConverterNotFoundException(sourceType, targetType);
				}
				return converter.convert(Array.get(source, 0), sourceElementType, targetType);
			}
		}
	}

}
