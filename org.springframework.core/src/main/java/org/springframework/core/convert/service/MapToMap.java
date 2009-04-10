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
package org.springframework.core.convert.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.core.convert.ConversionExecutionException;
import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Converts from one map to another map, with support for converting individual map elements based on generic type information.
 * @author Keith Donald
 */
class MapToMap implements ConversionExecutor {

	private TypeDescriptor sourceType;

	private TypeDescriptor targetType;

	private ConversionService conversionService;

	private EntryConverter entryConverter;

	/**
	 * Creates a new map-to-map converter
	 * @param sourceType the source map type
	 * @param targetType the target map type
	 * @param conversionService the conversion service
	 */
	public MapToMap(TypeDescriptor sourceType, TypeDescriptor targetType, ConversionService conversionService) {
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.conversionService = conversionService;
		this.entryConverter = createEntryConverter();
	}

	private EntryConverter createEntryConverter() {
		if (sourceType.isMapEntryTypeKnown() && targetType.isMapEntryTypeKnown()) {
			ConversionExecutor keyConverter = conversionService.getConversionExecutor(sourceType.getMapKeyType(),
					TypeDescriptor.valueOf(targetType.getMapKeyType()));
			ConversionExecutor valueConverter = conversionService.getConversionExecutor(sourceType.getMapValueType(),
					TypeDescriptor.valueOf(targetType.getMapValueType()));
			return new EntryConverter(keyConverter, valueConverter);
		} else {
			return EntryConverter.NO_OP_INSTANCE;
		}
	}

	@SuppressWarnings("unchecked")
	public Object execute(Object source) throws ConversionExecutionException {
		try {
			Map map = (Map) source;
			Map targetMap = (Map) getImpl(targetType.getType()).newInstance();
			EntryConverter converter = getEntryConverter(map);
			Iterator<Map.Entry<?, ?>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = it.next();
				targetMap.put(converter.convertKey(entry.getKey()), converter.convertValue(entry.getValue()));
			}
			return targetMap;
		} catch (Exception e) {
			throw new ConversionExecutionException(source, sourceType.getType(), targetType, e);
		}
	}

	private EntryConverter getEntryConverter(Map<?, ?> map) {
		EntryConverter entryConverter = this.entryConverter;
		if (entryConverter == EntryConverter.NO_OP_INSTANCE) {
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
				entryConverter = new EntryConverter(keyConverter, valueConverter);
			}
		}
		return entryConverter;
	}

	static Class<?> getImpl(Class<?> targetClass) {
		if (targetClass.isInterface()) {
			if (Map.class.equals(targetClass)) {
				return HashMap.class;
			} else if (SortedMap.class.equals(targetClass)) {
				return TreeMap.class;
			} else {
				throw new IllegalArgumentException("Unsupported Map interface [" + targetClass.getName() + "]");
			}
		} else {
			return targetClass;
		}
	}

	private static class EntryConverter {

		public static final EntryConverter NO_OP_INSTANCE = new EntryConverter();

		private ConversionExecutor keyConverter;

		private ConversionExecutor valueConverter;

		private EntryConverter() {

		}

		public EntryConverter(ConversionExecutor keyConverter, ConversionExecutor valueConverter) {
			this.keyConverter = keyConverter;
			this.valueConverter = valueConverter;
		}

		public Object convertKey(Object key) {
			if (keyConverter != null) {
				return keyConverter.execute(key);
			} else {
				return key;
			}
		}

		public Object convertValue(Object value) {
			if (valueConverter != null) {
				return valueConverter.execute(value);
			} else {
				return value;
			}
		}

	}

}
