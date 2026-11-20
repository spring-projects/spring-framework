/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.support.annotation;

import java.lang.annotation.Annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * Simple {@link Pointcut} that looks for a specific annotation being present on a
 * {@linkplain #forClassAnnotation class} or {@linkplain #forMethodAnnotation method}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0
 * @see AnnotationClassFilter
 * @see AnnotationMethodMatcher
 */
public class AnnotationMatchingPointcut implements Pointcut {

	private final ClassFilter classFilter;

	private final MethodMatcher methodMatcher;


	/**
	 * Create a new AnnotationMatchingPointcut for the given annotation type.
	 * @param classAnnotationType the annotation type to look for at the class level
	 */
	public AnnotationMatchingPointcut(Class<? extends Annotation> classAnnotationType) {
		this(classAnnotationType, false);
	}

	/**
	 * Create a new AnnotationMatchingPointcut for the given annotation type.
	 * @param classAnnotationType the annotation type to look for at the class level
	 * @param checkInherited whether to also check the superclasses and interfaces
	 * as well as meta-annotations for the annotation type
	 * @see AnnotationClassFilter#AnnotationClassFilter(Class, boolean)
	 */
	public AnnotationMatchingPointcut(Class<? extends Annotation> classAnnotationType, boolean checkInherited) {
		this.classFilter = new AnnotationClassFilter(classAnnotationType, checkInherited);
		this.methodMatcher = MethodMatcher.TRUE;
	}

	/**
	 * Create a new AnnotationMatchingPointcut for the given annotation types.
	 * @param classAnnotationType the annotation type to look for at the class level
	 * (can be {@code null})
	 * @param methodAnnotationType the annotation type to look for at the method level
	 * (can be {@code null})
	 */
	public AnnotationMatchingPointcut(@Nullable Class<? extends Annotation> classAnnotationType,
			@Nullable Class<? extends Annotation> methodAnnotationType) {

		this(classAnnotationType, methodAnnotationType, false);
	}

	/**
	 * Create a new AnnotationMatchingPointcut for the given annotation types.
	 * @param classAnnotationType the annotation type to look for at the class level
	 * (can be {@code null})
	 * @param methodAnnotationType the annotation type to look for at the method level
	 * (can be {@code null})
	 * @param checkInherited whether to also check the superclasses and interfaces
	 * as well as meta-annotations for the annotation type
	 * @since 5.0
	 * @see AnnotationClassFilter#AnnotationClassFilter(Class, boolean)
	 * @see AnnotationMethodMatcher#AnnotationMethodMatcher(Class, boolean)
	 */
	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	public AnnotationMatchingPointcut(@Nullable Class<? extends Annotation> classAnnotationType,
			@Nullable Class<? extends Annotation> methodAnnotationType, boolean checkInherited) {

		Assert.isTrue((classAnnotationType != null || methodAnnotationType != null),
				"Either Class annotation type or Method annotation type needs to be specified (or both)");

		if (classAnnotationType != null) {
			this.classFilter = new AnnotationClassFilter(classAnnotationType, checkInherited);
		}
		else {
			this.classFilter = new AnnotationCandidateClassFilter(methodAnnotationType);
		}

		if (methodAnnotationType != null) {
			this.methodMatcher = new AnnotationMethodMatcher(methodAnnotationType, checkInherited);
		}
		else {
			this.methodMatcher = MethodMatcher.TRUE;
		}
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
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof AnnotationMatchingPointcut otherPointcut &&
				this.classFilter.equals(otherPointcut.classFilter) &&
				this.methodMatcher.equals(otherPointcut.methodMatcher)));
	}

	@Override
	public int hashCode() {
		return this.classFilter.hashCode() * 37 + this.methodMatcher.hashCode();
	}

	@Override
	public String toString() {
		return "AnnotationMatchingPointcut: " + this.classFilter + ", " + this.methodMatcher;
	}

	/**
	 * Factory method for an AnnotationMatchingPointcut that matches
	 * for the specified annotation at the class level.
	 * @param annotationType the annotation type to look for at the class level
	 * @return the corresponding AnnotationMatchingPointcut
	 */
	public static AnnotationMatchingPointcut forClassAnnotation(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		return new AnnotationMatchingPointcut(annotationType);
	}

	/**
	 * Factory method for an AnnotationMatchingPointcut that matches
	 * for the specified annotation at the method level.
	 * @param annotationType the annotation type to look for at the method level
	 * @return the corresponding AnnotationMatchingPointcut
	 */
	public static AnnotationMatchingPointcut forMethodAnnotation(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		return new AnnotationMatchingPointcut(null, annotationType);
	}


	/**
	 * {@link ClassFilter} that delegates to {@link AnnotationUtils#isCandidateClass}
	 * for filtering classes whose methods are not worth searching to begin with.
	 * @since 5.2
	 */
	private static class AnnotationCandidateClassFilter implements ClassFilter {

		private final Class<? extends Annotation> annotationType;

		AnnotationCandidateClassFilter(Class<? extends Annotation> annotationType) {
			this.annotationType = annotationType;
		}

		@Override
		public boolean matches(Class<?> clazz) {
			return AnnotationUtils.isCandidateClass(clazz, this.annotationType);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof AnnotationCandidateClassFilter that &&
					this.annotationType.equals(that.annotationType)));
		}

		@Override
		public int hashCode() {
			return this.annotationType.hashCode();
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + this.annotationType;
		}

	}

}
