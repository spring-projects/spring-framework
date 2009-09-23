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

import java.lang.reflect.Array;
import java.util.Map;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;

final class StringArrayToMapGenericConverter implements GenericConverter {

	private final GenericConversionService conversionService;

	public StringArrayToMapGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		TypeDescriptor targetKeyType = targetType.getMapKeyTypeDescriptor();
		TypeDescriptor targetValueType = targetType.getMapValueTypeDescriptor();
		if (targetKeyType == TypeDescriptor.NULL && targetValueType == TypeDescriptor.NULL) {
			return mapWithoutConversion(source, targetType);
		}
		TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
		boolean keysCompatible = false;
		if (sourceElementType.isAssignableTo(targetKeyType)) {
			keysCompatible = true;
		}
		boolean valuesCompatible = false;
		if (sourceElementType.isAssignableTo(targetValueType)) {
			valuesCompatible = true;
		}
		if (keysCompatible && valuesCompatible) {
			return mapWithoutConversion(source, targetType);
		}
		int length = Array.getLength(source);
		Map target = CollectionFactory.createMap(targetType.getType(), length);
		MapEntryConverter converter = new MapEntryConverter(sourceElementType, sourceElementType, targetKeyType, targetValueType, keysCompatible, valuesCompatible, conversionService);		
		for (int i = 0; i < length; i++) {
			String property = (String) Array.get(source, i);
			String[] fields = property.split("=");
			if (fields.length < 2) {
				throw new IllegalArgumentException("Invalid String property '" + property
						+ "'; properties should be in the format name=value");
			}
			Object targetKey = converter.convertKey(fields[0]);
			Object targetValue = converter.convertValue(fields[1]);
			target.put(targetKey, targetValue);
		}
		return target;
	}

	private Map mapWithoutConversion(Object source, TypeDescriptor targetType) {
		int length = Array.getLength(source);
		Map target = CollectionFactory.createMap(targetType.getType(), length);
		for (int i = 0; i < length; i++) {
			String property = (String) Array.get(source, i);
			String[] fields = property.split("=");
			if (fields.length < 2) {
				throw new IllegalArgumentException("Invalid String property '" + property
						+ "'; properties should be in the format name=value");
			}
			String key = fields[0];
			String value = fields[1];
			target.put(key, value);
		}
		return target;
	}

}
