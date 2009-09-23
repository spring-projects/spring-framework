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

import org.springframework.core.convert.TypeDescriptor;

final class MapToStringArrayGenericConverter implements GenericConverter {

	private final GenericConversionService conversionService;

	public MapToStringArrayGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		Map sourceMap = (Map) source;
		TypeDescriptor sourceKeyType = sourceType.getMapKeyTypeDescriptor();
		TypeDescriptor sourceValueType = sourceType.getMapValueTypeDescriptor();		
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		boolean keysCompatible = false;
		if (sourceKeyType.isAssignableTo(targetElementType)) {
			keysCompatible = true;
		}
		boolean valuesCompatible = false;
		if (sourceValueType.isAssignableTo(targetElementType)) {
			valuesCompatible = true;
		}		
		Object array = Array.newInstance(targetElementType.getType(), sourceMap.size());
		MapEntryConverter converter = new MapEntryConverter(sourceKeyType, sourceValueType, targetElementType, targetElementType, keysCompatible, valuesCompatible, conversionService);		
		int i = 0;
		for (Object entry : sourceMap.entrySet()) {
			Map.Entry mapEntry = (Map.Entry) entry;
			Object key = mapEntry.getKey();
			Object value = mapEntry.getValue();
			String property = converter.convertKey(key) + "=" + converter.convertValue(value);
			Array.set(array, i, property);
			i++;
		}
		return array;		
	}

}
