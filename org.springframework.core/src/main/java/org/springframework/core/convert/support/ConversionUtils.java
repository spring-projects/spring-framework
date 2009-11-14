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
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;

final class ConversionUtils {

	private ConversionUtils() {
	}
	
	public static Object invokeConverter(GenericConverter converter, Object source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {
		try {
			return converter.convert(source, sourceType, targetType);
		} catch (Exception ex) {
			throw new ConversionFailedException(sourceType, targetType, source, ex);
		}
	}

	public static TypeDescriptor getElementType(Collection<?> collection) {
		for (Object element : collection) {
			if (element != null) {
				return TypeDescriptor.valueOf(element.getClass());
			}
		}
		return TypeDescriptor.NULL;
	}
	
	public static TypeDescriptor[] getMapEntryTypes(Map<?, ?> sourceMap) {
		Class<?> keyType = null;
		Class<?> valueType = null;
		for (Object entry : sourceMap.entrySet()) {
			Map.Entry<?, ?> mapEntry = (Map.Entry<?, ?>) entry;
			Object key = mapEntry.getKey();
			if (keyType == null && key != null) {
				keyType = key.getClass();
			}
			Object value = mapEntry.getValue();
			if (valueType == null && value != null) {
				valueType = value.getClass();
			}
			if (keyType!= null && valueType != null) {
				break;
			}
		}
		return new TypeDescriptor[] { TypeDescriptor.valueOf(keyType), TypeDescriptor.valueOf(valueType) };
	}

	public static List<?> asList(Object array) {
		return array != null ? new ArrayList(array) : null;
	}
	
	@SuppressWarnings("serial")
	private static class ArrayList extends AbstractList<Object> implements RandomAccess, java.io.Serializable {

		private Object array;

		ArrayList(Object array) {
			this.array = array;
		}

		public int size() {
			return Array.getLength(array);
		}

		public Object get(int index) {
			return Array.get(array, index);
		}
		
	}
}
