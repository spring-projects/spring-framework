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
 * Converts an Object to a single-element Collection containing the Object.
 * Will convert the Object to the target Collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class ObjectToCollectionConverter implements ConditionalGenericConverter {

	private final GenericConversionService conversionService;

	public ObjectToCollectionConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Collection.class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.canConvert(sourceType, targetType.getElementTypeDescriptor());
	}

	@SuppressWarnings("unchecked")
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}
		Collection target = CollectionFactory.createCollection(targetType.getType(), 1);
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		if (targetElementType == TypeDescriptor.NULL || sourceType.isAssignableTo(targetElementType)) {
			target.add(source);
		}
		else {
			GenericConverter converter = this.conversionService.getConverter(sourceType, targetElementType);
			if (converter == null) {
				throw new ConverterNotFoundException(sourceType, targetElementType);
			}
			target.add(ConversionUtils.invokeConverter(converter, source, sourceType, targetElementType));
		}
		return target;
	}

}
