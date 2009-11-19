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

import static org.springframework.core.convert.support.ConversionUtils.invokeConverter;

import java.util.Collection;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.StringUtils;

/**
 * Converts from a single Object to a Collection.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class ObjectToCollectionConverter implements GenericConverter {

	private final GenericConversionService conversionService;

	public ObjectToCollectionConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Class<?>[][] getConvertibleTypes() {
		return new Class<?>[][] { { Object.class, Collection.class } };
	}

	@SuppressWarnings("unchecked")
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {		
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		if (sourceType.typeEquals(String.class)) {
			String string = (String) source;
			String[] fields = StringUtils.commaDelimitedListToStringArray(string);
			Collection target = CollectionFactory.createCollection(targetType.getType(), fields.length);
			if (targetElementType == TypeDescriptor.NULL || sourceType.isAssignableTo(targetElementType)) {
				for (int i = 0; i < fields.length; i++) {
					target.add(fields[i]);				
				}
			}
			else {
				GenericConverter converter = this.conversionService.getConverter(sourceType, targetElementType);
				if (converter == null) {
					throw new ConverterNotFoundException(sourceType, targetElementType);
				}
				for (int i = 0; i < fields.length; i++) {
					String sourceElement = fields[i];
					Object targetElement = invokeConverter(converter, sourceElement, sourceType, targetElementType);
					target.add(targetElement);				
				}
			}
			return target;

		}
		else {
			Collection target = CollectionFactory.createCollection(targetType.getType(), 1);			
			if (targetElementType == TypeDescriptor.NULL || sourceType.isAssignableTo(targetElementType)) {
				target.add(source);
			}
			else {
				GenericConverter converter = this.conversionService.getConverter(sourceType, targetElementType);
				if (converter == null) {
					throw new ConverterNotFoundException(sourceType, targetElementType);
				}
				target.add(invokeConverter(converter, source, sourceType, targetElementType));
			}
			return target;
		}
	}

}
