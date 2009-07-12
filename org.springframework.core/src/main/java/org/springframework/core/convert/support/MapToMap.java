/*
 * Copyright 2004-2009 the original author or authors.
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

import java.util.Iterator;
import java.util.Map;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Converts from one map to another map, with support for converting individual map elements based on generic type information.
 * @author Keith Donald
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class MapToMap implements ConversionExecutor {

	private TypeDescriptor sourceType;

	private TypeDescriptor targetType;

	private GenericTypeConverter conversionService;

	private MapEntryConverter entryConverter;

	/**
	 * Creates a new map-to-map converter
	 * @param sourceType the source map type
	 * @param targetType the target map type
	 * @param conversionService the conversion service
	 */
	public MapToMap(TypeDescriptor sourceType, TypeDescriptor targetType, GenericTypeConverter conversionService) {
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.conversionService = conversionService;
		this.entryConverter = createEntryConverter();
	}

	private MapEntryConverter createEntryConverter() {
		if (sourceType.isMapEntryTypeKnown() && targetType.isMapEntryTypeKnown()) {
			ConversionExecutor keyConverter = conversionService.getConversionExecutor(sourceType.getMapKeyType(),
					TypeDescriptor.valueOf(targetType.getMapKeyType()));
			ConversionExecutor valueConverter = conversionService.getConversionExecutor(sourceType.getMapValueType(),
					TypeDescriptor.valueOf(targetType.getMapValueType()));
			return new MapEntryConverter(keyConverter, valueConverter);
		} else {
			return MapEntryConverter.NO_OP_INSTANCE;
		}
	}

	public Object execute(Object source) throws ConversionFailedException {
		try {
			Map map = (Map) source;
			Map targetMap = (Map) ConversionUtils.getMapImpl(targetType.getType()).newInstance();
			MapEntryConverter converter = getEntryConverter(map);
			Iterator<Map.Entry<?, ?>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = it.next();
				targetMap.put(converter.convertKey(entry.getKey()), converter.convertValue(entry.getValue()));
			}
			return targetMap;
		} catch (Exception e) {
			throw new ConversionFailedException(source, sourceType.getType(), targetType.getType(), e);
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
				Iterator<?> it = map.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
					Object key = entry.getKey();
					Object value = entry.getValue();
					if (keyConverter == null && key != null) {
						keyConverter = conversionService.getConversionExecutor(key.getClass(), TypeDescriptor
								.valueOf(targetKeyType));
					}
					if (valueConverter == null && value != null) {
						valueConverter = conversionService.getConversionExecutor(value.getClass(), TypeDescriptor
								.valueOf(targetValueType));
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
