/*
 * Copyright 2002-2022 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests that verify support for finding multiple composed annotations on a single
 * annotated element.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
class MergedAnnotationsComposedOnSingleAnnotatedElementTests {

	// See SPR-13486

	@Test
	void inheritedStrategyMultipleComposedAnnotationsOnClass() {
		assertInheritedStrategyBehavior(MultipleComposedCachesClass.class);
	}

	@Test
	void inheritedStrategyMultipleInheritedComposedAnnotationsOnSuperclass() {
		assertInheritedStrategyBehavior(SubMultipleComposedCachesClass.class);
	}

	@Test
	void inheritedStrategyMultipleNoninheritedComposedAnnotationsOnClass() {
		MergedAnnotations annotations = MergedAnnotations.from(
				MultipleNoninheritedComposedCachesClass.class,
				SearchStrategy.INHERITED_ANNOTATIONS);
		assertThat(stream(annotations, "value")).containsExactly("noninheritedCache1",
				"noninheritedCache2");
	}

	@Test
	void inheritedStrategyMultipleNoninheritedComposedAnnotationsOnSuperclass() {
		MergedAnnotations annotations = MergedAnnotations.from(
				SubMultipleNoninheritedComposedCachesClass.class,
				SearchStrategy.INHERITED_ANNOTATIONS);
		assertThat(annotations.stream(Cacheable.class)).isEmpty();
	}

	@Test
	void inheritedStrategyComposedPlusLocalAnnotationsOnClass() {
		assertInheritedStrategyBehavior(ComposedPlusLocalCachesClass.class);
	}

	@Test
	void inheritedStrategyMultipleComposedAnnotationsOnInterface() {
		MergedAnnotations annotations = MergedAnnotations.from(
				MultipleComposedCachesOnInterfaceClass.class,
				SearchStrategy.INHERITED_ANNOTATIONS);
		assertThat(annotations.stream(Cacheable.class)).isEmpty();
	}

	@Test
	void inheritedStrategyMultipleComposedAnnotationsOnMethod() throws Exception {
		assertInheritedStrategyBehavior(
				getClass().getDeclaredMethod("multipleComposedCachesMethod"));
	}

	@Test
	void inheritedStrategyComposedPlusLocalAnnotationsOnMethod() throws Exception {
		assertInheritedStrategyBehavior(
				getClass().getDeclaredMethod("composedPlusLocalCachesMethod"));
	}

	private void assertInheritedStrategyBehavior(AnnotatedElement element) {
		MergedAnnotations annotations = MergedAnnotations.from(element,
				SearchStrategy.INHERITED_ANNOTATIONS);
		assertThat(stream(annotations, "key")).containsExactly("fooKey", "barKey");
		assertThat(stream(annotations, "value")).containsExactly("fooCache", "barCache");
	}

	@Test
	void typeHierarchyStrategyMultipleComposedAnnotationsOnClass() {
		assertTypeHierarchyStrategyBehavior(MultipleComposedCachesClass.class);
	}

	@Test
	void typeHierarchyStrategyMultipleInheritedComposedAnnotationsOnSuperclass() {
		assertTypeHierarchyStrategyBehavior(SubMultipleComposedCachesClass.class);
	}

	@Test
	void typeHierarchyStrategyMultipleNoninheritedComposedAnnotationsOnClass() {
		MergedAnnotations annotations = MergedAnnotations.from(
				MultipleNoninheritedComposedCachesClass.class, SearchStrategy.TYPE_HIERARCHY);
		assertThat(stream(annotations, "value")).containsExactly("noninheritedCache1",
				"noninheritedCache2");
	}

	@Test
	void typeHierarchyStrategyMultipleNoninheritedComposedAnnotationsOnSuperclass() {
		MergedAnnotations annotations = MergedAnnotations.from(
				SubMultipleNoninheritedComposedCachesClass.class,
				SearchStrategy.TYPE_HIERARCHY);
		assertThat(stream(annotations, "value")).containsExactly("noninheritedCache1",
				"noninheritedCache2");
	}

	@Test
	void typeHierarchyStrategyComposedPlusLocalAnnotationsOnClass() {
		assertTypeHierarchyStrategyBehavior(ComposedPlusLocalCachesClass.class);
	}

	@Test
	void typeHierarchyStrategyMultipleComposedAnnotationsOnInterface() {
		assertTypeHierarchyStrategyBehavior(MultipleComposedCachesOnInterfaceClass.class);
	}

	@Test
	void typeHierarchyStrategyComposedCacheOnInterfaceAndLocalCacheOnClass() {
		assertTypeHierarchyStrategyBehavior(
				ComposedCacheOnInterfaceAndLocalCacheClass.class);
	}

	@Test
	void typeHierarchyStrategyMultipleComposedAnnotationsOnMethod() throws Exception {
		assertTypeHierarchyStrategyBehavior(
				getClass().getDeclaredMethod("multipleComposedCachesMethod"));
	}

	@Test
	void typeHierarchyStrategyComposedPlusLocalAnnotationsOnMethod()
			throws Exception {
		assertTypeHierarchyStrategyBehavior(
				getClass().getDeclaredMethod("composedPlusLocalCachesMethod"));
	}

	@Test
	void typeHierarchyStrategyMultipleComposedAnnotationsOnBridgeMethod()
			throws Exception {
		assertTypeHierarchyStrategyBehavior(getBridgeMethod());
	}

	private void assertTypeHierarchyStrategyBehavior(AnnotatedElement element) {
		MergedAnnotations annotations = MergedAnnotations.from(element,
				SearchStrategy.TYPE_HIERARCHY);
		assertThat(stream(annotations, "key")).containsExactly("fooKey", "barKey");
		assertThat(stream(annotations, "value")).containsExactly("fooCache", "barCache");
	}

	Method getBridgeMethod() throws NoSuchMethodException {
		List<Method> methods = new ArrayList<>();
		ReflectionUtils.doWithLocalMethods(StringGenericParameter.class, method -> {
			if ("getFor".equals(method.getName())) {
				methods.add(method);
			}
		});
		Method bridgeMethod = methods.get(0).getReturnType() == Object.class ? methods.get(0) : methods.get(1);
		assertThat(bridgeMethod.isBridge()).isTrue();
		return bridgeMethod;
	}

	private Stream<String> stream(MergedAnnotations annotations, String attributeName) {
		return annotations.stream(Cacheable.class).map(
				annotation -> annotation.getString(attributeName));
	}

	// @formatter:off

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

	private static class SubMultipleComposedCachesClass
			extends MultipleComposedCachesClass {
	}

	@NoninheritedCache1
	@NoninheritedCache2
	private static class MultipleNoninheritedComposedCachesClass {
	}

	private static class SubMultipleNoninheritedComposedCachesClass
			extends MultipleNoninheritedComposedCachesClass {
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

	@BarCache(key = "barKey")
	private interface ComposedCacheInterface {
	}

	@Cacheable(cacheName = "fooCache", key = "fooKey")
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
		public String getFor(Class<String> cls) { return "foo"; }
		public String getFor(Integer integer) { return "foo"; }
	}

	// @formatter:on

}
