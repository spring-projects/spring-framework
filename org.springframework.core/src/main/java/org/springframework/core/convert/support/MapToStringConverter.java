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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Converts from a Map to a String.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class MapToStringConverter implements ConditionalGenericConverter {

	private final GenericConversionService conversionService;

	public MapToStringConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Map.class, String.class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.canConvert(sourceType.getMapKeyTypeDescriptor(), targetType)
				&& this.conversionService.canConvert(sourceType.getMapValueTypeDescriptor(), targetType);
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}
		Map<?, ?> sourceMap = (Map<?, ?>) source;
		if (sourceMap.size() == 0) {
			return "";
		} else {
			TypeDescriptor sourceKeyType = sourceType.getMapKeyTypeDescriptor();
			TypeDescriptor sourceValueType = sourceType.getMapValueTypeDescriptor();
			if (sourceKeyType == TypeDescriptor.NULL || sourceValueType == TypeDescriptor.NULL) {
				TypeDescriptor[] sourceEntryTypes = getMapEntryTypes(sourceMap);
				sourceKeyType = sourceEntryTypes[0];
				sourceValueType = sourceEntryTypes[1];
			}
			boolean keysCompatible = false;
			if (sourceKeyType != TypeDescriptor.NULL && sourceKeyType.isAssignableTo(targetType)) {
				keysCompatible = true;
			}
			boolean valuesCompatible = false;
			if (sourceValueType != TypeDescriptor.NULL && sourceValueType.isAssignableTo(targetType)) {
				valuesCompatible = true;
			}
			Properties props = new Properties();
			if (keysCompatible && valuesCompatible) {
				for (Object entry : sourceMap.entrySet()) {
					Map.Entry<?, ?> mapEntry = (Map.Entry<?, ?>) entry;
					props.setProperty((String) mapEntry.getKey(), (String) mapEntry.getValue());
				}
				return store(props);
			} else {
				MapEntryConverter converter = new MapEntryConverter(sourceKeyType, sourceValueType, targetType,
						targetType, keysCompatible, valuesCompatible, this.conversionService);
				for (Object entry : sourceMap.entrySet()) {
					Map.Entry<?, ?> mapEntry = (Map.Entry<?, ?>) entry;
					Object key = converter.convertKey(mapEntry.getKey());
					Object value = converter.convertValue(mapEntry.getValue());
					props.setProperty((String) key, (String) value);
				}
				return store(props);
			}
		}
	}

	private String store(Properties props) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			props.store(os, null);
			return os.toString("ISO-8859-1");
		} catch (IOException e) {
			// Should never happen.
			throw new IllegalArgumentException("Failed to store [" + props + "] into String", e);
		}
	}

}
