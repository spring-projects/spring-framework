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

import java.lang.reflect.Array;
import java.util.Map;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Converts a String array to a Map.
 * Each element in the array must be formatted as key=value.
 *
 * @author Keith Donald
 * @since 3.0
 */
class StringArrayToMap implements ConversionExecutor {

	private final TypeDescriptor sourceType;

	private final TypeDescriptor targetType;

	private final GenericConversionService conversionService;

	private final MapEntryConverter entryConverter;


	public StringArrayToMap(TypeDescriptor sourceType, TypeDescriptor targetType, GenericConversionService conversionService) {
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.conversionService = conversionService;
		this.entryConverter = createEntryConverter();
	}


	private MapEntryConverter createEntryConverter() {
		if (this.targetType.isMapEntryTypeKnown()) {
			ConversionExecutor keyConverter = this.conversionService.getConversionExecutor(String.class,
					TypeDescriptor.valueOf(this.targetType.getMapKeyType()));
			ConversionExecutor valueConverter = this.conversionService.getConversionExecutor(String.class,
					TypeDescriptor.valueOf(this.targetType.getMapValueType()));
			return new MapEntryConverter(keyConverter, valueConverter);
		}
		else {
			return MapEntryConverter.NO_OP_INSTANCE;
		}
	}

	@SuppressWarnings("unchecked")
	public Object execute(Object source) throws ConversionFailedException {
		try {
			int length = Array.getLength(source);
			Map targetMap = CollectionFactory.createMap(this.targetType.getType(), length);
			for (int i = 0; i < length; i++) {
				String property = (String) Array.get(source, i);
				String[] fields = property.split("=");
				if (fields.length < 2) {
					throw new IllegalArgumentException("Invalid String property: " + property);
				}
				String key = fields[0];
				String value = fields[1];
				targetMap.put(this.entryConverter.convertKey(key), this.entryConverter.convertValue(value));
			}
			return targetMap;
		}
		catch (Exception ex) {
			throw new ConversionFailedException(source, this.sourceType.getType(), this.targetType.getType(), ex);
		}
	}

}
