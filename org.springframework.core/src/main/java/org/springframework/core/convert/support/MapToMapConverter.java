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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.core.CollectionFactory;
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

	private final GenericConversionService conversionService;

	public MapToMapConverter(GenericConversionService conversionService) {
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
			return this.conversionService.convertNullSource(sourceType, targetType);
		}
		Map<?, ?> sourceMap = (Map<?, ?>) source;
		TypeDescriptor targetKeyType = targetType.getMapKeyTypeDescriptor();
		TypeDescriptor targetValueType = targetType.getMapValueTypeDescriptor();
		if (targetKeyType == TypeDescriptor.NULL && targetValueType == TypeDescriptor.NULL) {
			return compatibleMapWithoutEntryConversion(sourceMap, targetType);
		}
		TypeDescriptor sourceKeyType = sourceType.getMapKeyTypeDescriptor();
		TypeDescriptor sourceValueType = sourceType.getMapValueTypeDescriptor();
		if (sourceKeyType == TypeDescriptor.NULL || sourceValueType == TypeDescriptor.NULL) {
			TypeDescriptor[] sourceEntryTypes = ConversionUtils.getMapEntryTypes(sourceMap);
			sourceKeyType = sourceEntryTypes[0];
			sourceValueType = sourceEntryTypes[1];
		}
		if (sourceKeyType == TypeDescriptor.NULL && sourceValueType == TypeDescriptor.NULL) {
			return compatibleMapWithoutEntryConversion(sourceMap, targetType);
		}
		boolean keysCompatible = false;
		if (sourceKeyType != TypeDescriptor.NULL && sourceKeyType.isAssignableTo(targetKeyType)) {
			keysCompatible = true;
		}
		boolean valuesCompatible = false;
		if (sourceValueType != TypeDescriptor.NULL && sourceValueType.isAssignableTo(targetValueType)) {
			valuesCompatible = true;
		}
		if (keysCompatible && valuesCompatible) {
			return compatibleMapWithoutEntryConversion(sourceMap, targetType);
		}
		Map targetMap = CollectionFactory.createMap(targetType.getType(), sourceMap.size());
		MapEntryConverter converter = new MapEntryConverter(sourceKeyType, sourceValueType, targetKeyType,
				targetValueType, keysCompatible, valuesCompatible, this.conversionService);
		for (Object entry : sourceMap.entrySet()) {
			Map.Entry sourceMapEntry = (Map.Entry) entry;
			Object targetKey = converter.convertKey(sourceMapEntry.getKey());
			Object targetValue = converter.convertValue(sourceMapEntry.getValue());
			targetMap.put(targetKey, targetValue);
		}
		return targetMap;
	}

	@SuppressWarnings("unchecked")
	private Map<?, ?> compatibleMapWithoutEntryConversion(Map<?, ?> source, TypeDescriptor targetType) {
		if (targetType.getType().isInstance(source)) {
			return source;
		}
		else {
			Map target = CollectionFactory.createMap(targetType.getType(), source.size());
			target.putAll(source);
			return target;
		}
	}

}
