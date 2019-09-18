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

import org.junit.Test;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AnnotationMatchingPointcut}.
 *
 * @author Sam Brannen
 * @since 5.1.10
 */
public class AnnotationMatchingPointcutTests {

	@Test
	public void classLevelPointcuts() {
		Pointcut pointcut1 = new AnnotationMatchingPointcut(Qualifier.class, true);
		Pointcut pointcut2 = new AnnotationMatchingPointcut(Qualifier.class, true);
		Pointcut pointcut3 = new AnnotationMatchingPointcut(Qualifier.class);

		assertEquals(AnnotationClassFilter.class, pointcut1.getClassFilter().getClass());
		assertEquals(AnnotationClassFilter.class, pointcut2.getClassFilter().getClass());
		assertEquals(AnnotationClassFilter.class, pointcut3.getClassFilter().getClass());
		assertTrue(pointcut1.getClassFilter().toString().contains(Qualifier.class.getName()));

		assertEquals(MethodMatcher.TRUE, pointcut1.getMethodMatcher());
		assertEquals(MethodMatcher.TRUE, pointcut2.getMethodMatcher());
		assertEquals(MethodMatcher.TRUE, pointcut3.getMethodMatcher());

		assertEquals(pointcut1, pointcut2);
		assertNotEquals(pointcut1, pointcut3);
		assertEquals(pointcut1.hashCode(), pointcut2.hashCode());
		// #1 and #3 have equivalent hash codes even though equals() returns false.
		assertEquals(pointcut1.hashCode(), pointcut3.hashCode());
		assertEquals(pointcut1.toString(), pointcut2.toString());
	}

	@Test
	public void methodLevelPointcuts() {
		Pointcut pointcut1 = new AnnotationMatchingPointcut(null, Qualifier.class, true);
		Pointcut pointcut2 = new AnnotationMatchingPointcut(null, Qualifier.class, true);
		Pointcut pointcut3 = new AnnotationMatchingPointcut(null, Qualifier.class);

		assertEquals(ClassFilter.TRUE, pointcut1.getClassFilter());
		assertEquals(ClassFilter.TRUE, pointcut2.getClassFilter());
		assertEquals(ClassFilter.TRUE, pointcut3.getClassFilter());
		assertEquals("ClassFilter.TRUE", pointcut1.getClassFilter().toString());

		assertEquals(AnnotationMethodMatcher.class, pointcut1.getMethodMatcher().getClass());
		assertEquals(AnnotationMethodMatcher.class, pointcut2.getMethodMatcher().getClass());
		assertEquals(AnnotationMethodMatcher.class, pointcut3.getMethodMatcher().getClass());

		assertEquals(pointcut1, pointcut2);
		assertNotEquals(pointcut1, pointcut3);
		assertEquals(pointcut1.hashCode(), pointcut2.hashCode());
		// #1 and #3 have equivalent hash codes even though equals() returns false.
		assertEquals(pointcut1.hashCode(), pointcut3.hashCode());
		assertEquals(pointcut1.toString(), pointcut2.toString());
	}

	@Test
	public void classLevelAndMethodLevelPointcuts() {
		Pointcut pointcut1 = new AnnotationMatchingPointcut(Qualifier.class, Qualifier.class, true);
		Pointcut pointcut2 = new AnnotationMatchingPointcut(Qualifier.class, Qualifier.class, true);
		Pointcut pointcut3 = new AnnotationMatchingPointcut(Qualifier.class, Qualifier.class);

		assertEquals(AnnotationClassFilter.class, pointcut1.getClassFilter().getClass());
		assertEquals(AnnotationClassFilter.class, pointcut2.getClassFilter().getClass());
		assertEquals(AnnotationClassFilter.class, pointcut3.getClassFilter().getClass());
		assertTrue(pointcut1.getClassFilter().toString().contains(Qualifier.class.getName()));

		assertEquals(AnnotationMethodMatcher.class, pointcut1.getMethodMatcher().getClass());
		assertEquals(AnnotationMethodMatcher.class, pointcut2.getMethodMatcher().getClass());
		assertEquals(AnnotationMethodMatcher.class, pointcut3.getMethodMatcher().getClass());
		assertTrue(pointcut1.getMethodMatcher().toString().contains(Qualifier.class.getName()));

		assertEquals(pointcut1, pointcut2);
		assertNotEquals(pointcut1, pointcut3);
		assertEquals(pointcut1.hashCode(), pointcut2.hashCode());
		// #1 and #3 have equivalent hash codes even though equals() returns false.
		assertEquals(pointcut1.hashCode(), pointcut3.hashCode());
		assertEquals(pointcut1.toString(), pointcut2.toString());
	}

}
