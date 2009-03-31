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
import org.springframework.core.convert.TypeDescriptor;

class MapToMap implements ConversionExecutor {

	private TypeDescriptor sourceType;

	private TypeDescriptor targetType;

	private GenericConversionService conversionService;

	public MapToMap(TypeDescriptor sourceType, TypeDescriptor targetType, GenericConversionService conversionService) {
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.conversionService = conversionService;
	}

	@SuppressWarnings("unchecked")
	public Object execute(Object source) throws ConversionExecutionException {
		try {
			// TODO shouldn't do all this if generic info is null - should cache executor after first iteration?
			Map map = (Map) source;
			Map targetMap = (Map) getImpl(targetType.getType()).newInstance();
			Iterator<Map.Entry<?, ?>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = it.next();
				Object key = entry.getKey();
				Object value = entry.getValue();
				key = conversionService.executeConversion(key, TypeDescriptor.valueOf(targetType.getMapKeyType()));
				value = conversionService.executeConversion(value, TypeDescriptor.valueOf(targetType.getMapValueType()));
				targetMap.put(key, value);
			}
			return targetMap;
		} catch (Exception e) {
			throw new ConversionExecutionException(source, sourceType, targetType, e);
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

}
