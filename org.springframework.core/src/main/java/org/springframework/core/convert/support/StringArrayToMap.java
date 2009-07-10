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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;

@SuppressWarnings("unchecked")
class StringArrayToMap implements ConversionExecutor {

	private TypeDescriptor sourceType;

	private TypeDescriptor targetType;

	private GenericTypeConverter conversionService;

	private EntryConverter entryConverter;

	/**
	 * Creates a new map-to-map converter
	 * @param sourceType the source map type
	 * @param targetType the target map type
	 * @param conversionService the conversion service
	 */
	public StringArrayToMap(TypeDescriptor sourceType, TypeDescriptor targetType, GenericTypeConverter conversionService) {
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.conversionService = conversionService;
		this.entryConverter = createEntryConverter();
	}

	private EntryConverter createEntryConverter() {
		if (targetType.isMapEntryTypeKnown()) {
			ConversionExecutor keyConverter = conversionService.getConversionExecutor(String.class,
					TypeDescriptor.valueOf(targetType.getMapKeyType()));
			ConversionExecutor valueConverter = conversionService.getConversionExecutor(String.class,
					TypeDescriptor.valueOf(targetType.getMapValueType()));
			return new EntryConverter(keyConverter, valueConverter);
		} else {
			return EntryConverter.NO_OP_INSTANCE;
		}
	}

	public Object execute(Object source) throws ConversionFailedException {
		try {
			Map targetMap = (Map) getImpl(targetType.getType()).newInstance();
			String[] array = (String[]) source;
			for (String string : array) {
				String[] fields = string.split("=");
				String key = fields[0];
				String value = fields[1];
				targetMap.put(entryConverter.convertKey(key), entryConverter.convertValue(value));
			}
			return targetMap;
		} catch (Exception e) {
			throw new ConversionFailedException(source, sourceType.getType(), targetType.getType(), e);
		}
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
