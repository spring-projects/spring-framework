/**
 * 
 */
package org.springframework.ui.binding.support;

import org.springframework.util.ObjectUtils;

public class CollectionTypeDescriptor {

	private Class<?> collectionType;

	private Class<?> elementType;

	/**
	 * Creates a new generic collection property type
	 * @param collectionType the collection type
	 * @param elementType the element type
	 */
	public CollectionTypeDescriptor(Class<?> collectionType, Class<?> elementType) {
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
		if (!(o instanceof CollectionTypeDescriptor)) {
			return false;
		}
		CollectionTypeDescriptor type = (CollectionTypeDescriptor) o;
		return collectionType.equals(type.collectionType)
				&& ObjectUtils.nullSafeEquals(elementType, type.elementType);
	}

	public int hashCode() {
		return collectionType.hashCode() + elementType.hashCode();
	}
}