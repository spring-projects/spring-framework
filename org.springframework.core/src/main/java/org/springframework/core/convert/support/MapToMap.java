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
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Converts from one map to another map, with support for converting individual map elements
 * based on generic type information.
 *
 * @author Keith Donald
 * @since 3.0
 */
class MapToMap implements ConversionExecutor {

	private final TypeDescriptor sourceType;

	private final TypeDescriptor targetType;

	private final GenericConversionService conversionService;

	private final MapEntryConverter entryConverter;


	/**
	 * Creates a new map-to-map converter
	 * @param sourceType the source map type
	 * @param targetType the target map type
	 * @param conversionService the conversion service
	 */
	public MapToMap(TypeDescriptor sourceType, TypeDescriptor targetType, GenericConversionService conversionService) {
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.conversionService = conversionService;
		this.entryConverter = createEntryConverter();
	}


	private MapEntryConverter createEntryConverter() {
		if (this.sourceType.isMapEntryTypeKnown() && this.targetType.isMapEntryTypeKnown()) {
			ConversionExecutor keyConverter = this.conversionService.getConversionExecutor(
					this.sourceType.getMapKeyType(), TypeDescriptor.valueOf(this.targetType.getMapKeyType()));
			ConversionExecutor valueConverter = this.conversionService.getConversionExecutor(
					this.sourceType.getMapValueType(), TypeDescriptor.valueOf(this.targetType.getMapValueType()));
			return new MapEntryConverter(keyConverter, valueConverter);
		}
		else {
			return MapEntryConverter.NO_OP_INSTANCE;
		}
	}

	@SuppressWarnings("unchecked")
	public Object execute(Object source) throws ConversionFailedException {
		try {
			Map<?, ?> map = (Map<?, ?>) source;
			Map targetMap = CollectionFactory.createMap(this.targetType.getType(), map.size());
			MapEntryConverter converter = getEntryConverter(map);
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				targetMap.put(converter.convertKey(entry.getKey()), converter.convertValue(entry.getValue()));
			}
			return targetMap;
		}
		catch (Exception ex) {
			throw new ConversionFailedException(source, this.sourceType.getType(), this.targetType.getType(), ex);
		}
	}

	private MapEntryConverter getEntryConverter(Map<?, ?> map) {
		MapEntryConverter entryConverter = this.entryConverter;
		if (entryConverter == MapEntryConverter.NO_OP_INSTANCE) {
			Class<?> targetKeyType = targetType.getMapKeyType();
			Class<?> targetValueType = targetType.getMapValueType();
			if (targetKeyType != null && targetValueType != null) {
				ConversionExecutor keyConverter = null;
				ConversionExecutor valueConverter = null;
				for (Map.Entry<?, ?> entry : map.entrySet()) {
					Object key = entry.getKey();
					Object value = entry.getValue();
					if (keyConverter == null && key != null) {
						keyConverter = conversionService
								.getConversionExecutor(key.getClass(), TypeDescriptor.valueOf(targetKeyType));
					}
					if (valueConverter == null && value != null) {
						valueConverter = conversionService
								.getConversionExecutor(value.getClass(), TypeDescriptor.valueOf(targetValueType));
					}
					if (keyConverter != null && valueConverter != null) {
						break;
					}
				}
				entryConverter = new MapEntryConverter(keyConverter, valueConverter);
			}
		}
		return entryConverter;
	}
	
}
