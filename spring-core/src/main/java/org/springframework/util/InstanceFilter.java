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
import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * A simple instance filter that checks if a given instance matches based on
 * collections of includes and excludes.
 *
 * <p>Subclasses may override {@link #match(Object, Object)} to provide a custom
 * matching algorithm.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.1
 * @param <T> the instance type
 */
public class InstanceFilter<T> {

	protected final Collection<? extends T> includes;

	protected final Collection<? extends T> excludes;

	protected final boolean matchIfEmpty;


	/**
	 * Create a new {@code InstanceFilter} based on include and exclude collections,
	 * with the {@code matchIfEmpty} flag set to {@code true}.
	 * <p>See {@link #InstanceFilter(Collection, Collection, boolean)} for details.
	 * @param includes the collection of includes
	 * @param excludes the collection of excludes
	 * @since 7.0
	 */
	public InstanceFilter(@Nullable Collection<? extends T> includes,
			@Nullable Collection<? extends T> excludes) {

		this(includes, excludes, true);
	}

	/**
	 * Create a new {@code InstanceFilter} based on include and exclude collections.
	 * <p>A particular element will match if it <em>matches</em> one of the elements
	 * in the {@code includes} list and does not match one of the elements in the
	 * {@code excludes} list.
	 * <p>Subclasses may redefine what matching means. By default, an element
	 * {@linkplain #match(Object, Object) matches} another if the two elements are
	 * {@linkplain Object#equals(Object) equal}.
	 * <p>If both collections are empty, {@code matchIfEmpty} defines if an element
	 * matches or not.
	 * @param includes the collection of includes
	 * @param excludes the collection of excludes
	 * @param matchIfEmpty the matching result if the includes and the excludes
	 * collections are both {@code null} or empty
	 */
	public InstanceFilter(@Nullable Collection<? extends T> includes,
			@Nullable Collection<? extends T> excludes, boolean matchIfEmpty) {

		this.includes = (includes != null ? Collections.unmodifiableCollection(includes) : Set.of());
		this.excludes = (excludes != null ? Collections.unmodifiableCollection(excludes) : Set.of());
		this.matchIfEmpty = matchIfEmpty;
	}


	/**
	 * Determine if the specified {@code instance} matches this filter.
	 */
	public boolean match(T instance) {
		Assert.notNull(instance, "Instance to match must not be null");

		boolean emptyIncludes = this.includes.isEmpty();
		boolean emptyExcludes = this.excludes.isEmpty();

		if (emptyIncludes && emptyExcludes) {
			return this.matchIfEmpty;
		}
		if (!emptyExcludes && match(instance, this.excludes)) {
			return false;
		}
		return (emptyIncludes || match(instance, this.includes));
	}

	/**
	 * Determine if the specified {@code instance} matches the specified
	 * {@code candidate}.
	 * <p>By default, the two instances match if they are
	 * {@linkplain Object#equals(Object) equal}.
	 * <p>Can be overridden by subclasses.
	 * @param instance the instance to check
	 * @param candidate a candidate defined by this filter
	 * @return {@code true} if the instance matches the candidate
	 */
	protected boolean match(T instance, T candidate) {
		return instance.equals(candidate);
	}

	/**
	 * Determine if the specified {@code instance} matches one of the candidates.
	 * @param instance the instance to check
	 * @param candidates the collection of candidates
	 * @return {@code true} if the instance matches; {@code false} if the
	 * candidates collection is empty or there is no match
	 */
	protected boolean match(T instance, Collection<? extends T> candidates) {
		for (T candidate : candidates) {
			if (match(instance, candidate)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(": includes=").append(this.includes);
		sb.append(", excludes=").append(this.excludes);
		sb.append(", matchIfEmpty=").append(this.matchIfEmpty);
		return sb.toString();
	}

}
