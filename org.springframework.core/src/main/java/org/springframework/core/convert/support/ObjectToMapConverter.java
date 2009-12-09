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

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * Converts from a single Object to a Map.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class ObjectToMapConverter implements GenericConverter {

	private final GenericConversionService conversionService;

	public ObjectToMapConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Map.class));
	}

	@SuppressWarnings("unchecked")
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}
		if (sourceType.typeEquals(String.class)) {
			String string = (String) source;
			return this.conversionService.convert(loadProperties(string), TypeDescriptor.valueOf(Properties.class), targetType);
		} else {
			Map target = CollectionFactory.createMap(targetType.getType(), 1);
			TypeDescriptor targetKeyType = targetType.getMapKeyTypeDescriptor();
			TypeDescriptor targetValueType = targetType.getMapValueTypeDescriptor();
			boolean keysCompatible = false;
			if (sourceType != TypeDescriptor.NULL && sourceType.isAssignableTo(targetKeyType)) {
				keysCompatible = true;
			}
			boolean valuesCompatible = false;
			if (sourceType != TypeDescriptor.NULL && sourceType.isAssignableTo(targetValueType)) {
				valuesCompatible = true;
			}
			if (keysCompatible && valuesCompatible) {
				target.put(source, source);
			} else {
				MapEntryConverter converter = new MapEntryConverter(sourceType, sourceType, targetKeyType,
						targetValueType, keysCompatible, valuesCompatible, this.conversionService);
				Object key = converter.convertKey(source);
				Object value = converter.convertValue(source);
				target.put(key, value);
			}
			return target;
		}
	}

	private Properties loadProperties(String string) {
		try {
			Properties props = new Properties();
			// Must use the ISO-8859-1 encoding because Properties.load(stream) expects it.
			props.load(new ByteArrayInputStream(string.getBytes("ISO-8859-1")));
			return props;
		} catch (Exception e) {
			// Should never happen.
			throw new IllegalArgumentException("Failed to parse [" + string + "] into Properties", e);
		}
	}

}
