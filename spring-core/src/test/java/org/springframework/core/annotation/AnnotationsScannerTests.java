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

package org.springframework.core.annotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link AnnotationsScanner}.
 *
 * @author Phillip Webb
 */
public class AnnotationsScannerTests {

	@Test
	public void directStrategyOnClassWhenNotAnnoatedScansNone() {
		Class<?> source = WithNoAnnotations.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).isEmpty();
	}

	@Test
	public void directStrategyOnClassScansAnnotations() {
		Class<?> source = WithSingleAnnotation.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void directStrategyOnClassWhenMultipleAnnotationsScansAnnotations() {
		Class<?> source = WithMultipleAnnotations.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	public void directStrategyOnClassWhenHasSuperclassScansOnlyDirect() {
		Class<?> source = WithSingleSuperclass.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void directStrategyOnClassWhenHasInterfaceScansOnlyDirect() {
		Class<?> source = WithSingleInterface.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void directStrategyOnClassHierarchyScansInCorrectOrder() {
		Class<?> source = WithHierarchy.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void inheritedAnnotationsStrategyOnClassWhenNotAnnoatedScansNone() {
		Class<?> source = WithNoAnnotations.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).isEmpty();
	}

	@Test
	public void inheritedAnnotationsStrategyOnClassScansAnnotations() {
		Class<?> source = WithSingleAnnotation.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void inheritedAnnotationsStrategyOnClassWhenMultipleAnnotationsScansAnnotations() {
		Class<?> source = WithMultipleAnnotations.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	public void inheritedAnnotationsStrategyOnClassWhenHasSuperclassScansOnlyInherited() {
		Class<?> source = WithSingleSuperclass.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1", "1:TestInheritedAnnotation2");
	}

	@Test
	public void inheritedAnnotationsStrategyOnClassWhenHasInterfaceDoesNotIncludeInterfaces() {
		Class<?> source = WithSingleInterface.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void inheritedAnnotationsStrategyOnClassHierarchyScansInCorrectOrder() {
		Class<?> source = WithHierarchy.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1", "1:TestInheritedAnnotation2");
	}

	@Test
	public void inheritedAnnotationsStrategyOnClassWhenHasAnnotationOnBothClassesIncudesOnlyOne() {
		Class<?> source = WithSingleSuperclassAndDoubleInherited.class;
		assertThat(Arrays.stream(source.getAnnotations()).map(
				Annotation::annotationType).map(Class::getName)).containsExactly(
						TestInheritedAnnotation2.class.getName());
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsOnly(
				"0:TestInheritedAnnotation2");
	}

	@Test
	public void superclassStrategyOnClassWhenNotAnnoatedScansNone() {
		Class<?> source = WithNoAnnotations.class;
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).isEmpty();
	}

	@Test
	public void superclassStrategyOnClassScansAnnotations() {
		Class<?> source = WithSingleAnnotation.class;
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void superclassStrategyOnClassWhenMultipleAnnotationsScansAnnotations() {
		Class<?> source = WithMultipleAnnotations.class;
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	public void superclassStrategyOnClassWhenHasSuperclassScansSuperclass() {
		Class<?> source = WithSingleSuperclass.class;
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	public void superclassStrategyOnClassWhenHasInterfaceDoesNotIncludeInterfaces() {
		Class<?> source = WithSingleInterface.class;
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void superclassStrategyOnClassHierarchyScansInCorrectOrder() {
		Class<?> source = WithHierarchy.class;
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2",
				"2:TestAnnotation3");
	}

	@Test
	public void exhaustiveStrategyOnClassWhenNotAnnoatedScansNone() {
		Class<?> source = WithNoAnnotations.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).isEmpty();
	}

	@Test
	public void exhaustiveStrategyOnClassScansAnnotations() {
		Class<?> source = WithSingleAnnotation.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void exhaustiveStrategyOnClassWhenMultipleAnnotationsScansAnnotations() {
		Class<?> source = WithMultipleAnnotations.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	public void exhaustiveStrategyOnClassWhenHasSuperclassScansSuperclass() {
		Class<?> source = WithSingleSuperclass.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	public void exhaustiveStrategyOnClassWhenHasInterfaceDoesNotIncludeInterfaces() {
		Class<?> source = WithSingleInterface.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	public void exhaustiveStrategyOnClassHierarchyScansInCorrectOrder() {
		Class<?> source = WithHierarchy.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation5", "1:TestInheritedAnnotation5",
				"2:TestAnnotation6", "3:TestAnnotation2", "3:TestInheritedAnnotation2",
				"4:TestAnnotation3", "5:TestAnnotation4");
	}

	@Test
	public void directStrategyOnMethodWhenNotAnnoatedScansNone() {
		Method source = methodFrom(WithNoAnnotations.class);
		assertThat(scan(source, SearchStrategy.DIRECT)).isEmpty();
	}

	@Test
	public void directStrategyOnMethodScansAnnotations() {
		Method source = methodFrom(WithSingleAnnotation.class);
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void directStrategyOnMethodWhenMultipleAnnotationsScansAnnotations() {
		Method source = methodFrom(WithMultipleAnnotations.class);
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	public void directStrategyOnMethodWhenHasSuperclassScansOnlyDirect() {
		Method source = methodFrom(WithSingleSuperclass.class);
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void directStrategyOnMethodWhenHasInterfaceScansOnlyDirect() {
		Method source = methodFrom(WithSingleInterface.class);
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void directStrategyOnMethodHierarchyScansInCorrectOrder() {
		Method source = methodFrom(WithHierarchy.class);
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void inheritedAnnotationsStrategyOnMethodWhenNotAnnoatedScansNone() {
		Method source = methodFrom(WithNoAnnotations.class);
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).isEmpty();
	}

	@Test
	public void inheritedAnnotationsStrategyOnMethodScansAnnotations() {
		Method source = methodFrom(WithSingleAnnotation.class);
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void inheritedAnnotationsStrategyOnMethodWhenMultipleAnnotationsScansAnnotations() {
		Method source = methodFrom(WithMultipleAnnotations.class);
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	public void inheritedAnnotationsMethodOnMethodWhenHasSuperclassIgnoresInherited() {
		Method source = methodFrom(WithSingleSuperclass.class);
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void inheritedAnnotationsStrategyOnMethodWhenHasInterfaceDoesNotIncludeInterfaces() {
		Method source = methodFrom(WithSingleInterface.class);
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void inheritedAnnotationsStrategyOnMethodHierarchyScansInCorrectOrder() {
		Method source = methodFrom(WithHierarchy.class);
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void superclassStrategyOnMethodWhenNotAnnoatedScansNone() {
		Method source = methodFrom(WithNoAnnotations.class);
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).isEmpty();
	}

	@Test
	public void superclassStrategyOnMethodScansAnnotations() {
		Method source = methodFrom(WithSingleAnnotation.class);
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void superclassStrategyOnMethodWhenMultipleAnnotationsScansAnnotations() {
		Method source = methodFrom(WithMultipleAnnotations.class);
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	public void superclassStrategyOnMethodWhenHasSuperclassScansSuperclass() {
		Method source = methodFrom(WithSingleSuperclass.class);
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	public void superclassStrategyOnMethodWhenHasInterfaceDoesNotIncludeInterfaces() {
		Method source = methodFrom(WithSingleInterface.class);
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void superclassStrategyOnMethodHierarchyScansInCorrectOrder() {
		Method source = methodFrom(WithHierarchy.class);
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2",
				"2:TestAnnotation3");
	}

	@Test
	public void exhaustiveStrategyOnMethodWhenNotAnnoatedScansNone() {
		Method source = methodFrom(WithNoAnnotations.class);
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).isEmpty();
	}

	@Test
	public void exhaustiveStrategyOnMethodScansAnnotations() {
		Method source = methodFrom(WithSingleAnnotation.class);
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void exhaustiveStrategyOnMethodWhenMultipleAnnotationsScansAnnotations() {
		Method source = methodFrom(WithMultipleAnnotations.class);
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	public void exhaustiveStrategyOnMethodWhenHasSuperclassScansSuperclass() {
		Method source = methodFrom(WithSingleSuperclass.class);
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	public void exhaustiveStrategyOnMethodWhenHasInterfaceDoesNotIncludeInterfaces() {
		Method source = methodFrom(WithSingleInterface.class);
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	public void exhaustiveStrategyOnMethodHierarchyScansInCorrectOrder() {
		Method source = methodFrom(WithHierarchy.class);
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation5", "1:TestInheritedAnnotation5",
				"2:TestAnnotation6", "3:TestAnnotation2", "3:TestInheritedAnnotation2",
				"4:TestAnnotation3", "5:TestAnnotation4");
	}

	@Test
	public void exhaustiveStrategyOnBridgeMethodScansAnnotations() throws Exception {
		Method source = BridgedMethod.class.getDeclaredMethod("method", Object.class);
		assertThat(source.isBridge()).isTrue();
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2");
	}

	@Test
	public void exhaustiveStrategyOnBridgedMethodScansAnnotations() throws Exception {
		Method source = BridgedMethod.class.getDeclaredMethod("method", String.class);
		assertThat(source.isBridge()).isFalse();
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2");
	}

	@Test
	public void directStrategyOnBridgeMethodScansAnnotations() throws Exception {
		Method source = BridgedMethod.class.getDeclaredMethod("method", Object.class);
		assertThat(source.isBridge()).isTrue();
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void dirextStrategyOnBridgedMethodScansAnnotations() throws Exception {
		Method source = BridgedMethod.class.getDeclaredMethod("method", String.class);
		assertThat(source.isBridge()).isFalse();
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void exhaustiveStrategyOnMethodWithIgnorablesScansAnnotations()
			throws Exception {
		Method source = methodFrom(Ignoreable.class);
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void exhaustiveStrategyOnMethodWithMultipleCandidatesScansAnnotations()
			throws Exception {
		Method source = methodFrom(MultipleMethods.class);
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void exhaustiveStrategyOnMethodWithGenericParameterOverrideScansAnnotations()
			throws Exception {
		Method source = ReflectionUtils.findMethod(GenericOverride.class, "method",
				String.class);
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2");
	}

	@Test
	public void exhaustiveStrategyOnMethodWithGenericParameterNonOverrideScansAnnotations()
			throws Exception {
		Method source = ReflectionUtils.findMethod(GenericNonOverride.class, "method",
				StringBuilder.class);
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void scanWhenProcessorReturnsFromDoWithAggregateExitsEarly() {
		String result = AnnotationsScanner.scan(this, WithSingleSuperclass.class,
				SearchStrategy.EXHAUSTIVE, new AnnotationsProcessor<Object, String>() {

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
	public void scanWhenProcessorReturnsFromDoWithAnnotationsExitsEarly() {
		List<Integer> indexes = new ArrayList<>();
		String result = AnnotationsScanner.scan(this, WithSingleSuperclass.class,
				SearchStrategy.EXHAUSTIVE,
				(context, aggregateIndex, source, annotations) -> {
					indexes.add(aggregateIndex);
					return "";
				});
		assertThat(result).isEmpty();
		assertThat(indexes).containsOnly(0);
	}

	@Test
	public void scanWhenProcessorHasFinishMethodUsesFinishResult() {
		String result = AnnotationsScanner.scan(this, WithSingleSuperclass.class,
				SearchStrategy.EXHAUSTIVE, new AnnotationsProcessor<Object, String>() {

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
		List<String> result = new ArrayList<>();
		AnnotationsScanner.scan(this, element, searchStrategy,
				(criteria, aggregateIndex, source, annotations) -> {
					for (Annotation annotation : annotations) {
						if (annotation != null) {
							String name = ClassUtils.getShortName(
									annotation.annotationType());
							name = name.substring(name.lastIndexOf(".") + 1);
							result.add(aggregateIndex + ":" + name);
						}
					}
					return null;
				});
		return result.stream();
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

		@TestAnnotation1
		public void method() {
		}
	}

	@TestInheritedAnnotation2
	static class WithSingleSuperclassAndDoubleInherited extends SingleSuperclass {

		@TestAnnotation1
		public void method() {
		}
	}

	@TestAnnotation1
	static class WithSingleInterface implements SingleInterface {

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

		@TestAnnotation1
		public void method() {
		}
	}

	@TestAnnotation2
	@TestInheritedAnnotation2
	static class HierarchySuperclass extends HierarchySuperSuperclass {

		@TestAnnotation2
		@TestInheritedAnnotation2
		public void method() {
		}
	}

	@TestAnnotation3
	static class HierarchySuperSuperclass implements HierarchySuperSuperclassInterface {

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

	@SuppressWarnings("serial")
	static class Ignoreable implements IgnoreableOverrideInterface1, IgnoreableOverrideInterface2, Serializable {

		@TestAnnotation1
		public void method() {
		}
	}

	interface IgnoreableOverrideInterface1 {

		@Nullable
		void method();
	}

	interface IgnoreableOverrideInterface2 {

		@Nullable
		void method();
	}

	static abstract class MultipleMethods implements MultipleMethodsInterface {

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

		@TestAnnotation1
		public void method(String argument) {
		}
	}

	interface GenericOverrideInterface<T extends CharSequence> {

		@TestAnnotation2
		void method(T argument);
	}

	static abstract class GenericNonOverride
			implements GenericNonOverrideInterface<String> {

		@TestAnnotation1
		public void method(StringBuilder argument) {
		}
	}

	interface GenericNonOverrideInterface<T extends CharSequence> {

		@TestAnnotation2
		void method(T argument);
	}

}
