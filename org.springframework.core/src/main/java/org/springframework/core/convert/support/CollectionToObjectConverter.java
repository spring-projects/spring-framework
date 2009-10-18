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

import static org.springframework.core.convert.support.ConversionUtils.getElementType;
import static org.springframework.core.convert.support.ConversionUtils.invokeConverter;

import java.util.Collection;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Converts from a Collection to a single Object.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class CollectionToObjectConverter implements GenericConverter {

	private static final String DELIMITER = ",";

	private final GenericConversionService conversionService;

	public CollectionToObjectConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}
		Collection sourceCollection = (Collection) source;
		if (sourceCollection.size() == 0) {
			if (targetType.typeEquals(String.class)) {
				return "";
			} else {
				return null;
			}
		} else {
			if (targetType.typeEquals(String.class)) {
				TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
				if (sourceElementType == TypeDescriptor.NULL) {
					sourceElementType = getElementType(sourceCollection);
				}
				if (sourceElementType == TypeDescriptor.NULL || sourceElementType.isAssignableTo(targetType)) {
					StringBuilder string = new StringBuilder();
					int i = 0;
					for (Object element : sourceCollection) {
						if (i > 0) {
							string.append(DELIMITER);
						}
						string.append(element);
						i++;
					}
					return string.toString();
				} else {
					GenericConverter converter = this.conversionService.getConverter(sourceElementType, targetType);
					if (converter == null) {
						throw new ConverterNotFoundException(sourceElementType, targetType);
					}
					StringBuilder string = new StringBuilder();
					int i = 0;
					for (Object sourceElement : sourceCollection) {
						if (i > 0) {
							string.append(DELIMITER);
						}
						Object targetElement = invokeConverter(converter, sourceElement, sourceElementType, targetType);
						string.append(targetElement);
						i++;
					}
					return string.toString();
				}
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

}
