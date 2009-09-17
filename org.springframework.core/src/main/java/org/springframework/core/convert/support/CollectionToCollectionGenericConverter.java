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
		Object firstNotNullElement = getFirstNotNullElement(sourceCollection);
		if (firstNotNullElement == null) {
			return compatibleCollectionWithoutElementConversion(sourceCollection, targetType);
		}
		Class targetElementType = targetType.getElementType(); 
		if (targetElementType == null || targetElementType.isAssignableFrom(firstNotNullElement.getClass())) {
			return compatibleCollectionWithoutElementConversion(sourceCollection, targetType);
		}
		Collection targetCollection = CollectionFactory.createCollection(targetType.getType(), sourceCollection.size());
		GenericConverter elementConverter = conversionService.getConverter(firstNotNullElement.getClass(), targetElementType);
		for (Object element : sourceCollection) {
			targetCollection.add(elementConverter.convert(element, TypeDescriptor.valueOf(targetElementType)));
		}
		return targetCollection;
	}

	private Collection compatibleCollectionWithoutElementConversion(Collection source, TypeDescriptor targetType) {
		if (targetType.getType().isAssignableFrom(source.getClass())) {
			return source;
		} else {
			Collection target = CollectionFactory.createCollection(targetType.getType(), source.size());
			for (Object element : source) {
				target.addAll(source);
			}
			return target;
		}
	}
	
	private Object getFirstNotNullElement(Collection collection) {
		for (Object element : collection) {
			if (element != null) {
				return element;
			}
		}
		return null;
	}
	
}
