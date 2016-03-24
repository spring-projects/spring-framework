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
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

import org.junit.Ignore;
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
	public void getMultipleComposedAnnotationsOnClass() {
		assertGetAllMergedAnnotationsBehavior(MultipleComposedCachesClass.class);
	}

	@Test
	public void getMultipleInheritedComposedAnnotationsOnSuperclass() {
		assertGetAllMergedAnnotationsBehavior(SubMultipleComposedCachesClass.class);
	}

	@Test
	public void getMultipleNoninheritedComposedAnnotationsOnClass() {
		Class<?> element = MultipleNoninheritedComposedCachesClass.class;
		Set<Cacheable> cacheables = getAllMergedAnnotations(element, Cacheable.class);
		assertNotNull(cacheables);
		assertEquals(2, cacheables.size());

		Iterator<Cacheable> iterator = cacheables.iterator();
		Cacheable cacheable1 = iterator.next();
		Cacheable cacheable2 = iterator.next();
		assertEquals("noninheritedCache1", cacheable1.value());
		assertEquals("noninheritedCache2", cacheable2.value());
	}

	@Test
	public void getMultipleNoninheritedComposedAnnotationsOnSuperclass() {
		Class<?> element = SubMultipleNoninheritedComposedCachesClass.class;
		Set<Cacheable> cacheables = getAllMergedAnnotations(element, Cacheable.class);
		assertNotNull(cacheables);
		assertEquals(0, cacheables.size());
	}

	@Test
	public void getComposedPlusLocalAnnotationsOnClass() {
		assertGetAllMergedAnnotationsBehavior(ComposedPlusLocalCachesClass.class);
	}

	@Test
	public void getMultipleComposedAnnotationsOnInterface() {
		Class<MultipleComposedCachesOnInterfaceClass> element = MultipleComposedCachesOnInterfaceClass.class;
		Set<Cacheable> cacheables = getAllMergedAnnotations(element, Cacheable.class);
		assertNotNull(cacheables);
		assertEquals(0, cacheables.size());
	}

	@Test
	public void getMultipleComposedAnnotationsOnMethod() throws Exception {
		AnnotatedElement element = getClass().getDeclaredMethod("multipleComposedCachesMethod");
		assertGetAllMergedAnnotationsBehavior(element);
	}

	@Test
	public void getComposedPlusLocalAnnotationsOnMethod() throws Exception {
		AnnotatedElement element = getClass().getDeclaredMethod("composedPlusLocalCachesMethod");
		assertGetAllMergedAnnotationsBehavior(element);
	}

	@Test
	@Ignore("Disabled since some Java 8 updates handle the bridge method differently")
	public void getMultipleComposedAnnotationsOnBridgeMethod() throws Exception {
		Set<Cacheable> cacheables = getAllMergedAnnotations(getBridgeMethod(), Cacheable.class);
		assertNotNull(cacheables);
		assertEquals(0, cacheables.size());
	}

	@Test
	public void findMultipleComposedAnnotationsOnClass() {
		assertFindAllMergedAnnotationsBehavior(MultipleComposedCachesClass.class);
	}

	@Test
	public void findMultipleInheritedComposedAnnotationsOnSuperclass() {
		assertFindAllMergedAnnotationsBehavior(SubMultipleComposedCachesClass.class);
	}

	@Test
	public void findMultipleNoninheritedComposedAnnotationsOnClass() {
		Class<?> element = MultipleNoninheritedComposedCachesClass.class;
		Set<Cacheable> cacheables = findAllMergedAnnotations(element, Cacheable.class);
		assertNotNull(cacheables);
		assertEquals(2, cacheables.size());

		Iterator<Cacheable> iterator = cacheables.iterator();
		Cacheable cacheable1 = iterator.next();
		Cacheable cacheable2 = iterator.next();
		assertEquals("noninheritedCache1", cacheable1.value());
		assertEquals("noninheritedCache2", cacheable2.value());
	}

	@Test
	public void findMultipleNoninheritedComposedAnnotationsOnSuperclass() {
		Class<?> element = SubMultipleNoninheritedComposedCachesClass.class;
		Set<Cacheable> cacheables = findAllMergedAnnotations(element, Cacheable.class);
		assertNotNull(cacheables);
		assertEquals(2, cacheables.size());

		Iterator<Cacheable> iterator = cacheables.iterator();
		Cacheable cacheable1 = iterator.next();
		Cacheable cacheable2 = iterator.next();
		assertEquals("noninheritedCache1", cacheable1.value());
		assertEquals("noninheritedCache2", cacheable2.value());
	}

	@Test
	public void findComposedPlusLocalAnnotationsOnClass() {
		assertFindAllMergedAnnotationsBehavior(ComposedPlusLocalCachesClass.class);
	}

	@Test
	public void findMultipleComposedAnnotationsOnInterface() {
		assertFindAllMergedAnnotationsBehavior(MultipleComposedCachesOnInterfaceClass.class);
	}

	@Test
	public void findComposedCacheOnInterfaceAndLocalCacheOnClass() {
		assertFindAllMergedAnnotationsBehavior(ComposedCacheOnInterfaceAndLocalCacheClass.class);
	}

	@Test
	public void findMultipleComposedAnnotationsOnMethod() throws Exception {
		AnnotatedElement element = getClass().getDeclaredMethod("multipleComposedCachesMethod");
		assertFindAllMergedAnnotationsBehavior(element);
	}

	@Test
	public void findComposedPlusLocalAnnotationsOnMethod() throws Exception {
		AnnotatedElement element = getClass().getDeclaredMethod("composedPlusLocalCachesMethod");
		assertFindAllMergedAnnotationsBehavior(element);
	}

	@Test
	public void findMultipleComposedAnnotationsOnBridgeMethod() throws Exception {
		assertFindAllMergedAnnotationsBehavior(getBridgeMethod());
	}

	/**
	 * Bridge/bridged method setup code copied from
	 * {@link org.springframework.core.BridgeMethodResolverTests#testWithGenericParameter()}.
	 */
	public Method getBridgeMethod() throws NoSuchMethodException {
		Method[] methods = StringGenericParameter.class.getMethods();
		Method bridgeMethod = null;
		Method bridgedMethod = null;

		for (Method method : methods) {
			if ("getFor".equals(method.getName()) && !method.getParameterTypes()[0].equals(Integer.class)) {
				if (method.getReturnType().equals(Object.class)) {
					bridgeMethod = method;
				}
				else {
					bridgedMethod = method;
				}
			}
		}
		assertTrue(bridgeMethod != null && bridgeMethod.isBridge());
		assertTrue(bridgedMethod != null && !bridgedMethod.isBridge());

		return bridgeMethod;
	}

	private void assertGetAllMergedAnnotationsBehavior(AnnotatedElement element) {
		assertNotNull(element);

		Set<Cacheable> cacheables = getAllMergedAnnotations(element, Cacheable.class);
		assertNotNull(cacheables);
		assertEquals(2, cacheables.size());

		Iterator<Cacheable> iterator = cacheables.iterator();
		Cacheable fooCacheable = iterator.next();
		Cacheable barCacheable = iterator.next();
		assertEquals("fooKey", fooCacheable.key());
		assertEquals("fooCache", fooCacheable.value());
		assertEquals("barKey", barCacheable.key());
		assertEquals("barCache", barCacheable.value());
	}

	private void assertFindAllMergedAnnotationsBehavior(AnnotatedElement element) {
		assertNotNull(element);

		Set<Cacheable> cacheables = findAllMergedAnnotations(element, Cacheable.class);
		assertNotNull(cacheables);
		assertEquals(2, cacheables.size());

		Iterator<Cacheable> iterator = cacheables.iterator();
		Cacheable fooCacheable = iterator.next();
		Cacheable barCacheable = iterator.next();
		assertEquals("fooKey", fooCacheable.key());
		assertEquals("fooCache", fooCacheable.value());
		assertEquals("barKey", barCacheable.key());
		assertEquals("barCache", barCacheable.value());
	}


	// -------------------------------------------------------------------------

	/**
	 * Mock of {@code org.springframework.cache.annotation.Cacheable}.
	 */
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface Cacheable {

		@AliasFor("cacheName")
		String value() default "";

		@AliasFor("value")
		String cacheName() default "";

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

	@Cacheable("noninheritedCache1")
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@interface NoninheritedCache1 {

		@AliasFor(annotation = Cacheable.class)
		String key() default "";
	}

	@Cacheable("noninheritedCache2")
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@interface NoninheritedCache2 {

		@AliasFor(annotation = Cacheable.class)
		String key() default "";
	}

	@FooCache(key = "fooKey")
	@BarCache(key = "barKey")
	private static class MultipleComposedCachesClass {
	}

	private static class SubMultipleComposedCachesClass extends MultipleComposedCachesClass {
	}

	@NoninheritedCache1
	@NoninheritedCache2
	private static class MultipleNoninheritedComposedCachesClass {
	}

	private static class SubMultipleNoninheritedComposedCachesClass extends MultipleNoninheritedComposedCachesClass {
	}

	@Cacheable(cacheName = "fooCache", key = "fooKey")
	@BarCache(key = "barKey")
	private static class ComposedPlusLocalCachesClass {
	}

	@FooCache(key = "fooKey")
	@BarCache(key = "barKey")
	private interface MultipleComposedCachesInterface {
	}

	private static class MultipleComposedCachesOnInterfaceClass implements MultipleComposedCachesInterface {
	}

	@Cacheable(cacheName = "fooCache", key = "fooKey")
	private interface ComposedCacheInterface {
	}

	@BarCache(key = "barKey")
	private static class ComposedCacheOnInterfaceAndLocalCacheClass implements ComposedCacheInterface {
	}


	@FooCache(key = "fooKey")
	@BarCache(key = "barKey")
	private void multipleComposedCachesMethod() {
	}

	@Cacheable(cacheName = "fooCache", key = "fooKey")
	@BarCache(key = "barKey")
	private void composedPlusLocalCachesMethod() {
	}


	public interface GenericParameter<T> {

		T getFor(Class<T> cls);
	}

	@SuppressWarnings("unused")
	private static class StringGenericParameter implements GenericParameter<String> {

		@FooCache(key = "fooKey")
		@BarCache(key = "barKey")
		@Override
		public String getFor(Class<String> cls) {
			return "foo";
		}

		public String getFor(Integer integer) {
			return "foo";
		}
	}

}
