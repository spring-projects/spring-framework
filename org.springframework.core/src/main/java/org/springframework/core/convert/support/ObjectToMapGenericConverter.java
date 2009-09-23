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

import java.util.Map;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;

final class ObjectToMapGenericConverter implements GenericConverter {

	private final GenericConversionService conversionService;
	
	private final ArrayToMapGenericConverter helperConverter;

	public ObjectToMapGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
		this.helperConverter = new ArrayToMapGenericConverter(conversionService);
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.typeEquals(String.class)) {
			String string = (String) source;
			String[] properties = string.split(" ");
			return this.helperConverter.convert(properties, TypeDescriptor.valueOf(String[].class), targetType);
		} else {
			Map target = CollectionFactory.createMap(targetType.getType(), 1);
			TypeDescriptor targetKeyType = targetType.getMapKeyTypeDescriptor();
			TypeDescriptor targetValueType = targetType.getMapValueTypeDescriptor();
			boolean keysCompatible = false;
			if (targetKeyType == TypeDescriptor.NULL || sourceType.isAssignableTo(targetKeyType)) {
				keysCompatible = true;
			}
			boolean valuesCompatible = false;
			if (targetValueType == TypeDescriptor.NULL || sourceType.isAssignableTo(targetValueType)) {
				valuesCompatible = true;
			}
			if (keysCompatible && valuesCompatible) {
				target.put(source, source);
			} else {
				MapEntryConverter converter = new MapEntryConverter(sourceType, sourceType, targetKeyType,
						targetValueType, keysCompatible, valuesCompatible, conversionService);
				Object key = converter.convertKey(source);
				Object value = converter.convertValue(source);
				target.put(key, value);
			}
			return target;
		}
	}

}