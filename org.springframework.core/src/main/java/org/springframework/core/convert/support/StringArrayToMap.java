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

import java.lang.reflect.Array;
import java.util.Map;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Converts a String array to a Map.
 * Each element in the array must be formatted as key=value.
 * @author Keith Donald
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class StringArrayToMap implements ConversionExecutor {

	private TypeDescriptor sourceType;

	private TypeDescriptor targetType;

	private GenericTypeConverter conversionService;

	private MapEntryConverter entryConverter;

	public StringArrayToMap(TypeDescriptor sourceType, TypeDescriptor targetType, GenericTypeConverter conversionService) {
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.conversionService = conversionService;
		this.entryConverter = createEntryConverter();
	}

	private MapEntryConverter createEntryConverter() {
		if (targetType.isMapEntryTypeKnown()) {
			ConversionExecutor keyConverter = conversionService.getConversionExecutor(String.class,
					TypeDescriptor.valueOf(targetType.getMapKeyType()));
			ConversionExecutor valueConverter = conversionService.getConversionExecutor(String.class,
					TypeDescriptor.valueOf(targetType.getMapValueType()));
			return new MapEntryConverter(keyConverter, valueConverter);
		} else {
			return MapEntryConverter.NO_OP_INSTANCE;
		}
	}

	public Object execute(Object source) throws ConversionFailedException {
		try {
			Map targetMap = (Map) ConversionUtils.getMapImpl(targetType.getType()).newInstance();
			int length = Array.getLength(source);
			for (int i = 0; i < length; i++) {
				String property = (String) Array.get(source, i);
				String[] fields = property.split("=");
				String key = fields[0];
				String value = fields[1];
				targetMap.put(entryConverter.convertKey(key), entryConverter.convertValue(value));
			}
			return targetMap;
		} catch (Exception e) {
			throw new ConversionFailedException(source, sourceType.getType(), targetType.getType(), e);
		}
	}

}
