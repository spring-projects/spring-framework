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

import static org.springframework.core.convert.support.ConversionUtils.getMapEntryTypes;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Converts a Map to a Collection.
 * First, creates a new Collection of the requested targetType with a size equal to the size of the source Map.
 * Then copies each element in the source map to the target collection.
 * During the copy process, if the target collection's parameterized type is a String, each Map entry is first encoded as a "key=value" property String, then added to the Collection.
 * If the collection type is another Object type, the value of each Map entry is added to the Collection.
 * Will perform a conversion from the source maps's parameterized K,V types to the target collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class MapToCollectionConverter implements ConditionalGenericConverter {

	private final GenericConversionService conversionService;

	public MapToCollectionConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Map.class, Collection.class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.canConvert(sourceType.getMapKeyTypeDescriptor(), targetType.getElementTypeDescriptor()) && 
			this.conversionService.canConvert(sourceType.getMapValueTypeDescriptor(), targetType.getElementTypeDescriptor());
	}

	@SuppressWarnings("unchecked")
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}
		Map<?, ?> sourceMap = (Map<?, ?>) source;
		TypeDescriptor sourceKeyType = sourceType.getMapKeyTypeDescriptor();
		TypeDescriptor sourceValueType = sourceType.getMapValueTypeDescriptor();
		if (sourceKeyType == TypeDescriptor.NULL || sourceValueType == TypeDescriptor.NULL) {
			TypeDescriptor[] sourceEntryTypes = getMapEntryTypes(sourceMap);
			sourceKeyType = sourceEntryTypes[0];
			sourceValueType = sourceEntryTypes[1];
		}
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		boolean keysCompatible = false;
		if (sourceKeyType != TypeDescriptor.NULL && sourceKeyType.isAssignableTo(targetElementType)) {
			keysCompatible = true;
		}
		boolean valuesCompatible = false;
		if (sourceValueType != TypeDescriptor.NULL || sourceValueType.isAssignableTo(targetElementType)) {
			valuesCompatible = true;
		}
		Collection target = CollectionFactory.createCollection(targetType.getType(), sourceMap.size());
		MapEntryConverter converter = new MapEntryConverter(sourceKeyType, sourceValueType, targetElementType,
				targetElementType, keysCompatible, valuesCompatible, this.conversionService);
		if (targetElementType.getType().equals(String.class)) {
			for (Object entry : sourceMap.entrySet()) {
				Map.Entry<?, ?> mapEntry = (Map.Entry<?, ?>) entry;
				String property = converter.convertKey(mapEntry.getKey()) + "="
						+ converter.convertValue(mapEntry.getValue());
				target.add(property);
			}
		} else {
			for (Object value : sourceMap.values()) {
				target.add(converter.convertValue(value));
			}
		}
		return target;
	}

}
