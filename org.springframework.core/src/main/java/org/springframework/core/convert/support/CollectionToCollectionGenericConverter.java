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
class CollectionToCollectionGenericConverter implements GenericConverter {

	private GenericConversionService conversionService;

	public CollectionToCollectionGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Object convert(Object source, TypeDescriptor targetType) {
		Collection sourceCollection = (Collection) source;
		Class targetElementType = targetType.getElementType();
		if (targetElementType == null) {
			return compatibleCollectionWithoutElementConversion(sourceCollection, targetType);
		}
		Class sourceElementType = getElementType(sourceCollection);
		if (sourceElementType == null || targetElementType.isAssignableFrom(sourceElementType)) {
			return compatibleCollectionWithoutElementConversion(sourceCollection, targetType);
		}
		Collection targetCollection = CollectionFactory.createCollection(targetType.getType(), sourceCollection.size());
		TypeDescriptor targetElementTypeDescriptor = TypeDescriptor.valueOf(targetElementType);
		GenericConverter elementConverter = conversionService.getConverter(sourceElementType,
				targetElementTypeDescriptor);
		for (Object element : sourceCollection) {
			targetCollection.add(elementConverter.convert(element, targetElementTypeDescriptor));
		}
		return targetCollection;
	}

	private Class getElementType(Collection collection) {
		for (Object element : collection) {
			if (element != null) {
				return element.getClass();
			}
		}
		return null;
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
