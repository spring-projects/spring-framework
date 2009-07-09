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
package org.springframework.ui.binding.support;

/**
 * Specifies the element type of a generic collection type.
 * @author Keith Donald
 * @since 3.0
 */
public class GenericCollectionPropertyType {

	private Class<?> collectionType;
	
	private Class<?> elementType;
	
	/**
	 * Creates a new generic collection property type
	 * @param collectionType the collection type
	 * @param elementType the element type
	 */
	public GenericCollectionPropertyType(Class<?> collectionType, Class<?> elementType) {
		this.collectionType = collectionType;
		this.elementType = elementType;
	}
	
	/**
	 * The collection type.
	 */
	public Class<?> getCollectionType() {
		return collectionType;
	}

	/**
	 * The element type.
	 */
	public Class<?> getElementType() {
		return elementType;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof GenericCollectionPropertyType)) {
			return false;
		}
		GenericCollectionPropertyType type = (GenericCollectionPropertyType) o;
		return collectionType.equals(type.collectionType) && elementType.equals(type.elementType);
	}
	
	public int hashCode() {
		return collectionType.hashCode() + elementType.hashCode();
	}
}