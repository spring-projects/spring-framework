/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.core.convert;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

abstract class AbstractDescriptor {

	private final Class<?> type;

	public AbstractDescriptor(Class<?> type) {
		//if (type == null) {
	//		throw new IllegalArgumentException("type cannot be null");
		//}
		this.type = type;
	}
	
	public Class<?> getType() {
		return type;		
	}

	public TypeDescriptor getElementType() {
		if (isCollection()) {
			Class<?> elementType = resolveCollectionElementType();
			return elementType != null ? new TypeDescriptor(nested(elementType, 0)) : null;
		} else if (isArray()) {
			Class<?> elementType = getType().getComponentType();
			return new TypeDescriptor(nested(elementType, 0));				
		} else {
			return null;
		}
	}
	
	public TypeDescriptor getMapKeyType() {
		if (isMap()) {
			Class<?> keyType = resolveMapKeyType();
			return keyType != null ? new TypeDescriptor(nested(keyType, 0)) : null;
		} else {
			return null;
		}
	}
	
	public TypeDescriptor getMapValueType() {
		if (isMap()) {
			Class<?> valueType = resolveMapValueType();
			return valueType != null ? new TypeDescriptor(nested(valueType, 1)) : null;
		} else {
			return null;
		}
	}

	public abstract Annotation[] getAnnotations();

	public AbstractDescriptor nested() {
		if (isCollection()) {
			return nested(resolveCollectionElementType(), 0);
		} else if (isArray()) {
			return nested(getType().getComponentType(), 0);
		} else if (isMap()) {
			return nested(resolveMapValueType(), 1);
		} else {
			throw new IllegalStateException("Not a collection, array, or map: cannot resolve nested value types");
		}
	}
	
	// subclassing hooks
	
	protected abstract Class<?> resolveCollectionElementType();
	
	protected abstract Class<?> resolveMapKeyType();
	
	protected abstract Class<?> resolveMapValueType();
	
	protected abstract AbstractDescriptor nested(Class<?> type, int typeIndex);
	
	// internal helpers
	
	private boolean isCollection() {
		return getType() != null && Collection.class.isAssignableFrom(getType());
	}
	
	private boolean isArray() {
		return getType() != null && getType().isArray();
	}
	
	private boolean isMap() {
		return getType() != null && Map.class.isAssignableFrom(getType());
	}
	
}