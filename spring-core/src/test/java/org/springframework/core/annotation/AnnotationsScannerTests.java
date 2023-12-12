/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotations.Search;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationsScanner}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
class AnnotationsScannerTests {

	@Test
	void directStrategyOnClassWhenNotAnnotatedScansNone() {
		Class<?> source = WithNoAnnotations.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).isEmpty();
	}

	@Test
	void directStrategyOnClassScansAnnotations() {
		Class<?> source = WithSingleAnnotation.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void directStrategyOnClassWhenMultipleAnnotationsScansAnnotations() {
		Class<?> source = WithMultipleAnnotations.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	void directStrategyOnClassWhenHasSuperclassScansOnlyDirect() {
		Class<?> source = WithSingleSuperclass.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void directStrategyOnClassWhenHasInterfaceScansOnlyDirect() {
		Class<?> source = WithSingleInterface.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void directStrategyOnClassHierarchyScansInCorrectOrder() {
		Class<?> source = WithHierarchy.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void inheritedAnnotationsStrategyOnClassWhenNotAnnotatedScansNone() {
		Class<?> source = WithNoAnnotations.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).isEmpty();
	}

	@Test
	void inheritedAnnotationsStrategyOnClassScansAnnotations() {
		Class<?> source = WithSingleAnnotation.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void inheritedAnnotationsStrategyOnClassWhenMultipleAnnotationsScansAnnotations() {
		Class<?> source = WithMultipleAnnotations.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	void inheritedAnnotationsStrategyOnClassWhenHasSuperclassScansOnlyInherited() {
		Class<?> source = WithSingleSuperclass.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1", "1:TestInheritedAnnotation2");
	}

	@Test
	void inheritedAnnotationsStrategyOnClassWhenHasInterfaceDoesNotIncludeInterfaces() {
		Class<?> source = WithSingleInterface.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void inheritedAnnotationsStrategyOnClassHierarchyScansInCorrectOrder() {
		Class<?> source = WithHierarchy.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1", "1:TestInheritedAnnotation2", "2:TestInheritedAnnotation3");
	}

	@Test
	void inheritedAnnotationsStrategyOnClassWhenHasAnnotationOnBothClassesIncludesOnlyOne() {
		Class<?> source = WithSingleSuperclassAndDoubleInherited.class;
		assertThat(Arrays.stream(source.getAnnotations()).map(
				Annotation::annotationType).map(Class::getName)).containsExactly(
						TestInheritedAnnotation2.class.getName());
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly("0:TestInheritedAnnotation2");
	}

	@Test
	void superclassStrategyOnClassWhenNotAnnotatedScansNone() {
		Class<?> source = WithNoAnnotations.class;
		assertThat(scan(source, SearchStrategy.SUPERCLASS)).isEmpty();
	}

	@Test
	void superclassStrategyOnClassScansAnnotations() {
		Class<?> source = WithSingleAnnotation.class;
		assertThat(scan(source, SearchStrategy.SUPERCLASS)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void superclassStrategyOnClassWhenMultipleAnnotationsScansAnnotations() {
		Class<?> source = WithMultipleAnnotations.class;
		assertThat(scan(source, SearchStrategy.SUPERCLASS)).containsExactly("0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	void superclassStrategyOnClassWhenHasSuperclassScansSuperclass() {
		Class<?> source = WithSingleSuperclass.class;
		assertThat(scan(source, SearchStrategy.SUPERCLASS)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	void superclassStrategyOnClassWhenHasInterfaceDoesNotIncludeInterfaces() {
		Class<?> source = WithSingleInterface.class;
		assertThat(scan(source, SearchStrategy.SUPERCLASS)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void superclassStrategyOnClassHierarchyScansInCorrectOrder() {
		Class<?> source = WithHierarchy.class;
		assertThat(scan(source, SearchStrategy.SUPERCLASS)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2",
				"2:TestAnnotation3", "2:TestInheritedAnnotation3");
	}

	@Test
	void typeHierarchyStrategyOnClassWhenNotAnnotatedScansNone() {
		Class<?> source = WithNoAnnotations.class;
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).isEmpty();
	}

	@Test
	void typeHierarchyStrategyOnClassScansAnnotations() {
		Class<?> source = WithSingleAnnotation.class;
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void typeHierarchyStrategyOnClassWhenMultipleAnnotationsScansAnnotations() {
		Class<?> source = WithMultipleAnnotations.class;
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	void typeHierarchyStrategyOnClassWhenHasSuperclassScansSuperclass() {
		Class<?> source = WithSingleSuperclass.class;
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	void typeHierarchyStrategyOnClassWhenHasSingleInterfaceScansInterfaces() {
		Class<?> source = WithSingleInterface.class;
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	void typeHierarchyStrategyOnClassHierarchyScansInCorrectOrder() {
		Class<?> source = WithHierarchy.class;
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation5", "1:TestInheritedAnnotation5",
				"2:TestAnnotation6", "3:TestAnnotation2", "3:TestInheritedAnnotation2",
				"4:TestAnnotation3", "4:TestInheritedAnnotation3", "5:TestAnnotation4");
	}

	@Test
	void directStrategyOnMethodWhenNotAnnotatedScansNone() {
		Method source = methodFrom(WithNoAnnotations.class);
		assertThat(scan(source, SearchStrategy.DIRECT)).isEmpty();
	}

	@Test
	void directStrategyOnMethodScansAnnotations() {
		Method source = methodFrom(WithSingleAnnotation.class);
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void directStrategyOnMethodWhenMultipleAnnotationsScansAnnotations() {
		Method source = methodFrom(WithMultipleAnnotations.class);
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	void directStrategyOnMethodWhenHasSuperclassScansOnlyDirect() {
		Method source = methodFrom(WithSingleSuperclass.class);
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void directStrategyOnMethodWhenHasInterfaceScansOnlyDirect() {
		Method source = methodFrom(WithSingleInterface.class);
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void directStrategyOnMethodHierarchyScansInCorrectOrder() {
		Method source = methodFrom(WithHierarchy.class);
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void inheritedAnnotationsStrategyOnMethodWhenNotAnnotatedScansNone() {
		Method source = methodFrom(WithNoAnnotations.class);
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).isEmpty();
	}

	@Test
	void inheritedAnnotationsStrategyOnMethodScansAnnotations() {
		Method source = methodFrom(WithSingleAnnotation.class);
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void inheritedAnnotationsStrategyOnMethodWhenMultipleAnnotationsScansAnnotations() {
		Method source = methodFrom(WithMultipleAnnotations.class);
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	void inheritedAnnotationsMethodOnMethodWhenHasSuperclassIgnoresInherited() {
		Method source = methodFrom(WithSingleSuperclass.class);
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void inheritedAnnotationsStrategyOnMethodWhenHasInterfaceDoesNotIncludeInterfaces() {
		Method source = methodFrom(WithSingleInterface.class);
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void inheritedAnnotationsStrategyOnMethodHierarchyScansInCorrectOrder() {
		Method source = methodFrom(WithHierarchy.class);
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void superclassStrategyOnMethodWhenNotAnnotatedScansNone() {
		Method source = methodFrom(WithNoAnnotations.class);
		assertThat(scan(source, SearchStrategy.SUPERCLASS)).isEmpty();
	}

	@Test
	void superclassStrategyOnMethodScansAnnotations() {
		Method source = methodFrom(WithSingleAnnotation.class);
		assertThat(scan(source, SearchStrategy.SUPERCLASS)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void superclassStrategyOnMethodWhenMultipleAnnotationsScansAnnotations() {
		Method source = methodFrom(WithMultipleAnnotations.class);
		assertThat(scan(source, SearchStrategy.SUPERCLASS)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	void superclassStrategyOnMethodWhenHasSuperclassScansSuperclass() {
		Method source = methodFrom(WithSingleSuperclass.class);
		assertThat(scan(source, SearchStrategy.SUPERCLASS)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	void superclassStrategyOnMethodWhenHasInterfaceDoesNotIncludeInterfaces() {
		Method source = methodFrom(WithSingleInterface.class);
		assertThat(scan(source, SearchStrategy.SUPERCLASS)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void superclassStrategyOnMethodHierarchyScansInCorrectOrder() {
		Method source = methodFrom(WithHierarchy.class);
		assertThat(scan(source, SearchStrategy.SUPERCLASS)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2",
				"2:TestAnnotation3");
	}

	@Test
	void typeHierarchyStrategyOnMethodWhenNotAnnotatedScansNone() {
		Method source = methodFrom(WithNoAnnotations.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).isEmpty();
	}

	@Test
	void typeHierarchyStrategyOnMethodScansAnnotations() {
		Method source = methodFrom(WithSingleAnnotation.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void typeHierarchyStrategyOnMethodWhenMultipleAnnotationsScansAnnotations() {
		Method source = methodFrom(WithMultipleAnnotations.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	void typeHierarchyStrategyOnMethodWhenHasSuperclassScansSuperclass() {
		Method source = methodFrom(WithSingleSuperclass.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	void typeHierarchyStrategyOnMethodWhenHasInterfaceScansInterfaces() {
		Method source = methodFrom(WithSingleInterface.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");

		source = methodFrom(Hello1Impl.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly("1:TestAnnotation1");
	}

	@Test  // gh-31803
	void typeHierarchyStrategyOnMethodWhenHasInterfaceHierarchyScansInterfacesOnlyOnce() {
		Method source = methodFrom(Hello2Impl.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly("1:TestAnnotation1");
	}

	@Test
	void typeHierarchyStrategyOnMethodHierarchyScansInCorrectOrder() {
		Method source = methodFrom(WithHierarchy.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation5", "1:TestInheritedAnnotation5",
				"2:TestAnnotation6", "3:TestAnnotation2", "3:TestInheritedAnnotation2",
				"4:TestAnnotation3", "5:TestAnnotation4");
	}

	@Test
	void typeHierarchyStrategyOnBridgeMethodScansAnnotations() throws Exception {
		Method source = BridgedMethod.class.getDeclaredMethod("method", Object.class);
		assertThat(source.isBridge()).isTrue();
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2");
	}

	@Test
	void typeHierarchyStrategyOnBridgedMethodScansAnnotations() throws Exception {
		Method source = BridgedMethod.class.getDeclaredMethod("method", String.class);
		assertThat(source.isBridge()).isFalse();
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2");
	}

	@Test
	void directStrategyOnBridgeMethodScansAnnotations() throws Exception {
		Method source = BridgedMethod.class.getDeclaredMethod("method", Object.class);
		assertThat(source.isBridge()).isTrue();
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void directStrategyOnBridgedMethodScansAnnotations() throws Exception {
		Method source = BridgedMethod.class.getDeclaredMethod("method", String.class);
		assertThat(source.isBridge()).isFalse();
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void typeHierarchyStrategyOnMethodWithIgnorablesScansAnnotations() {
		Method source = methodFrom(Ignorable.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void typeHierarchyStrategyOnMethodWithMultipleCandidatesScansAnnotations() {
		Method source = methodFrom(MultipleMethods.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void typeHierarchyStrategyOnMethodWithGenericParameterOverrideScansAnnotations() {
		Method source = ReflectionUtils.findMethod(GenericOverride.class, "method", String.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2");
	}

	@Test
	void typeHierarchyStrategyOnMethodWithGenericParameterNonOverrideScansAnnotations() {
		Method source = ReflectionUtils.findMethod(GenericNonOverride.class, "method", StringBuilder.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY)).containsExactly("0:TestAnnotation1");
	}

	@Test
	void typeHierarchyStrategyWithEnclosingClassPredicatesOnEnclosedStaticClassScansAnnotations() {
		Class<?> source = AnnotationEnclosingClassSample.EnclosedStatic.EnclosedStaticStatic.class;
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY, ClassUtils::isInnerClass))
				.containsExactly("0:EnclosedThree");
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY, Search.always).toList())
				.isEqualTo(scan(source, SearchStrategy.TYPE_HIERARCHY, ClassUtils::isStaticClass).toList())
				.containsExactly("0:EnclosedThree", "1:EnclosedTwo", "2:EnclosedOne");
	}

	@Test
	void typeHierarchyStrategyWithEnclosingClassPredicatesOnEnclosedInnerClassScansAnnotations() {
		Class<?> source = AnnotationEnclosingClassSample.EnclosedInner.EnclosedInnerInner.class;
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY, ClassUtils::isStaticClass))
				.containsExactly("0:EnclosedThree");
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY, Search.always).toList())
				.isEqualTo(scan(source, SearchStrategy.TYPE_HIERARCHY, ClassUtils::isInnerClass).toList())
				.containsExactly("0:EnclosedThree", "1:EnclosedTwo", "2:EnclosedOne");
	}

	@Test
	void typeHierarchyStrategyWithEnclosingClassPredicatesOnMethodHierarchyUsesTypeHierarchyScan() {
		Method source = methodFrom(WithHierarchy.class);
		assertThat(scan(source, SearchStrategy.TYPE_HIERARCHY, Search.always).toList())
				.isEqualTo(scan(source, SearchStrategy.TYPE_HIERARCHY, ClassUtils::isInnerClass).toList())
				.containsExactly(
					"0:TestAnnotation1", "1:TestAnnotation5", "1:TestInheritedAnnotation5",
					"2:TestAnnotation6", "3:TestAnnotation2", "3:TestInheritedAnnotation2",
					"4:TestAnnotation3", "5:TestAnnotation4");
	}

	@Test
	void scanWhenProcessorReturnsFromDoWithAggregateExitsEarly() {
		String result = scan(this, WithSingleSuperclass.class, SearchStrategy.TYPE_HIERARCHY,
				new AnnotationsProcessor<Object, String>() {

					@Override
					@Nullable
					public String doWithAggregate(Object context, int aggregateIndex) {
						return "";
					}

					@Override
					@Nullable
					public String doWithAnnotations(Object context, int aggregateIndex,
							Object source, Annotation[] annotations) {
						throw new IllegalStateException("Should not call");
					}

				});
		assertThat(result).isEmpty();
	}

	@Test
	void scanWhenProcessorReturnsFromDoWithAnnotationsExitsEarly() {
		List<Integer> indexes = new ArrayList<>();
		String result = scan(this, WithSingleSuperclass.class, SearchStrategy.TYPE_HIERARCHY,
				(context, aggregateIndex, source, annotations) -> {
					indexes.add(aggregateIndex);
					return "";
				});
		assertThat(result).isEmpty();
		assertThat(indexes).containsExactly(0);
	}

	@Test
	void scanWhenProcessorHasFinishMethodUsesFinishResult() {
		String result = scan(this, WithSingleSuperclass.class, SearchStrategy.TYPE_HIERARCHY,
				new AnnotationsProcessor<Object, String>() {

					@Override
					@Nullable
					public String doWithAnnotations(Object context, int aggregateIndex,
							Object source, Annotation[] annotations) {
						return "K";
					}

					@Override
					@Nullable
					public String finish(String result) {
						return "O" + result;
					}

				});
		assertThat(result).isEqualTo("OK");
	}


	private Method methodFrom(Class<?> type) {
		return ReflectionUtils.findMethod(type, "method");
	}

	private Stream<String> scan(AnnotatedElement element, SearchStrategy searchStrategy) {
		return scan(element, searchStrategy, Search.never);
	}

	private Stream<String> scan(AnnotatedElement element, SearchStrategy searchStrategy,
			Predicate<Class<?>> searchEnclosingClass) {

		List<String> results = new ArrayList<>();
		scan(this, element, searchStrategy, searchEnclosingClass,
				(criteria, aggregateIndex, source, annotations) -> {
					trackIndexedAnnotations(aggregateIndex, annotations, results);
					return null; // continue searching
				});
		return results.stream();
	}

	private static <C, R> R scan(C context, AnnotatedElement source, SearchStrategy searchStrategy,
			AnnotationsProcessor<C, R> processor) {

		return scan(context, source, searchStrategy, Search.never, processor);
	}

	private static <C, R> R scan(C context, AnnotatedElement source, SearchStrategy searchStrategy,
			Predicate<Class<?>> searchEnclosingClass, AnnotationsProcessor<C, R> processor) {

		return AnnotationsScanner.scan(context, source, searchStrategy, searchEnclosingClass, processor);
	}

	private void trackIndexedAnnotations(int aggregateIndex, Annotation[] annotations, List<String> results) {
		Arrays.stream(annotations)
			.filter(Objects::nonNull)
			.map(annotation -> indexedName(aggregateIndex, annotation))
			.forEach(results::add);
	}

	private String indexedName(int aggregateIndex, Annotation annotation) {
		return aggregateIndex + ":" + annotation.annotationType().getSimpleName();
	}


	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAnnotation1 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAnnotation2 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAnnotation3 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAnnotation4 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAnnotation5 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAnnotation6 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface TestInheritedAnnotation1 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface TestInheritedAnnotation2 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface TestInheritedAnnotation3 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface TestInheritedAnnotation4 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface TestInheritedAnnotation5 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface OnSuperClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface OnInterface {
	}

	static class WithNoAnnotations {

		public void method() {
		}
	}

	@TestAnnotation1
	static class WithSingleAnnotation {

		@TestAnnotation1
		public void method() {
		}
	}

	@TestAnnotation1
	@TestAnnotation2
	static class WithMultipleAnnotations {

		@TestAnnotation1
		@TestAnnotation2
		public void method() {
		}
	}

	@TestAnnotation2
	@TestInheritedAnnotation2
	static class SingleSuperclass {

		@TestAnnotation2
		@TestInheritedAnnotation2
		public void method() {
		}
	}

	@TestAnnotation1
	static class WithSingleSuperclass extends SingleSuperclass {

		@Override
		@TestAnnotation1
		public void method() {
		}
	}

	@TestInheritedAnnotation2
	static class WithSingleSuperclassAndDoubleInherited extends SingleSuperclass {

		@Override
		@TestAnnotation1
		public void method() {
		}
	}

	@TestAnnotation1
	static class WithSingleInterface implements SingleInterface {

		@Override
		@TestAnnotation1
		public void method() {
		}
	}

	@TestAnnotation2
	@TestInheritedAnnotation2
	interface SingleInterface {

		@TestAnnotation2
		@TestInheritedAnnotation2
		void method();
	}

	@TestAnnotation1
	static class WithHierarchy extends HierarchySuperclass implements HierarchyInterface {

		@Override
		@TestAnnotation1
		public void method() {
		}
	}

	interface Hello1 {

		@TestAnnotation1
		void method();
	}

	interface Hello2 extends Hello1 {
	}

	static class Hello1Impl implements Hello1 {

		@Override
		public void method() {
		}
	}

	static class Hello2Impl implements Hello2 {

		@Override
		public void method() {
		}
	}


	@TestAnnotation2
	@TestInheritedAnnotation2
	static class HierarchySuperclass extends HierarchySuperSuperclass {

		@Override
		@TestAnnotation2
		@TestInheritedAnnotation2
		public void method() {
		}
	}

	@TestAnnotation3
	@TestInheritedAnnotation3
	static class HierarchySuperSuperclass implements HierarchySuperSuperclassInterface {

		@Override
		@TestAnnotation3
		public void method() {
		}
	}

	@TestAnnotation4
	interface HierarchySuperSuperclassInterface {

		@TestAnnotation4
		void method();
	}

	@TestAnnotation5
	@TestInheritedAnnotation5
	interface HierarchyInterface extends HierarchyInterfaceInterface {

		@Override
		@TestAnnotation5
		@TestInheritedAnnotation5
		void method();
	}

	@TestAnnotation6
	interface HierarchyInterfaceInterface {

		@TestAnnotation6
		void method();
	}

	static class BridgedMethod implements BridgeMethod<String> {

		@Override
		@TestAnnotation1
		public void method(String arg) {
		}
	}

	interface BridgeMethod<T> {

		@TestAnnotation2
		void method(T arg);
	}

	static class Ignorable implements IgnorableOverrideInterface1, IgnorableOverrideInterface2 {

		@Override
		@TestAnnotation1
		public void method() {
		}
	}

	interface IgnorableOverrideInterface1 {

		@Nullable
		void method();
	}

	interface IgnorableOverrideInterface2 {

		@Nullable
		void method();
	}

	abstract static class MultipleMethods implements MultipleMethodsInterface {

		@TestAnnotation1
		public void method() {
		}
	}

	interface MultipleMethodsInterface {

		@TestAnnotation2
		void method(String arg);

		@TestAnnotation2
		void method1();
	}

	static class GenericOverride implements GenericOverrideInterface<String> {

		@Override
		@TestAnnotation1
		public void method(String argument) {
		}
	}

	interface GenericOverrideInterface<T extends CharSequence> {

		@TestAnnotation2
		void method(T argument);
	}

	abstract static class GenericNonOverride implements GenericNonOverrideInterface<String> {

		@TestAnnotation1
		public void method(StringBuilder argument) {
		}
	}

	interface GenericNonOverrideInterface<T extends CharSequence> {

		@TestAnnotation2
		void method(T argument);
	}

}
