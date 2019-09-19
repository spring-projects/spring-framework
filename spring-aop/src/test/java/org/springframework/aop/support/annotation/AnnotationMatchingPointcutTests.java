/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.support.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AnnotationMatchingPointcut}.
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

}
