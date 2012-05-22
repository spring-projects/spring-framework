/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.support;

import java.io.Serializable;

import org.springframework.aop.ClassFilter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Static utility methods for composing
 * {@link org.springframework.aop.ClassFilter ClassFilters}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
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
	 * ClassFilter implementation for a union of the given ClassFilters.
	 */
	private static class UnionClassFilter implements ClassFilter, Serializable {

		private ClassFilter[] filters;

		public UnionClassFilter(ClassFilter[] filters) {
			this.filters = filters;
		}

		public boolean matches(Class<?> clazz) {
			for (int i = 0; i < this.filters.length; i++) {
				if (this.filters[i].matches(clazz)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof UnionClassFilter &&
					ObjectUtils.nullSafeEquals(this.filters, ((UnionClassFilter) other).filters)));
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.filters);
		}
	}


	/**
	 * ClassFilter implementation for an intersection of the given ClassFilters.
	 */
	private static class IntersectionClassFilter implements ClassFilter, Serializable {

		private ClassFilter[] filters;

		public IntersectionClassFilter(ClassFilter[] filters) {
			this.filters = filters;
		}

		public boolean matches(Class<?> clazz) {
			for (int i = 0; i < this.filters.length; i++) {
				if (!this.filters[i].matches(clazz)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof IntersectionClassFilter &&
					ObjectUtils.nullSafeEquals(this.filters, ((IntersectionClassFilter) other).filters)));
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.filters);
		}
	}

}
