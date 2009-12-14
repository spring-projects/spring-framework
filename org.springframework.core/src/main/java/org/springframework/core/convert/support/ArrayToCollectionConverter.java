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
import java.util.Collections;
import java.util.Set;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * Converts an Array to a Collection.
 *
 * <p>First, creates a new Collection of the requested targetType.
 * Then adds each array element to the target collection.
 * Will perform an element conversion from the source component type
 * to the collection's parameterized type if necessary.
 * 
 * @author Keith Donald
 * @since 3.0
 */
final class ArrayToCollectionConverter implements ConditionalGenericConverter {

	private final GenericConversionService conversionService;

	public ArrayToCollectionConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object[].class, Collection.class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.canConvert(sourceType.getElementTypeDescriptor(), targetType.getElementTypeDescriptor());
	}

	@SuppressWarnings("unchecked")
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}		
		int length = Array.getLength(source);
		Collection collection = CollectionFactory.createCollection(targetType.getType(), length);
		TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		if (targetElementType == TypeDescriptor.NULL || sourceElementType.isAssignableTo(targetElementType)) {
			for (int i = 0; i < length; i++) {
				collection.add(Array.get(source, i));
			}
		}
		else {
			GenericConverter converter = this.conversionService.getConverter(sourceElementType, targetElementType);
			if (converter == null) {
				throw new ConverterNotFoundException(sourceElementType, targetElementType);
			}
			for (int i = 0; i < length; i++) {
				Object sourceElement = Array.get(source, i);
				Object targetElement = ConversionUtils.invokeConverter(
						converter, sourceElement, sourceElementType, targetElementType);
				collection.add(targetElement);
			}
		}
		return collection;
	}

}
