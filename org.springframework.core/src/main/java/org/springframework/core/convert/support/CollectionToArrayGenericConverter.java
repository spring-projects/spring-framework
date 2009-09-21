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
import java.util.Collection;
import java.util.Iterator;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;

final class CollectionToArrayGenericConverter implements GenericConverter {

	private final GenericConversionService conversionService;

	public CollectionToArrayGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}
	
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		Collection sourceCollection = (Collection) source;
		TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
		if (sourceElementType == TypeDescriptor.NULL) {
			sourceElementType = getElementType(sourceCollection);
		}
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		Object array = Array.newInstance(targetElementType.getType(), sourceCollection.size());
		int i = 0;		
		if (sourceElementType == TypeDescriptor.NULL || sourceElementType.isAssignableTo(targetElementType)) {
			for (Iterator it = sourceCollection.iterator(); it.hasNext(); i++) {
				Array.set(array, i, it.next());
			}
		} else {
			GenericConverter converter = this.conversionService.getConverter(sourceElementType, targetElementType);
			if (converter == null) {
				throw new ConverterNotFoundException(sourceType, targetType);
			}			
			for (Iterator it = sourceCollection.iterator(); it.hasNext(); i++) {
				Array.set(array, i, converter.convert(it.next(), sourceElementType, targetElementType));
			}
		}
		return array;
	}
	
	private TypeDescriptor getElementType(Collection collection) {
		for (Object element : collection) {
			if (element != null) {
				return TypeDescriptor.valueOf(element.getClass());
			}
		}
		return TypeDescriptor.NULL;
	}

}
