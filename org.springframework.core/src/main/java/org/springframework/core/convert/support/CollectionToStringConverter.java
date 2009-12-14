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
 * Converts a Collection to a comma-delimited String.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class CollectionToStringConverter implements ConditionalGenericConverter {

	private static final String DELIMITER = ",";

	private final GenericConversionService conversionService;

	public CollectionToStringConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, String.class));
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
			return "";
		}
		else {
			TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
			if (sourceElementType == TypeDescriptor.NULL) {
				sourceElementType = ConversionUtils.getElementType(sourceCollection);
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
			}
			else {
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
					Object targetElement = ConversionUtils.invokeConverter(
							converter, sourceElement, sourceElementType, targetType);
					string.append(targetElement);
					i++;
				}
				return string.toString();
			}
		}
	}

}
