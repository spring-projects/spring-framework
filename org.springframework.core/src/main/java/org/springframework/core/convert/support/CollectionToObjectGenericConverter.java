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

import java.util.Collection;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;

final class CollectionToObjectGenericConverter implements GenericConverter {

	private final GenericConversionService conversionService;

	public CollectionToObjectGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		Collection sourceCollection = (Collection) source;
		if (sourceCollection.size() == 0) {
			return null;
		} else {
			Object firstElement = sourceCollection.iterator().next();			
			TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
			if (sourceElementType == TypeDescriptor.NULL && firstElement != null) {
				sourceElementType = TypeDescriptor.valueOf(firstElement.getClass());
			}			
			if (sourceElementType == TypeDescriptor.NULL || sourceElementType.isAssignableTo(targetType)) {
				return firstElement;
			} else {
				GenericConverter converter = this.conversionService.getConverter(sourceElementType, targetType);
				if (converter == null) {
					throw new ConverterNotFoundException(sourceElementType, targetType);
				}				
				return invokeConverter(converter, firstElement, sourceElementType, targetType);
			}
		}
	}
}
