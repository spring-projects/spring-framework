/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Tag collection class used to hold managed array elements, which may
 * include runtime bean references (to be resolved into bean objects).
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ManagedArray extends ManagedList<Object> {

	/** Resolved element type for runtime creation of the target array. */
	volatile @Nullable Class<?> resolvedElementType;


	/**
	 * Create a new managed array placeholder.
	 * @param elementTypeName the target element type as a class name
	 * @param size the size of the array
	 */
	public ManagedArray(String elementTypeName, int size) {
		super(size);
		Assert.notNull(elementTypeName, "elementTypeName must not be null");
		setElementTypeName(elementTypeName);
	}

}
