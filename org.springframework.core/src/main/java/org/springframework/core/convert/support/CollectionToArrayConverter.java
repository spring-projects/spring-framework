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
import java.util.Iterator;
import java.util.Set;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * Converts a Collection to an array.
 *
 * <p>First, creates a new array of the requested targetType with a length equal to the
 * size of the source Collection. Then sets each collection element into the array.
 * Will perform an element conversion from the collection's parameterized type to the
 * array's component type if necessary.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class CollectionToArrayConverter implements ConditionalGenericConverter {

	private final GenericConversionService conversionService;

	public CollectionToArrayConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, Object[].class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.canConvert(sourceType.getElementTypeDescriptor(), targetType.getElementTypeDescriptor());
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}		
		Collection<?> sourceCollection = (Collection<?>) source;
		TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
		if (sourceElementType == TypeDescriptor.NULL) {
			sourceElementType = ConversionUtils.getElementType(sourceCollection);
		}
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		Object array = Array.newInstance(targetElementType.getType(), sourceCollection.size());
		int i = 0;
		if (sourceElementType == TypeDescriptor.NULL || sourceElementType.isAssignableTo(targetElementType)) {
			for (Iterator<?> it = sourceCollection.iterator(); it.hasNext(); i++) {
				Array.set(array, i, it.next());
			}
		}
		else {
			GenericConverter converter = this.conversionService.getConverter(sourceElementType, targetElementType);
			if (converter == null) {
				throw new ConverterNotFoundException(sourceElementType, targetElementType);
			}
			for (Iterator<?> it = sourceCollection.iterator(); it.hasNext(); i++) {
				Object sourceElement = it.next();
				Object targetElement = ConversionUtils.invokeConverter(
						converter, sourceElement, sourceElementType, targetElementType);
				Array.set(array, i, targetElement);
			}
		}
		return array;
	}

}
