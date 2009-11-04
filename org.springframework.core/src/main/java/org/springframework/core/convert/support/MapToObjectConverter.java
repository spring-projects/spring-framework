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

import org.springframework.core.convert.TypeDescriptor;

/**
 * Converts from a Ma to a single Object.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class MapToObjectConverter implements GenericConverter {

	private static final String DELIMITER = " ";

	private final GenericConversionService conversionService;

	public MapToObjectConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}		
		Map<?, ?> sourceMap = (Map<?, ?>) source;
		if (sourceMap.size() == 0) {
			if (targetType.typeEquals(String.class)) {
				return "";
			} else {
				return null;
			}
		} else {
			if (targetType.typeEquals(String.class)) {
				TypeDescriptor sourceKeyType = sourceType.getMapKeyTypeDescriptor();
				TypeDescriptor sourceValueType = sourceType.getMapValueTypeDescriptor();
				boolean keysCompatible = false;
				if (sourceKeyType == TypeDescriptor.NULL || sourceKeyType.isAssignableTo(targetType)) {
					keysCompatible = true;
				}
				boolean valuesCompatible = false;
				if (sourceValueType == TypeDescriptor.NULL || sourceValueType.isAssignableTo(targetType)) {
					valuesCompatible = true;
				}
				if (keysCompatible && valuesCompatible) {
					StringBuilder string = new StringBuilder();
					int i = 0;
					for (Object entry : sourceMap.entrySet()) {
						Map.Entry<?, ?> mapEntry = (Map.Entry<?, ?>) entry;
						if (i > 0) {
							string.append(DELIMITER);
						}
						String property = mapEntry.getKey() + "=" + mapEntry.getValue();
						string.append(property);
						i++;
					}
					return string.toString();
				} else {
					MapEntryConverter converter = new MapEntryConverter(sourceKeyType, sourceValueType, targetType,
							targetType, keysCompatible, valuesCompatible, this.conversionService);
					StringBuilder string = new StringBuilder();
					int i = 0;
					for (Object entry : sourceMap.entrySet()) {
						Map.Entry<?, ?> mapEntry = (Map.Entry<?, ?>) entry;
						if (i > 0) {
							string.append(DELIMITER);
						}
						Object key = converter.convertKey(mapEntry.getKey());
						Object value = converter.convertValue(mapEntry.getValue());
						String property = key + "=" + value;
						string.append(property);
						i++;
					}
					return string.toString();
				}
			} else {
				TypeDescriptor sourceValueType = sourceType.getMapValueTypeDescriptor();
				boolean valuesCompatible = false;
				if (sourceValueType == TypeDescriptor.NULL || sourceValueType.isAssignableTo(targetType)) {
					valuesCompatible = true;
				}
				if (valuesCompatible) {
					return sourceMap.values().iterator().next();
				} else {
					MapEntryConverter converter = new MapEntryConverter(sourceValueType, sourceValueType, targetType,
							targetType, true, valuesCompatible, this.conversionService);
					Object value = sourceMap.values().iterator().next();
					return converter.convertValue(value);
				}
			}
		}
	}

}
