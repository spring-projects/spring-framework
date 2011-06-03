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
		this.type = type;
	}
	
	public Class<?> getType() {
		return type;		
	}

	public TypeDescriptor getElementType() {
		if (isCollection()) {
			Class<?> elementType = wildcard(getCollectionElementClass());
			return new TypeDescriptor(nested(elementType, 0));
		} else if (isArray()) {
			Class<?> elementType = getType().getComponentType();
			return new TypeDescriptor(nested(elementType, 0));				
		} else {
			return TypeDescriptor.NULL;
		}
	}
	
	public TypeDescriptor getMapKeyType() {
		if (isMap()) {
			Class<?> keyType = wildcard(getMapKeyClass());
			return new TypeDescriptor(nested(keyType, 0));
		} else {
			return TypeDescriptor.NULL;
		}
	}
	
	public TypeDescriptor getMapValueType() {
		if (isMap()) {
			Class<?> valueType = wildcard(getMapValueClass());
			return new TypeDescriptor(nested(valueType, 1));
		} else {
			return TypeDescriptor.NULL;
		}
	}

	public abstract Annotation[] getAnnotations();

	public AbstractDescriptor nested() {
		if (isCollection()) {
			return nested(wildcard(getCollectionElementClass()), 0);
		} else if (isArray()) {
			return nested(getType().getComponentType(), 0);
		} else if (isMap()) {
			return nested(wildcard(getMapValueClass()), 1);
		} else {
			throw new IllegalStateException("Not a collection, array, or map: cannot resolve nested value types");
		}
	}
	
	// subclassing hooks
	
	protected abstract Class<?> getCollectionElementClass();
	
	protected abstract Class<?> getMapKeyClass();
	
	protected abstract Class<?> getMapValueClass();
	
	protected abstract AbstractDescriptor nested(Class<?> type, int typeIndex);
	
	// internal helpers
	
	private boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}
	
	private boolean isArray() {
		return getType().isArray();
	}
	
	private boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}
	
	private Class<?> wildcard(Class<?> type) {
		return type != null ? type : Object.class;
	}
	
}