/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.support.annotation;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationMatchingPointcut}.
 *
 * @author Sam Brannen
 * @since 5.1.10
 */
class AnnotationMatchingPointcutTests {

	@Test
	void classLevelPointcuts() {
		Pointcut pointcut1 = new AnnotationMatchingPointcut(Qualifier.class, true);
		Pointcut pointcut2 = new AnnotationMatchingPointcut(Qualifier.class, true);
		Pointcut pointcut3 = new AnnotationMatchingPointcut(Qualifier.class);

		assertThat(pointcut1.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
		assertThat(pointcut2.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
		assertThat(pointcut3.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
		assertThat(pointcut1.getClassFilter().toString()).contains(Qualifier.class.getName());

		assertThat(pointcut1.getMethodMatcher()).isEqualTo(MethodMatcher.TRUE);
		assertThat(pointcut2.getMethodMatcher()).isEqualTo(MethodMatcher.TRUE);
		assertThat(pointcut3.getMethodMatcher()).isEqualTo(MethodMatcher.TRUE);

		assertThat(pointcut1).isEqualTo(pointcut2);
		assertThat(pointcut1).isNotEqualTo(pointcut3);
		assertThat(pointcut1.hashCode()).isEqualTo(pointcut2.hashCode());
		// #1 and #3 have equivalent hash codes even though equals() returns false.
		assertThat(pointcut1.hashCode()).isEqualTo(pointcut3.hashCode());
		assertThat(pointcut1.toString()).isEqualTo(pointcut2.toString());
	}

	@Test
	void methodLevelPointcuts() {
		Pointcut pointcut1 = new AnnotationMatchingPointcut(null, Qualifier.class, true);
		Pointcut pointcut2 = new AnnotationMatchingPointcut(null, Qualifier.class, true);
		Pointcut pointcut3 = new AnnotationMatchingPointcut(null, Qualifier.class);

		assertThat(pointcut1.getClassFilter().getClass().getSimpleName()).isEqualTo("AnnotationCandidateClassFilter");
		assertThat(pointcut2.getClassFilter().getClass().getSimpleName()).isEqualTo("AnnotationCandidateClassFilter");
		assertThat(pointcut3.getClassFilter().getClass().getSimpleName()).isEqualTo("AnnotationCandidateClassFilter");
		assertThat(pointcut1.getClassFilter().toString()).contains(Qualifier.class.getName());

		assertThat(pointcut1.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
		assertThat(pointcut2.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
		assertThat(pointcut3.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);

		assertThat(pointcut1).isEqualTo(pointcut2);
		assertThat(pointcut1).isNotEqualTo(pointcut3);
		assertThat(pointcut1.hashCode()).isEqualTo(pointcut2.hashCode());
		// #1 and #3 have equivalent hash codes even though equals() returns false.
		assertThat(pointcut1.hashCode()).isEqualTo(pointcut3.hashCode());
		assertThat(pointcut1.toString()).isEqualTo(pointcut2.toString());
	}

	@Test
	void classLevelAndMethodLevelPointcuts() {
		Pointcut pointcut1 = new AnnotationMatchingPointcut(Qualifier.class, Qualifier.class, true);
		Pointcut pointcut2 = new AnnotationMatchingPointcut(Qualifier.class, Qualifier.class, true);
		Pointcut pointcut3 = new AnnotationMatchingPointcut(Qualifier.class, Qualifier.class);

		assertThat(pointcut1.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
		assertThat(pointcut2.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
		assertThat(pointcut3.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
		assertThat(pointcut1.getClassFilter().toString()).contains(Qualifier.class.getName());

		assertThat(pointcut1.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
		assertThat(pointcut2.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
		assertThat(pointcut3.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
		assertThat(pointcut1.getMethodMatcher().toString()).contains(Qualifier.class.getName());

		assertThat(pointcut1).isEqualTo(pointcut2);
		assertThat(pointcut1).isNotEqualTo(pointcut3);
		assertThat(pointcut1.hashCode()).isEqualTo(pointcut2.hashCode());
		// #1 and #3 have equivalent hash codes even though equals() returns false.
		assertThat(pointcut1.hashCode()).isEqualTo(pointcut3.hashCode());
		assertThat(pointcut1.toString()).isEqualTo(pointcut2.toString());
	}


	@Test
	void methodMatcherHandlesInheritedAndOverriddenMethods() throws Exception {
		AnnotationMethodMatcher matcher = new AnnotationMethodMatcher(Qualifier.class, true);
		Method objectToString = Object.class.getMethod("toString");
		Method baseUnannotated = BaseService.class.getMethod("unannotated");
		Method baseOverridden = BaseService.class.getMethod("overridden");

		assertThat(matcher.matches(objectToString, SubService.class)).isFalse();
		assertThat(matcher.matches(baseUnannotated, SubService.class)).isFalse();
		assertThat(matcher.matches(baseOverridden, SubService.class)).isTrue();
	}

	static class BaseService {

		public void unannotated() {
		}

		public void overridden() {
		}
	}

	static class SubService extends BaseService {

		@Override
		@Qualifier
		public void overridden() {
		}
	}

}
