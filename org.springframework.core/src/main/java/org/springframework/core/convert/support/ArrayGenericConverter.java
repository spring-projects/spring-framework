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

import org.springframework.core.convert.TypeDescriptor;

/**
 * A generic converter that can convert from one array type to another.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
class ArrayGenericConverter implements GenericConverter {

	private GenericConversionService conversionService;

	public ArrayGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return sourceType.isArray() && targetType.isArray();		
	}
	
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.isAssignableTo(targetType)) {
			return source;
		}
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
		Object target = Array.newInstance(targetElementType.getType(), Array.getLength(source));
		GenericConverter converter = conversionService.getConverter(sourceElementType, targetElementType);
		for (int i = 0; i < Array.getLength(target); i++) {
			Array.set(target, i, converter.convert(Array.get(source, i), sourceElementType, targetElementType));
		}
		return target;
	}

}