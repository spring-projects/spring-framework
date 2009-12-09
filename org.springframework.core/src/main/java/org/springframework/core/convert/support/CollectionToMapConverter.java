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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import static org.springframework.core.convert.support.ConversionUtils.*;

/**
 * Converts from a Collection to a Map.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class CollectionToMapConverter implements GenericConverter {

	private final GenericConversionService conversionService;

	public CollectionToMapConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, Map.class));
	}

	@SuppressWarnings("unchecked")
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}		
		Collection<?> sourceCollection = (Collection<?>) source;
		TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
		if (sourceElementType == TypeDescriptor.NULL) {
			sourceElementType = getElementType(sourceCollection);
		}
		TypeDescriptor targetKeyType = targetType.getMapKeyTypeDescriptor();
		TypeDescriptor targetValueType = targetType.getMapValueTypeDescriptor();
		boolean keysCompatible = false;
		if (sourceElementType != TypeDescriptor.NULL && sourceElementType.isAssignableTo(targetKeyType)) {
			keysCompatible = true;
		}
		boolean valuesCompatible = false;
		if (sourceElementType != TypeDescriptor.NULL && sourceElementType.isAssignableTo(targetValueType)) {
			valuesCompatible = true;
		}
		if (keysCompatible && valuesCompatible) {
			Map target = CollectionFactory.createMap(targetType.getType(), sourceCollection.size());
			if (sourceElementType.typeEquals(String.class)) {
				for (Object element : sourceCollection) {
					String[] property = parseProperty((String) element);
					target.put(property[0], property[1]);
				}
			}
			else {
				for (Object element : sourceCollection) {
					target.put(element, element);
				}
			}
			return target;
		}
		else {
			Map target = CollectionFactory.createMap(targetType.getType(), sourceCollection.size());
			MapEntryConverter converter = new MapEntryConverter(sourceElementType, sourceElementType, targetKeyType,
					targetValueType, keysCompatible, valuesCompatible, conversionService);
			if (sourceElementType.typeEquals(String.class)) {
				for (Object element : sourceCollection) {
					String[] property = parseProperty((String) element);
					Object targetKey = converter.convertKey(property[0]);
					Object targetValue = converter.convertValue(property[1]);
					target.put(targetKey, targetValue);
				}
			}
			else {
				for (Object element : sourceCollection) {
					Object targetKey = converter.convertKey(element);
					Object targetValue = converter.convertValue(element);
					target.put(targetKey, targetValue);
				}
			}
			return target;
		}
	}

	private String[] parseProperty(String string) {
		String[] property = string.split("=");
		if (property.length < 2) {
			throw new IllegalArgumentException("Invalid String property '" + string +
					"'; properties should be in the format name=value");
		}
		return property;
	}

}
