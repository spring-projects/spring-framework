/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.util.Assert;

/**
 * Convenient class for building up pointcuts. All methods return
 * ComposablePointcut, so we can use a concise idiom like:
 *
 * {@code
 * Pointcut pc = new ComposablePointcut().union(classFilter).intersection(methodMatcher).intersection(pointcut);
 * }
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 11.11.2003
 * @see Pointcuts
 */
public class ComposablePointcut implements Pointcut, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = -2743223737633663832L;

	private ClassFilter classFilter;

	private MethodMatcher methodMatcher;


	/**
	 * Create a default ComposablePointcut, with {@code ClassFilter.TRUE}
	 * and {@code MethodMatcher.TRUE}.
	 */
	public ComposablePointcut() {
		this.classFilter = ClassFilter.TRUE;
		this.methodMatcher = MethodMatcher.TRUE;
	}

	/**
	 * Create a ComposablePointcut based on the given Pointcut.
	 * @param pointcut the original Pointcut
	 */
	public ComposablePointcut(Pointcut pointcut) {
		Assert.notNull(pointcut, "Pointcut must not be null");
		this.classFilter = pointcut.getClassFilter();
		this.methodMatcher = pointcut.getMethodMatcher();
	}

	/**
	 * Create a ComposablePointcut for the given ClassFilter,
	 * with {@code MethodMatcher.TRUE}.
	 * @param classFilter the ClassFilter to use
	 */
	public ComposablePointcut(ClassFilter classFilter) {
		Assert.notNull(classFilter, "ClassFilter must not be null");
		this.classFilter = classFilter;
		this.methodMatcher = MethodMatcher.TRUE;
	}

	/**
	 * Create a ComposablePointcut for the given MethodMatcher,
	 * with {@code ClassFilter.TRUE}.
	 * @param methodMatcher the MethodMatcher to use
	 */
	public ComposablePointcut(MethodMatcher methodMatcher) {
		Assert.notNull(methodMatcher, "MethodMatcher must not be null");
		this.classFilter = ClassFilter.TRUE;
		this.methodMatcher = methodMatcher;
	}

	/**
	 * Create a ComposablePointcut for the given ClassFilter and MethodMatcher.
	 * @param classFilter the ClassFilter to use
	 * @param methodMatcher the MethodMatcher to use
	 */
	public ComposablePointcut(ClassFilter classFilter, MethodMatcher methodMatcher) {
		Assert.notNull(classFilter, "ClassFilter must not be null");
		Assert.notNull(methodMatcher, "MethodMatcher must not be null");
		this.classFilter = classFilter;
		this.methodMatcher = methodMatcher;
	}


	/**
	 * Apply a union with the given ClassFilter.
	 * @param other the ClassFilter to apply a union with
	 * @return this composable pointcut (for call chaining)
	 */
	public ComposablePointcut union(ClassFilter other) {
		this.classFilter = ClassFilters.union(this.classFilter, other);
		return this;
	}

	/**
	 * Apply an intersection with the given ClassFilter.
	 * @param other the ClassFilter to apply an intersection with
	 * @return this composable pointcut (for call chaining)
	 */
	public ComposablePointcut intersection(ClassFilter other) {
		this.classFilter = ClassFilters.intersection(this.classFilter, other);
		return this;
	}

	/**
	 * Apply a union with the given MethodMatcher.
	 * @param other the MethodMatcher to apply a union with
	 * @return this composable pointcut (for call chaining)
	 */
	public ComposablePointcut union(MethodMatcher other) {
		this.methodMatcher = MethodMatchers.union(this.methodMatcher, other);
		return this;
	}

	/**
	 * Apply an intersection with the given MethodMatcher.
	 * @param other the MethodMatcher to apply an intersection with
	 * @return this composable pointcut (for call chaining)
	 */
	public ComposablePointcut intersection(MethodMatcher other) {
		this.methodMatcher = MethodMatchers.intersection(this.methodMatcher, other);
		return this;
	}

	/**
	 * Apply a union with the given Pointcut.
	 * <p>Note that for a Pointcut union, methods will only match if their
	 * original ClassFilter (from the originating Pointcut) matches as well.
	 * MethodMatchers and ClassFilters from different Pointcuts will never
	 * get interleaved with each other.
	 * @param other the Pointcut to apply a union with
	 * @return this composable pointcut (for call chaining)
	 */
	public ComposablePointcut union(Pointcut other) {
		this.methodMatcher = MethodMatchers.union(
				this.methodMatcher, this.classFilter, other.getMethodMatcher(), other.getClassFilter());
		this.classFilter = ClassFilters.union(this.classFilter, other.getClassFilter());
		return this;
	}

	/**
	 * Apply an intersection with the given Pointcut.
	 * @param other the Pointcut to apply an intersection with
	 * @return this composable pointcut (for call chaining)
	 */
	public ComposablePointcut intersection(Pointcut other) {
		this.classFilter = ClassFilters.intersection(this.classFilter, other.getClassFilter());
		this.methodMatcher = MethodMatchers.intersection(this.methodMatcher, other.getMethodMatcher());
		return this;
	}


	@Override
	public ClassFilter getClassFilter() {
		return this.classFilter;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return this.methodMatcher;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ComposablePointcut)) {
			return false;
		}
		ComposablePointcut otherPointcut = (ComposablePointcut) other;
		return (this.classFilter.equals(otherPointcut.classFilter) &&
				this.methodMatcher.equals(otherPointcut.methodMatcher));
	}

	@Override
	public int hashCode() {
		return this.classFilter.hashCode() * 37 + this.methodMatcher.hashCode();
	}

	@Override
	public String toString() {
		return "ComposablePointcut: " + this.classFilter + ", " +this.methodMatcher;
	}

}
