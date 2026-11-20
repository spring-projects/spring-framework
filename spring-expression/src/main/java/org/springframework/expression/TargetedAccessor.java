/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

import org.jspecify.annotations.Nullable;

/**
 * Strategy for types that access elements of specific target classes.
 *
 * <p>This interface places no restrictions on what constitutes an element.
 *
 * <p>A targeted accessor can specify a set of target classes for which it should
 * be called. However, if it returns {@code null} or an empty array from
 * {@link #getSpecificTargetClasses()}, it will typically be called for all
 * access operations and given a chance to determine if it supports a concrete
 * access attempt.
 *
 * <p>Targeted accessors are considered to be ordered, and each will be called
 * in turn. The only rule that affects the call order is that any accessor which
 * specifies explicit support for a given target type via
 * {@link #getSpecificTargetClasses()} will be called first, before other generic
 * accessors that do not specify support for explicit target types. In addition,
 * accessors that support the exact target type will be called before accessors
 * that support a supertype of the target type.
 *
 * @author Sam Brannen
 * @since 6.2
 * @see PropertyAccessor
 * @see IndexAccessor
 */
public interface TargetedAccessor {

	/**
	 * Get the set of classes for which this accessor should be called.
	 * <p>Returning {@code null} or an empty array indicates this is a generic
	 * accessor that can be called in an attempt to access an element on any
	 * type.
	 * @return an array of classes that this accessor is suitable for
	 * (or {@code null} or an empty array if a generic accessor)
	 */
	Class<?> @Nullable [] getSpecificTargetClasses();

}
