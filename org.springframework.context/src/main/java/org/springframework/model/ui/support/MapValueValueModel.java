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
package org.springframework.model.ui.support;

import java.util.Map;

import org.springframework.core.convert.TypeDescriptor;

/**
 * A ValueModel for a element in a Map.
 * @author Keith Donald
 * @since 3.0
 */
public class MapValueValueModel implements ValueModel {

	private Object key;

	private Class<?> elementType;

	@SuppressWarnings("unchecked")
	private Map map;

	@SuppressWarnings("unchecked")
	public MapValueValueModel(Object key, Class<?> elementType, Map map, FieldModelContext bindingContext) {
		this.key = key;
		this.elementType = elementType;
		this.map = map;			
	}

	public Object getValue() {
		return map.get(key);
	}

	public Class<?> getValueType() {
		if (elementType != null) {
			return elementType;
		} else {
			return getValue().getClass();
		}
	}

	public TypeDescriptor<?> getValueTypeDescriptor() {
		return TypeDescriptor.valueOf(getValueType());
	}

	public boolean isWriteable() {
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public void setValue(Object value) {
		map.put(key, value);
	}
}