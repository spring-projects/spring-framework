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

package org.springframework.aop.support;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.ClassFilter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Static utility methods for composing {@link ClassFilter ClassFilters}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 11.11.2003
 * @see MethodMatchers
 * @see Pointcuts
 */
public abstract class ClassFilters {

	/**
	 * Match all classes that <i>either</i> (or both) of the given ClassFilters matches.
	 * @param cf1 the first ClassFilter
	 * @param cf2 the second ClassFilter
	 * @return a distinct ClassFilter that matches all classes that either
	 * of the given ClassFilter matches
	 */
	public static ClassFilter union(ClassFilter cf1, ClassFilter cf2) {
		Assert.notNull(cf1, "First ClassFilter must not be null");
		Assert.notNull(cf2, "Second ClassFilter must not be null");
		return new UnionClassFilter(new ClassFilter[] {cf1, cf2});
	}

	/**
	 * Match all classes that <i>either</i> (or all) of the given ClassFilters matches.
	 * @param classFilters the ClassFilters to match
	 * @return a distinct ClassFilter that matches all classes that either
	 * of the given ClassFilter matches
	 */
	public static ClassFilter union(ClassFilter[] classFilters) {
		Assert.notEmpty(classFilters, "ClassFilter array must not be empty");
		return new UnionClassFilter(classFilters);
	}

	/**
	 * Match all classes that <i>both</i> of the given ClassFilters match.
	 * @param cf1 the first ClassFilter
	 * @param cf2 the second ClassFilter
	 * @return a distinct ClassFilter that matches all classes that both
	 * of the given ClassFilter match
	 */
	public static ClassFilter intersection(ClassFilter cf1, ClassFilter cf2) {
		Assert.notNull(cf1, "First ClassFilter must not be null");
		Assert.notNull(cf2, "Second ClassFilter must not be null");
		return new IntersectionClassFilter(new ClassFilter[] {cf1, cf2});
	}

	/**
	 * Match all classes that <i>all</i> of the given ClassFilters match.
	 * @param classFilters the ClassFilters to match
	 * @return a distinct ClassFilter that matches all classes that both
	 * of the given ClassFilter match
	 */
	public static ClassFilter intersection(ClassFilter[] classFilters) {
		Assert.notEmpty(classFilters, "ClassFilter array must not be empty");
		return new IntersectionClassFilter(classFilters);
	}

	/**
	 * Return a class filter that represents the logical negation of the specified
	 * filter instance.
	 * @param classFilter the {@link ClassFilter} to negate
	 * @return a filter that represents the logical negation of the specified filter
	 * @since 6.1
	 */
	public static ClassFilter negate(ClassFilter classFilter) {
		Assert.notNull(classFilter, "ClassFilter must not be null");
		return new NegateClassFilter(classFilter);
	}


	/**
	 * ClassFilter implementation for a union of the given ClassFilters.
	 */
	@SuppressWarnings("serial")
	private static class UnionClassFilter implements ClassFilter, Serializable {

		private final ClassFilter[] filters;

		UnionClassFilter(ClassFilter[] filters) {
			this.filters = filters;
		}

		@Override
		public boolean matches(Class<?> clazz) {
			for (ClassFilter filter : this.filters) {
				if (filter.matches(clazz)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof UnionClassFilter that &&
					ObjectUtils.nullSafeEquals(this.filters, that.filters)));
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.filters);
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + Arrays.toString(this.filters);
		}
	}


	/**
	 * ClassFilter implementation for an intersection of the given ClassFilters.
	 */
	@SuppressWarnings("serial")
	private static class IntersectionClassFilter implements ClassFilter, Serializable {

		private final ClassFilter[] filters;

		IntersectionClassFilter(ClassFilter[] filters) {
			this.filters = filters;
		}

		@Override
		public boolean matches(Class<?> clazz) {
			for (ClassFilter filter : this.filters) {
				if (!filter.matches(clazz)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof IntersectionClassFilter that &&
					ObjectUtils.nullSafeEquals(this.filters, that.filters)));
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.filters);
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + Arrays.toString(this.filters);
		}
	}


	/**
	 * ClassFilter implementation for a logical negation of the given ClassFilter.
	 */
	@SuppressWarnings("serial")
	private static class NegateClassFilter implements ClassFilter, Serializable {

		private final ClassFilter original;

		NegateClassFilter(ClassFilter original) {
			this.original = original;
		}

		@Override
		public boolean matches(Class<?> clazz) {
			return !this.original.matches(clazz);
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof NegateClassFilter that &&
					this.original.equals(that.original)));
		}

		@Override
		public int hashCode() {
			return Objects.hash(getClass(), this.original);
		}

		@Override
		public String toString() {
			return "Negate " + this.original;
		}
	}

}
