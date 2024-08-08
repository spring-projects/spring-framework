/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression;

import org.springframework.lang.Nullable;

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
 * specifies explicit support for a given target class via
 * {@link #getSpecificTargetClasses()} will be called first, before other generic
 * accessors that do not specify explicit support for the given target class.
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
	@Nullable
	Class<?>[] getSpecificTargetClasses();

}
