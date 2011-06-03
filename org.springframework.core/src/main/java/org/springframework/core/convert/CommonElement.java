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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

class CommonElement {
	
	private final Class<?> type;
	
	private final Object value;

	public CommonElement(Class<?> type, Object value) {
		this.type = type;
		this.value = value;
	}

	public Class<?> getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

	public TypeDescriptor toTypeDescriptor() {
		if (type == null) {
			return TypeDescriptor.NULL;
		} else if (value instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>) value;
			return new TypeDescriptor(type, typeDescriptor(collection));			
		}
		else if (value instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) value;
			return new TypeDescriptor(type, typeDescriptor(map.keySet()), typeDescriptor(map.values()));
		}
		else {
			return TypeDescriptor.valueOf(type);
		}
	}
	
	public static TypeDescriptor typeDescriptor(Collection<?> collection) {
		return findCommonElement(collection).toTypeDescriptor();
	}

	// internal helpers
	
	private static CommonElement findCommonElement(Collection<?> values) {
		Class<?> commonType = null;
		Object candidate = null;
		for (Object value : values) {
			if (value != null) {
				if (candidate == null) {
					commonType = value.getClass();
					candidate = value;
				} else {
					commonType = commonType(commonType, value.getClass());
					if (commonType == Object.class) {
						return new CommonElement(Object.class, null);
					}
				}
			}
		}
		return new CommonElement(commonType, candidate);
	}

	private static Class<?> commonType(Class<?> commonType, Class<?> valueClass) {
		Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
		LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
		classQueue.addFirst(commonType);
		while (!classQueue.isEmpty()) {
			Class<?> currentClass = classQueue.removeLast();
			if (currentClass.isAssignableFrom(valueClass)) {
				return currentClass;
			}
			Class<?> superClass = currentClass.getSuperclass();
			if (superClass != null && superClass != Object.class) {
				classQueue.addFirst(currentClass.getSuperclass());
			}
			for (Class<?> interfaceType : currentClass.getInterfaces()) {
				addInterfaceHierarchy(interfaceType, interfaces);
			}
		}
		for (Class<?> interfaceType : interfaces) {
			if (interfaceType.isAssignableFrom(valueClass)) {
				return interfaceType;
			}			
		}
		return Object.class;
	}

	private static void addInterfaceHierarchy(Class<?> interfaceType, Set<Class<?>> interfaces) {
		interfaces.add(interfaceType);
		for (Class<?> inheritedInterface : interfaceType.getInterfaces()) {
			addInterfaceHierarchy(inheritedInterface, interfaces);
		}
	}

}