/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.springframework.core.annotation.AnnotatedElementUtils.*;

/**
 * Unit tests that verify support for finding multiple composed annotations on
 * a single annotated element.
 *
 * <p>See <a href="https://jira.spring.io/browse/SPR-13486">SPR-13486</a>.
 *
 * @author Sam Brannen
 * @since 4.3
 * @see AnnotatedElementUtils
 * @see AnnotatedElementUtilsTests
 */
public class MultipleComposedAnnotationsOnSingleAnnotatedElementTests {

	@Test
	public void multipleComposedAnnotationsOnClass() {
		assertMultipleComposedAnnotations(MultipleCachesClass.class);
	}

	@Test
	public void multipleComposedAnnotationsOnMethod() throws Exception {
		AnnotatedElement element = getClass().getDeclaredMethod("multipleCachesMethod");
		assertMultipleComposedAnnotations(element);
	}

	private void assertMultipleComposedAnnotations(AnnotatedElement element) {
		assertNotNull(element);

		// Prerequisites
		FooCache fooCache = element.getAnnotation(FooCache.class);
		BarCache barCache = element.getAnnotation(BarCache.class);
		assertNotNull(fooCache);
		assertNotNull(barCache);
		assertEquals("fooKey", fooCache.key());
		assertEquals("barKey", barCache.key());

		// Assert the status quo for finding the 1st merged annotation.
		Cacheable cacheable = findMergedAnnotation(element, Cacheable.class);
		assertNotNull(cacheable);
		assertEquals("fooCache", cacheable.value());
		assertEquals("fooKey", cacheable.key());

		// TODO Introduce findMergedAnnotations(...) in AnnotatedElementUtils.

		// assertEquals("barCache", cacheable.value());
		// assertEquals("barKey", cacheable.key());
	}


	// -------------------------------------------------------------------------

	/**
	 * Mock of {@code org.springframework.cache.annotation.Cacheable}.
	 */
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface Cacheable {

		String value();

		String key() default "";
	}

	@Cacheable("fooCache")
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface FooCache {

		@AliasFor(annotation = Cacheable.class)
		String key() default "";
	}

	@Cacheable("barCache")
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface BarCache {

		@AliasFor(annotation = Cacheable.class)
		String key();
	}

	@FooCache(key = "fooKey")
	@BarCache(key = "barKey")
	private static class MultipleCachesClass {
	}


	@FooCache(key = "fooKey")
	@BarCache(key = "barKey")
	private void multipleCachesMethod() {
	}

}
