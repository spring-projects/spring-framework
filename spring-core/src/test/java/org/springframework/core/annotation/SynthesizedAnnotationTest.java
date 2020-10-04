/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This is an example of using
 * {@link AnnotatedElementUtils#getMergedAnnotationWithMultipleAliases(AnnotatedElement, Class)}
 * instead of {@link AnnotatedElementUtils#getMergedAnnotation(AnnotatedElement, Class)}.
 *
 * @author ZiCheng Zhang
 * @date 2020/10/03
 */
public class SynthesizedAnnotationTest {

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Test1 {

		@AliasFor("test12")
		String test1() default "test1";

		@AliasFor("test1")
		String test12() default "test1";
	}

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Test1
	public @interface Test2 {
		@AliasFor(annotation = Test1.class)
		String test1() default "test2";
	}

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Test2
	public @interface Test3 {

		@AliasFor(annotation = Test2.class, attribute = "test1")
		String test3() default "test3";
	}

	@Test3(test3 = "override the method")
	public static class Element {
	}

	@Test1(test1 = "override the method")
	public static class Element2 {
	}

	@Test
	public void test1() {
		Test1 annotation = AnnotatedElementUtils.getMergedAnnotation(Element.class, Test1.class);
		Test2 test2 = AnnotatedElementUtils.getMergedAnnotation(Element.class, Test2.class);
		Test1 annotation2 = AnnotatedElementUtils.getMergedAnnotation(Element2.class, Test1.class);
		assertNotNull(annotation);
		assertNotNull(test2);
		assertEquals(annotation, annotation2);
		assertEquals("override the method", annotation.test1());
		assertEquals("override the method", test2.test1());
		assertEquals(annotation.test1(), annotation.test12());
	}

	@Test
	public void test2() {
		Test1 annotation = AnnotatedElementUtils.getMergedAnnotationWithMultipleAliases(Element.class, Test1.class);
		Test2 test2 = AnnotatedElementUtils.getMergedAnnotationWithMultipleAliases(Element.class, Test2.class);
		Test1 annotation2 = AnnotatedElementUtils.getMergedAnnotationWithMultipleAliases(Element2.class, Test1.class);
		assertNotNull(annotation);
		assertNotNull(test2);
		assertEquals(annotation, annotation2);
		assertEquals("override the method", annotation.test1());
		assertEquals("override the method", test2.test1());
		assertEquals(annotation.test1(), annotation.test12());
	}
}
