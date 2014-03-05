/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

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

	private final ConversionService conversionService;

	public CollectionToCollectionConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, Collection.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(
				sourceType.getElementTypeDescriptor(), targetType.getElementTypeDescriptor(), this.conversionService);
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		boolean copyRequired = !targetType.getType().isInstance(source);
		Collection<?> sourceCollection = (Collection<?>) source;
		if (!copyRequired && sourceCollection.isEmpty()) {
			return sourceCollection;
		}
		Collection<Object> target = CollectionFactory.createCollection(targetType.getType(), sourceCollection.size());
		if (targetType.getElementTypeDescriptor() == null) {
			if (copyRequired) { // create a copy only if necessary
				for (Object element : sourceCollection) {
					target.add(element);
				}
				return target;
			} else {
				return source;
			}
		}
		else {
			for (Object sourceElement : sourceCollection) {
				Object targetElement = this.conversionService.convert(sourceElement,
						sourceType.elementTypeDescriptor(sourceElement), targetType.getElementTypeDescriptor());
				target.add(targetElement);
				if (sourceElement != targetElement) {
					copyRequired = true;
				}
			}
			return (copyRequired ? target : source);
		}
	}

}
