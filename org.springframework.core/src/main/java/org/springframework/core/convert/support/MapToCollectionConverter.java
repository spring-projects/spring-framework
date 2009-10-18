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

import java.util.Collection;
import java.util.Map;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Converts from a Map to a Collection.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class MapToCollectionConverter implements GenericConverter {

	private final GenericConversionService conversionService;

	public MapToCollectionConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@SuppressWarnings("unchecked")
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}
		Map sourceMap = (Map) source;
		TypeDescriptor sourceKeyType = sourceType.getMapKeyTypeDescriptor();
		TypeDescriptor sourceValueType = sourceType.getMapValueTypeDescriptor();
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		boolean keysCompatible = false;
		if (targetElementType == TypeDescriptor.NULL || sourceKeyType.isAssignableTo(targetElementType)) {
			keysCompatible = true;
		}
		boolean valuesCompatible = false;
		if (targetElementType == TypeDescriptor.NULL || sourceValueType.isAssignableTo(targetElementType)) {
			valuesCompatible = true;
		}
		Collection target = CollectionFactory.createCollection(targetType.getType(), sourceMap.size());
		MapEntryConverter converter = new MapEntryConverter(sourceKeyType, sourceValueType, targetElementType,
				targetElementType, keysCompatible, valuesCompatible, this.conversionService);
		if (targetElementType.getType().equals(String.class)) {
			for (Object entry : sourceMap.entrySet()) {
				Map.Entry mapEntry = (Map.Entry) entry;
				String property = converter.convertKey(mapEntry.getKey()) + "="
						+ converter.convertValue(mapEntry.getValue());
				target.add(property);
			}
		}
		else {
			for (Object value : sourceMap.values()) {
				target.add(value);
			}			
		}
		return target;
	}

}
