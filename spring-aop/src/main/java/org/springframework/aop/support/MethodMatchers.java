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
import java.lang.reflect.Method;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.util.Assert;

/**
 * Static utility methods for composing {@link MethodMatcher MethodMatchers}.
 *
 * <p>A MethodMatcher may be evaluated statically (based on method and target
 * class) or need further evaluation dynamically (based on arguments at the
 * time of method invocation).
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 11.11.2003
 * @see ClassFilters
 * @see Pointcuts
 */
public abstract class MethodMatchers {

	/**
	 * Match all methods that <i>either</i> (or both) of the given MethodMatchers matches.
	 * @param mm1 the first MethodMatcher
	 * @param mm2 the second MethodMatcher
	 * @return a distinct MethodMatcher that matches all methods that either
	 * of the given MethodMatchers matches
	 */
	public static MethodMatcher union(MethodMatcher mm1, MethodMatcher mm2) {
		return (mm1 instanceof IntroductionAwareMethodMatcher || mm2 instanceof IntroductionAwareMethodMatcher ?
				new UnionIntroductionAwareMethodMatcher(mm1, mm2) : new UnionMethodMatcher(mm1, mm2));
	}

	/**
	 * Match all methods that <i>either</i> (or both) of the given MethodMatchers matches.
	 * @param mm1 the first MethodMatcher
	 * @param cf1 the corresponding ClassFilter for the first MethodMatcher
	 * @param mm2 the second MethodMatcher
	 * @param cf2 the corresponding ClassFilter for the second MethodMatcher
	 * @return a distinct MethodMatcher that matches all methods that either
	 * of the given MethodMatchers matches
	 */
	static MethodMatcher union(MethodMatcher mm1, ClassFilter cf1, MethodMatcher mm2, ClassFilter cf2) {
		return (mm1 instanceof IntroductionAwareMethodMatcher || mm2 instanceof IntroductionAwareMethodMatcher ?
				new ClassFilterAwareUnionIntroductionAwareMethodMatcher(mm1, cf1, mm2, cf2) :
				new ClassFilterAwareUnionMethodMatcher(mm1, cf1, mm2, cf2));
	}

	/**
	 * Match all methods that <i>both</i> of the given MethodMatchers match.
	 * @param mm1 the first MethodMatcher
	 * @param mm2 the second MethodMatcher
	 * @return a distinct MethodMatcher that matches all methods that both
	 * of the given MethodMatchers match
	 */
	public static MethodMatcher intersection(MethodMatcher mm1, MethodMatcher mm2) {
		return (mm1 instanceof IntroductionAwareMethodMatcher || mm2 instanceof IntroductionAwareMethodMatcher ?
				new IntersectionIntroductionAwareMethodMatcher(mm1, mm2) : new IntersectionMethodMatcher(mm1, mm2));
	}

	/**
	 * Return a method matcher that represents the logical negation of the specified
	 * matcher instance.
	 * @param methodMatcher the {@link MethodMatcher} to negate
	 * @return a matcher that represents the logical negation of the specified matcher
	 * @since 6.1
	 */
	public static MethodMatcher negate(MethodMatcher methodMatcher) {
		Assert.notNull(methodMatcher, "MethodMatcher must not be null");
		return new NegateMethodMatcher(methodMatcher);
	}

	/**
	 * Apply the given MethodMatcher to the given Method, supporting an
	 * {@link org.springframework.aop.IntroductionAwareMethodMatcher}
	 * (if applicable).
	 * @param mm the MethodMatcher to apply (may be an IntroductionAwareMethodMatcher)
	 * @param method the candidate method
	 * @param targetClass the target class
	 * @param hasIntroductions {@code true} if the object on whose behalf we are
	 * asking is the subject on one or more introductions; {@code false} otherwise
	 * @return whether this method matches statically
	 */
	public static boolean matches(MethodMatcher mm, Method method, Class<?> targetClass, boolean hasIntroductions) {
		Assert.notNull(mm, "MethodMatcher must not be null");
		return (mm instanceof IntroductionAwareMethodMatcher iamm ?
				iamm.matches(method, targetClass, hasIntroductions) :
				mm.matches(method, targetClass));
	}


	/**
	 * MethodMatcher implementation for a union of two given MethodMatchers.
	 */
	@SuppressWarnings("serial")
	private static class UnionMethodMatcher implements MethodMatcher, Serializable {

		protected final MethodMatcher mm1;

		protected final MethodMatcher mm2;

		public UnionMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
			Assert.notNull(mm1, "First MethodMatcher must not be null");
			Assert.notNull(mm2, "Second MethodMatcher must not be null");
			this.mm1 = mm1;
			this.mm2 = mm2;
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return (matchesClass1(targetClass) && this.mm1.matches(method, targetClass)) ||
					(matchesClass2(targetClass) && this.mm2.matches(method, targetClass));
		}

		protected boolean matchesClass1(Class<?> targetClass) {
			return true;
		}

		protected boolean matchesClass2(Class<?> targetClass) {
			return true;
		}

