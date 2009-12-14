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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * Converts a Collection to an Object by returning the first collection element after converting it to the desired targetType.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class CollectionToObjectConverter implements ConditionalGenericConverter {

	private final GenericConversionService conversionService;

	public CollectionToObjectConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, Object.class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.canConvert(sourceType.getElementTypeDescriptor(), targetType);
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}
		Collection<?> sourceCollection = (Collection<?>) source;
		if (sourceCollection.size() == 0) {
			return null;
		}
		else {
			Object firstElement = sourceCollection.iterator().next();
			TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
			if (sourceElementType == TypeDescriptor.NULL && firstElement != null) {
				sourceElementType = TypeDescriptor.valueOf(firstElement.getClass());
			}
			if (sourceElementType == TypeDescriptor.NULL || sourceElementType.isAssignableTo(targetType)) {
				return firstElement;
			}
			else {
				GenericConverter converter = this.conversionService.getConverter(sourceElementType, targetType);
				if (converter == null) {
					throw new ConverterNotFoundException(sourceElementType, targetType);
				}
				return ConversionUtils.invokeConverter(converter, firstElement, sourceElementType, targetType);
			}
		}
	}

}
