/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.util;

import java.util.Collection;
import java.util.Collections;

import org.springframework.lang.Nullable;

/**
 * A simple instance filter that checks if a given instance match based on
 * a collection of includes and excludes element.
 *
 * <p>Subclasses may want to override {@link #match(Object, Object)} to provide
 * a custom matching algorithm.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <T> the instance type
 */
public class InstanceFilter<T> {

	private final Collection<? extends T> includes;

	private final Collection<? extends T> excludes;

	private final boolean matchIfEmpty;


	/**
	 * Create a new instance based on includes/excludes collections.
	 * <p>A particular element will match if it "matches" the one of the element in the
	 * includes list and  does not match one of the element in the excludes list.
	 * <p>Subclasses may redefine what matching means. By default, an element match with
	 * another if it is equals according to {@link Object#equals(Object)}
	 * <p>If both collections are empty, {@code matchIfEmpty} defines if
	 * an element matches or not.
	 * @param includes the collection of includes
	 * @param excludes the collection of excludes
	 * @param matchIfEmpty the matching result if both the includes and the excludes
	 * collections are empty
	 */
	public InstanceFilter(@Nullable Collection<? extends T> includes,
			@Nullable Collection<? extends T> excludes, boolean matchIfEmpty) {

		this.includes = (includes != null ? includes : Collections.emptyList());
		this.excludes = (excludes != null ? excludes : Collections.emptyList());
		this.matchIfEmpty = matchIfEmpty;
	}


	/**
	 * Determine if the specified {code instance} matches this filter.
	 */
	public boolean match(T instance) {
		Assert.notNull(instance, "Instance to match must not be null");

		boolean includesSet = !this.includes.isEmpty();
		boolean excludesSet = !this.excludes.isEmpty();
		if (!includesSet && !excludesSet) {
			return this.matchIfEmpty;
		}

		boolean matchIncludes = match(instance, this.includes);
		boolean matchExcludes = match(instance, this.excludes);
		if (!includesSet) {
			return !matchExcludes;
		}
		if (!excludesSet) {
			return matchIncludes;
		}
		return matchIncludes && !matchExcludes;
	}

	/**
	 * Determine if the specified {@code instance} is equal to the
	 * specified {@code candidate}.
	 * @param instance the instance to handle
	 * @param candidate a candidate defined by this filter
	 * @return {@code true} if the instance matches the candidate
	 */
	protected boolean match(T instance, T candidate) {
		return instance.equals(candidate);
	}

	/**
	 * Determine if the specified {@code instance} matches one of the candidates.
	 * <p>If the candidates collection is {@code null}, returns {@code false}.
	 * @param instance the instance to check
	 * @param candidates a list of candidates
	 * @return {@code true} if the instance match or the candidates collection is null
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
