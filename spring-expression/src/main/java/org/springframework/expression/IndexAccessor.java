/*
 * Copyright 2002-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

/**
 * An index accessor is able to read from and possibly write to an indexed
 * structure of a target object.
 *
 * <p>This interface places no restrictions on what constitutes an indexed
 * structure. Implementors are therefore free to access indexed values any way
 * they deem appropriate.
 *
 * <p>An index accessor can specify an array of
 * {@linkplain #getSpecificTargetClasses() target classes} for which it should be
 * called. See {@link TargetedAccessor} for details.
 *
 * @author Jackmiking Lee
 * @author Sam Brannen
 * @since 6.2
 * @see TargetedAccessor
 * @see PropertyAccessor
 */
public interface IndexAccessor extends TargetedAccessor {

	/**
	 * Determine if this index accessor is able to read a specified index on a
	 * specified target object.
	 * @param context the evaluation context in which the access is being attempted
	 * @param target the target object upon which the index is being accessed
	 * @param index the index being accessed
	 * @return {@code true} if this index accessor is able to read the index
	 * @throws AccessException if there is any problem determining whether the
	 * index can be read
	 */
	boolean canRead(EvaluationContext context, Object target, Object index) throws AccessException;

	/**
	 * Read an index from a specified target object.
	 * <p>Should only be invoked if {@link #canRead} returns {@code true} for the
	 * same arguments.
	 * @param context the evaluation context in which the access is being attempted
	 * @param target the target object upon which the index is being accessed
	 * @param index the index being accessed
	 * @return a TypedValue object wrapping the index value read and a type
	 * descriptor for the value
	 * @throws AccessException if there is any problem reading the index
	 */
	TypedValue read(EvaluationContext context, Object target, Object index) throws AccessException;

	/**
	 * Determine if this index accessor is able to write to a specified index on
	 * a specified target object.
	 * @param context the evaluation context in which the access is being attempted
	 * @param target the target object upon which the index is being accessed
	 * @param index the index being accessed
	 * @return {@code true} if this index accessor is able to write to the index
	 * @throws AccessException if there is any problem determining whether the
	 * index can be written to
	 */
	boolean canWrite(EvaluationContext context, Object target, Object index) throws AccessException;

	/**
	 * Write to an index on a specified target object.
	 * <p>Should only be invoked if {@link #canWrite} returns {@code true} for the
	 * same arguments.
	 * @param context the evaluation context in which the access is being attempted
	 * @param target the target object upon which the index is being accessed
	 * @param index the index being accessed
	 * @param newValue the new value for the index
	 * @throws AccessException if there is any problem writing to the index
	 */
	void write(EvaluationContext context, Object target, Object index, @Nullable Object newValue)
			throws AccessException;

}
