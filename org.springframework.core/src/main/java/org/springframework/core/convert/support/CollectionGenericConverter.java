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

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;

/**
 * A generic converter that can convert from one collection type to another.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
class CollectionGenericConverter implements GenericConverter {

	private GenericConversionService conversionService;

	public CollectionGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}
	
	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return sourceType.isCollection() && targetType.isCollection();
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		Collection sourceCollection = (Collection) source;
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		if (targetElementType == TypeDescriptor.NULL) {
			return compatibleCollectionWithoutElementConversion(sourceCollection, targetType);
		}
		TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
		if (sourceElementType == TypeDescriptor.NULL) {
			sourceElementType = getElementType(sourceCollection);
		}
		if (sourceElementType == TypeDescriptor.NULL || sourceElementType.isAssignableTo(targetElementType)) {
			return compatibleCollectionWithoutElementConversion(sourceCollection, targetType);
		}
		Collection targetCollection = CollectionFactory.createCollection(targetType.getType(), sourceCollection.size());
		GenericConverter elementConverter = conversionService.getConverter(sourceElementType, targetElementType);
		for (Object element : sourceCollection) {
			targetCollection.add(elementConverter.convert(element, sourceElementType, targetElementType));
		}
		return targetCollection;
	}

	private TypeDescriptor getElementType(Collection collection) {
		for (Object element : collection) {
			if (element != null) {
				return TypeDescriptor.valueOf(element.getClass());
			}
		}
		return TypeDescriptor.NULL;
	}

	private Collection compatibleCollectionWithoutElementConversion(Collection source, TypeDescriptor targetType) {
		if (targetType.getType().isAssignableFrom(source.getClass())) {
			return source;
		} else {
			Collection target = CollectionFactory.createCollection(targetType.getType(), source.size());
			target.addAll(source);
			return target;
		}
	}

}