		@Override
		public boolean isRuntime() {
			return this.mm1.isRuntime() || this.mm2.isRuntime();
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, @Nullable Object... args) {
			return this.mm1.matches(method, targetClass, args) || this.mm2.matches(method, targetClass, args);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof UnionMethodMatcher that &&
					this.mm1.equals(that.mm1) && this.mm2.equals(that.mm2)));
		}

		@Override
		public int hashCode() {
			return 37 * this.mm1.hashCode() + this.mm2.hashCode();
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + this.mm1 + ", " + this.mm2;
		}
	}


	/**
	 * MethodMatcher implementation for a union of two given MethodMatchers
	 * of which at least one is an IntroductionAwareMethodMatcher.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	private static class UnionIntroductionAwareMethodMatcher extends UnionMethodMatcher
			implements IntroductionAwareMethodMatcher {

		public UnionIntroductionAwareMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
			super(mm1, mm2);
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
			return (matchesClass1(targetClass) && MethodMatchers.matches(this.mm1, method, targetClass, hasIntroductions)) ||
					(matchesClass2(targetClass) && MethodMatchers.matches(this.mm2, method, targetClass, hasIntroductions));
		}
	}


	/**
	 * MethodMatcher implementation for a union of two given MethodMatchers,
	 * supporting an associated ClassFilter per MethodMatcher.
	 */
	@SuppressWarnings("serial")
	private static class ClassFilterAwareUnionMethodMatcher extends UnionMethodMatcher {

		private final ClassFilter cf1;

		private final ClassFilter cf2;

		public ClassFilterAwareUnionMethodMatcher(MethodMatcher mm1, ClassFilter cf1, MethodMatcher mm2, ClassFilter cf2) {
			super(mm1, mm2);
			this.cf1 = cf1;
			this.cf2 = cf2;
		}

		@Override
		protected boolean matchesClass1(Class<?> targetClass) {
			return this.cf1.matches(targetClass);
		}

		@Override
		protected boolean matchesClass2(Class<?> targetClass) {
			return this.cf2.matches(targetClass);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			ClassFilter otherCf1 = ClassFilter.TRUE;
			ClassFilter otherCf2 = ClassFilter.TRUE;
			if (other instanceof ClassFilterAwareUnionMethodMatcher cfa) {
				otherCf1 = cfa.cf1;
				otherCf2 = cfa.cf2;
			}
			return (this.cf1.equals(otherCf1) && this.cf2.equals(otherCf2));
		}

		@Override
		public int hashCode() {
			// Allow for matching with regular UnionMethodMatcher by providing same hash...
			return super.hashCode();
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + this.cf1 + ", " + this.mm1 + ", " + this.cf2 + ", " + this.mm2;
		}
	}


	/**
	 * MethodMatcher implementation for a union of two given MethodMatchers
	 * of which at least one is an IntroductionAwareMethodMatcher,
	 * supporting an associated ClassFilter per MethodMatcher.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	private static class ClassFilterAwareUnionIntroductionAwareMethodMatcher extends ClassFilterAwareUnionMethodMatcher
			implements IntroductionAwareMethodMatcher {

		public ClassFilterAwareUnionIntroductionAwareMethodMatcher(
				MethodMatcher mm1, ClassFilter cf1, MethodMatcher mm2, ClassFilter cf2) {

			super(mm1, cf1, mm2, cf2);
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
			return (matchesClass1(targetClass) && MethodMatchers.matches(this.mm1, method, targetClass, hasIntroductions)) ||
					(matchesClass2(targetClass) && MethodMatchers.matches(this.mm2, method, targetClass, hasIntroductions));
		}
	}


	/**
	 * MethodMatcher implementation for an intersection of two given MethodMatchers.
	 */
	@SuppressWarnings("serial")
	private static class IntersectionMethodMatcher implements MethodMatcher, Serializable {

		protected final MethodMatcher mm1;

		protected final MethodMatcher mm2;

		public IntersectionMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
			Assert.notNull(mm1, "First MethodMatcher must not be null");
			Assert.notNull(mm2, "Second MethodMatcher must not be null");
			this.mm1 = mm1;
			this.mm2 = mm2;
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return (this.mm1.matches(method, targetClass) && this.mm2.matches(method, targetClass));
		}

		@Override
		public boolean isRuntime() {
			return (this.mm1.isRuntime() || this.mm2.isRuntime());
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, @Nullable Object... args) {
			// Because a dynamic intersection may be composed of a static and dynamic part,
			// we must avoid calling the 3-arg matches method on a dynamic matcher, as
			// it will probably be an unsupported operation.
			boolean aMatches = (this.mm1.isRuntime() ?
					this.mm1.matches(method, targetClass, args) : this.mm1.matches(method, targetClass));
			boolean bMatches = (this.mm2.isRuntime() ?
					this.mm2.matches(method, targetClass, args) : this.mm2.matches(method, targetClass));
			return aMatches && bMatches;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof IntersectionMethodMatcher that &&
					this.mm1.equals(that.mm1) && this.mm2.equals(that.mm2)));
		}

		@Override
		public int hashCode() {
			return 37 * this.mm1.hashCode() + this.mm2.hashCode();
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + this.mm1 + ", " + this.mm2;
		}
	}


	/**
	 * MethodMatcher implementation for an intersection of two given MethodMatchers
	 * of which at least one is an IntroductionAwareMethodMatcher.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	private static class IntersectionIntroductionAwareMethodMatcher extends IntersectionMethodMatcher
			implements IntroductionAwareMethodMatcher {

		public IntersectionIntroductionAwareMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
			super(mm1, mm2);
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
			return (MethodMatchers.matches(this.mm1, method, targetClass, hasIntroductions) &&
					MethodMatchers.matches(this.mm2, method, targetClass, hasIntroductions));
		}
	}


	@SuppressWarnings("serial")
	private static class NegateMethodMatcher implements MethodMatcher, Serializable {

		private final MethodMatcher original;

		NegateMethodMatcher(MethodMatcher original) {
			this.original = original;
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return !this.original.matches(method, targetClass);
		}

		@Override
		public boolean isRuntime() {
			return this.original.isRuntime();
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, @Nullable Object... args) {
			return !this.original.matches(method, targetClass, args);
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof NegateMethodMatcher that &&
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
