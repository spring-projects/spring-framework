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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Converts an Array to a Collection.
 *
 * <p>First, creates a new Collection of the requested targetType.
 * Then adds each array element to the target collection.
 * Will perform an element conversion from the source component type to the collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class ArrayToCollectionConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;

	public ArrayToCollectionConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object[].class, Collection.class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(
				sourceType.getElementTypeDescriptor(), targetType.getElementTypeDescriptor(), this.conversionService);
	}

	@SuppressWarnings("unchecked")
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		int length = Array.getLength(source);
		Collection<Object> target = CollectionFactory.createCollection(targetType.getType(), length);
		if (targetType.getElementTypeDescriptor() == null) {
			for (int i = 0; i < length; i++) {
				Object sourceElement = Array.get(source, i);
				target.add(sourceElement);
			}
		}
		else {
			for (int i = 0; i < length; i++) {
				Object sourceElement = Array.get(source, i);
				Object targetElement = this.conversionService.convert(sourceElement,
						sourceType.elementTypeDescriptor(sourceElement), targetType.getElementTypeDescriptor());
				target.add(targetElement);
			}
		}
		return target;
	}

}
