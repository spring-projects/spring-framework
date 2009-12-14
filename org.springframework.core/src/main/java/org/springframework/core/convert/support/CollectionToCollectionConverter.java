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

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * Converts from a Collection to another Collection.
 *
 * <p>First, creates a new Collection of the requested targetType with a size equal to the
 * size of the source Collection. Then copies each element in the source collection to the
 * target collection. Will perform an element conversion from the source collection's
 * parameterized type to the target collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class CollectionToCollectionConverter implements ConditionalGenericConverter {

	private final GenericConversionService conversionService;

	public CollectionToCollectionConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, Collection.class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.canConvert(sourceType.getElementTypeDescriptor(), targetType.getElementTypeDescriptor());
	}
	
	@SuppressWarnings("unchecked")
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
		if (sourceElementType == TypeDescriptor.NULL || targetElementType == TypeDescriptor.NULL
				|| sourceElementType.isAssignableTo(targetElementType)) {
			if (sourceType.isAssignableTo(targetType)) {
				return sourceCollection;
			}
			else {
				Collection target = CollectionFactory.createCollection(targetType.getType(), sourceCollection.size());
				target.addAll(sourceCollection);
				return target;
			}
		}
		Collection target = CollectionFactory.createCollection(targetType.getType(), sourceCollection.size());
		GenericConverter converter = this.conversionService.getConverter(sourceElementType, targetElementType);
		if (converter == null) {
			throw new ConverterNotFoundException(sourceElementType, targetElementType);
		}
		for (Object element : sourceCollection) {
			target.add(ConversionUtils.invokeConverter(converter, element, sourceElementType, targetElementType));
		}
		return target;
	}

}
