/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Converts a Map to another Map.
 *
 * <p>First, creates a new Map of the requested targetType with a size equal to the
 * size of the source Map. Then copies each element in the source map to the target map.
 * Will perform a conversion from the source maps's parameterized K,V types to the target
 * map's parameterized types K,V if necessary.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class MapToMapConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;

	public MapToMapConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Map.class, Map.class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return canConvertKey(sourceType, targetType) && canConvertValue(sourceType, targetType);
	}
	
	@SuppressWarnings("unchecked")
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		boolean copyRequired = !targetType.getType().isInstance(source);
		Map<Object, Object> sourceMap = (Map<Object, Object>) source;
		if (!copyRequired && sourceMap.isEmpty()) {
			return sourceMap;
		}
		Map<Object, Object> targetMap = CollectionFactory.createMap(targetType.getType(), sourceMap.size());
		for (Map.Entry<Object, Object> entry : sourceMap.entrySet()) {
			Object sourceKey = entry.getKey();
			Object sourceValue = entry.getValue();
			Object targetKey = convertKey(sourceKey, sourceType, targetType.getMapKeyTypeDescriptor());
			Object targetValue = convertValue(sourceValue, sourceType, targetType.getMapValueTypeDescriptor());
			targetMap.put(targetKey, targetValue);
			if (sourceKey != targetKey || sourceValue != targetValue) {
				copyRequired = true;
			}
		}
		return (copyRequired ? targetMap : sourceMap);
	}
	
	// internal helpers

	private boolean canConvertKey(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType.getMapKeyTypeDescriptor(),
				targetType.getMapKeyTypeDescriptor(), this.conversionService);
	}
	
	private boolean canConvertValue(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType.getMapValueTypeDescriptor(),
				targetType.getMapValueTypeDescriptor(), this.conversionService);
	}
	
	private Object convertKey(Object sourceKey, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (targetType == null) {
			return sourceKey;
		}
		return this.conversionService.convert(sourceKey, sourceType.getMapKeyTypeDescriptor(sourceKey), targetType);
	}

	private Object convertValue(Object sourceValue, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (targetType == null) {
			return sourceValue;
		}
		return this.conversionService.convert(sourceValue, sourceType.getMapValueTypeDescriptor(sourceValue), targetType);
	}

}
