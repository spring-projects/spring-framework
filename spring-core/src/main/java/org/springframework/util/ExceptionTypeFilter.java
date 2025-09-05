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

package org.springframework.util;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

/**
 * An {@link InstanceFilter} that handles exception types.
 *
 * <p>An exception type will match against a given candidate if it is assignable
 * to that candidate.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.1
 */
public class ExceptionTypeFilter extends InstanceFilter<Class<? extends Throwable>> {

	/**
	 * Create a new {@code ExceptionTypeFilter} based on include and exclude
	 * collections, with the {@code matchIfEmpty} flag set to {@code true}.
	 * <p>See {@link #ExceptionTypeFilter(Collection, Collection, boolean)} for
	 * details.
	 * @param includes the collection of includes
	 * @param excludes the collection of excludes
	 * @since 7.0
	 */
	public ExceptionTypeFilter(@Nullable Collection<Class<? extends Throwable>> includes,
			@Nullable Collection<Class<? extends Throwable>> excludes) {

		super(includes, excludes);
	}

	/**
	 * Create a new {@code ExceptionTypeFilter} based on include and exclude
	 * collections.
	 * <p>See {@link InstanceFilter#InstanceFilter(Collection, Collection, boolean)
	 * InstanceFilter} for details.
	 * @param includes the collection of includes
	 * @param excludes the collection of excludes
	 * @param matchIfEmpty the matching result if the includes and the excludes
	 * collections are both {@code null} or empty
	 */
	public ExceptionTypeFilter(@Nullable Collection<? extends Class<? extends Throwable>> includes,
			@Nullable Collection<? extends Class<? extends Throwable>> excludes, boolean matchIfEmpty) {

		super(includes, excludes, matchIfEmpty);
	}


	/**
	 * Determine if the type of the supplied {@code exception} matches this filter.
	 * @since 7.0
	 * @see InstanceFilter#match(Object)
	 */
	public boolean match(Throwable exception) {
		return match(exception.getClass());
	}


	/**
	 * Determine if the specified {@code instance} matches the specified
	 * {@code candidate}.
	 * <p>By default, the two instances match if the {@code candidate} type is
	 * {@linkplain Class#isAssignableFrom(Class) is assignable from} the
	 * {@code instance} type.
	 * <p>Can be overridden by subclasses.
	 * @param instance the instance to check
	 * @param candidate a candidate defined by this filter
	 * @return {@code true} if the instance matches the candidate
	 */
	@Override
	protected boolean match(Class<? extends Throwable> instance, Class<? extends Throwable> candidate) {
		return candidate.isAssignableFrom(instance);
	}

}
