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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author ZiCheng Zhang
 * @date 2020/10/02
 */
public class AlisforsTests {

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Test1 {
		String test1() default "test1";
	}

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Test2 {
		String test2() default "test2";
	}

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Test1
	@Test2
	public @interface Test3 {

		@AliasFor(annotation = Test1.class, attribute = "test1")
		@AliasFor(annotation = Test2.class, attribute = "test2")
		String test3() default "test3";
	}

	@Test3(test3 = "override the method")
	public static class Element1 {
	}

	@Test1(test1 = "override the method")
	public static class Element2 {
	}

	@Test
	public void test1() {
		Test1 annotation = AnnotatedElementUtils.getMergedAnnotationWithMultipleAliases(Element1.class, Test1.class);
		Test2 test2 = AnnotatedElementUtils.getMergedAnnotationWithMultipleAliases(Element1.class, Test2.class);
		Test1 annotation2 = AnnotatedElementUtils.getMergedAnnotationWithMultipleAliases(Element2.class, Test1.class);
		assertEquals(annotation, annotation2);
		assertNotNull(annotation);
		assertNotNull(test2);
		assertEquals(annotation.test1(), test2.test2());
	}

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Test4 {

		@AliasFor("test2")
		@AliasFor("test3")
		String test1() default "test";

		@AliasFor("test1")
		@AliasFor("test3")
		String test2() default "test";

		@AliasFor("test1")
		@AliasFor("test2")
		String test3() default "test";
	}

	@Test4(test1 = "override the method")
	public static class Element3 {
	}

	@Test
	public void test2() {
		Test4 annotation = AnnotatedElementUtils.getMergedAnnotationWithMultipleAliases(Element3.class, Test4.class);
		String test1 = annotation.test1();
		String test2 = annotation.test2();
		String test3 = annotation.test3();
		assertEquals("override the method", test1);
		assertEquals(test1, test2);
		assertEquals(test1, test3);
		assertEquals(test2, test3);
	}

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Test5 {

		@AliasFor("test2")
		@AliasFor("test3")
		String test1() default "test1";

		@AliasFor("test1")
		@AliasFor("test3")
		String test2() default "test1";

		@AliasFor("test1")
		@AliasFor("test2")
		String test3() default "test1";
	}

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Test5
	public @interface Test6 {

		@AliasFor("test5")
		@AliasFor("test6")
		String test4() default "test2";

		@AliasFor("test4")
		@AliasFor("test6")
		String test5() default "test2";

		@AliasFor(annotation = Test5.class, attribute = "test1")
		@AliasFor("test4")
		@AliasFor("test5")
		String test6() default "test2";
	}

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Test6
	public @interface Test7 {

		@AliasFor(annotation = Test6.class, attribute = "test6")
		String test7() default "test3";
	}

	@Test7(test7 = "override the method")
	public static class Element4 {
	}

	@Test
	public void test3() {
		Test5 test5 = AnnotatedElementUtils.getMergedAnnotationWithMultipleAliases(Element4.class, Test5.class);
		Test6 test6 = AnnotatedElementUtils.getMergedAnnotationWithMultipleAliases(Element4.class, Test6.class);
		assertNotNull(test5);
		assertNotNull(test6);
		System.out.println(test5.toString());
		System.out.println(test6.toString());
		assertEquals("override the method", test6.test4());
		assertEquals("override the method", test6.test5());
		assertEquals("override the method", test6.test6());
		assertEquals("override the method", test5.test1());
		assertEquals("override the method", test5.test2());
		assertEquals("override the method", test5.test3());
	}

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Test8 {

		@AliasFor("test2")
		@AliasFor("test3")
		String test1() default "test1";

		@AliasFor("test1")
		@AliasFor("test3")
		String test2() default "test1";

		@AliasFor("test1")
		@AliasFor("test2")
		String test3() default "test1";
	}

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Test8
	public @interface Test9 {

		@AliasFor(annotation = Test8.class, attribute = "test1")
		@AliasFor("test5")
		@AliasFor("test6")
		String test4() default "test2";

		@AliasFor("test4")
		@AliasFor("test6")
		String test5() default "test2";

		@AliasFor("test4")
		@AliasFor("test5")
		String test6() default "test2";
	}

	@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Test9
	public @interface Test10 {

		@AliasFor(annotation = Test9.class, attribute = "test6")
		String test7() default "test3";
	}

	@Test10(test7 = "override the method")
	public static class Element5 {
	}

	/**
	 * fixme {@link #test3()} passed, but {@code #test4()} not passed.
	 * We should handle the transfer of attributes between levels first,
	 * and then handle the transfer of attributes at the same level
	 */
	@Test
	public void test4() {
		Test8 test8 = AnnotatedElementUtils.getMergedAnnotationWithMultipleAliases(Element5.class, Test8.class);
		Test9 test9 = AnnotatedElementUtils.getMergedAnnotationWithMultipleAliases(Element5.class, Test9.class);
		assertNotNull(test8);
		assertNotNull(test9);
		System.out.println(test8.toString());
		System.out.println(test9.toString());
		assertEquals("override the method", test9.test4());
		assertEquals("override the method", test9.test5());
		assertEquals("override the method", test9.test6());
		assertEquals("override the method", test8.test1());
		assertEquals("override the method", test8.test2());
		assertEquals("override the method", test8.test3());
	}
}