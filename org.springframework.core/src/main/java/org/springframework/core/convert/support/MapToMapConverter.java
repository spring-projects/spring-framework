/*
 * Copyright 2002-2010 the original author or authors.
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
		return this.conversionService.canConvert(sourceType.getMapKeyTypeDescriptor(), targetType.getMapKeyTypeDescriptor()) && 
			this.conversionService.canConvert(sourceType.getMapValueTypeDescriptor(), targetType.getMapValueTypeDescriptor());
	}

	@SuppressWarnings("unchecked")
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		Map<Object, Object> sourceMap = (Map<Object, Object>) source;
		Map<Object, Object> targetMap = CollectionFactory.createMap(targetType.getType(), sourceMap.size());
		TypeDescriptor sourceKeyType = sourceType.getMapKeyTypeDescriptor();
		TypeDescriptor targetKeyType = targetType.getMapKeyTypeDescriptor();
		TypeDescriptor sourceValueType = sourceType.getMapValueTypeDescriptor();
		TypeDescriptor targetValueType = targetType.getMapValueTypeDescriptor();
		if (Object.class.equals(targetKeyType.getType()) && Object.class.equals(targetValueType.getType())) {
			for (Map.Entry<Object, Object> entry : sourceMap.entrySet()) {
				targetMap.put(entry.getKey(), entry.getValue());
			}
		} else {
			for (Map.Entry<Object, Object> entry : sourceMap.entrySet()) {
				Object sourceKey = entry.getKey();
				Object sourceValue = entry.getValue();
				Object targetKey = this.conversionService.convert(sourceKey, sourceKeyType, targetKeyType);
				Object targetValue = this.conversionService.convert(sourceValue, sourceValueType, targetValueType);
				targetMap.put(targetKey, targetValue);
			}			
		}
		return targetMap;
	}
	
}