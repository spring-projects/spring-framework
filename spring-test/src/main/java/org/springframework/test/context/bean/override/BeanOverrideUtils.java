/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.bean.override;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Utility methods for working with bean overrides.
 *
 * <p>Primarily intended for use within the framework.
 *
 * @author Sam Brannen
 * @since 7.1
 */
public abstract class BeanOverrideUtils {

	private static final boolean KOTLIN_REFLECT_PRESENT = KotlinDetector.isKotlinReflectPresent();

	private static final Comparator<MergedAnnotation<? extends Annotation>> reversedMetaDistance =
			Comparator.<MergedAnnotation<? extends Annotation>> comparingInt(MergedAnnotation::getDistance).reversed();


	/**
	 * Resolve the {@link BeanOverrideHandler} for the given {@link Parameter}.
	 * @param parameter the parameter to process
	 * @param testClass the test class to process
	 * @return the bean override handler for the parameter, or {@code null} if no
	 * handler was found
	 * @see BeanOverrideProcessor#createHandler(Annotation, Class, Parameter)
	 * @see #findAllHandlers(Class)
	 */
	public static @Nullable BeanOverrideHandler resolveHandlerForParameter(Parameter parameter, Class<?> testClass) {
		AtomicReference<BeanOverrideHandler> handlerReference = new AtomicReference<>();
		AtomicBoolean overrideAnnotationFound = new AtomicBoolean();
		processElement(parameter, (processor, composedAnnotation) -> {
			Assert.state(!overrideAnnotationFound.getPlain(),
					() -> "Multiple @BeanOverride annotations found on parameter: " + parameter);
			overrideAnnotationFound.setPlain(true);
			BeanOverrideHandler handler = processor.createHandler(composedAnnotation, testClass, parameter);
			Assert.state(handler != null,
					() -> "BeanOverrideProcessor [%s] returned null BeanOverrideHandler for parameter [%s]"
							.formatted(processor.getClass().getSimpleName(), parameter));
			handlerReference.setPlain(handler);
		});
		return handlerReference.getPlain();
	}

	/**
	 * Process the given {@code testClass} and build the corresponding
	 * {@link BeanOverrideHandler} list derived from {@link BeanOverride @BeanOverride}
	 * fields in the test class and its type hierarchy.
	 * <p>This method does not search the enclosing class hierarchy, does not
	 * search for {@code @BeanOverride} declarations on classes or interfaces, and
	 * does not search for {@code @BeanOverride} declarations on constructor
	 * parameters.
	 * @param testClass the test class to process
	 * @return a list of bean override handlers
	 * @see #findAllHandlers(Class)
	 */
	public static List<BeanOverrideHandler> findHandlersForFields(Class<?> testClass) {
		return findHandlers(testClass, true);
	}

	/**
	 * Process the given {@code testClass} and build the corresponding
	 * {@link BeanOverrideHandler} list derived from {@link BeanOverride @BeanOverride}
	 * fields in the test class and in its type hierarchy as well as from
	 * {@code @BeanOverride} declarations on classes and interfaces and
	 * {@code @BeanOverride} declarations on constructor parameters.
	 * <p>This method additionally searches for {@code @BeanOverride} declarations
	 * in the enclosing class hierarchy based on
	 * {@link TestContextAnnotationUtils#searchEnclosingClass(Class)} semantics.
	 * @param testClass the test class to process
	 * @return a list of bean override handlers
	 * @see #findHandlersForFields(Class)
	 */
	public static List<BeanOverrideHandler> findAllHandlers(Class<?> testClass) {
		return findHandlers(testClass, false);
	}

	private static List<BeanOverrideHandler> findHandlers(Class<?> testClass, boolean localFieldsOnly) {
		List<BeanOverrideHandler> handlers = new ArrayList<>();
		findHandlers(testClass, testClass, handlers, localFieldsOnly, false, new HashSet<>());
		return handlers;
	}

	/**
	 * Find handlers using tail recursion to ensure that "locally declared" bean overrides
	 * take precedence over inherited bean overrides.
	 * <p>Note: the search algorithm is effectively the inverse of the algorithm used in
	 * {@link org.springframework.test.context.TestContextAnnotationUtils#findAnnotationDescriptor(Class, Class)},
	 * but with tail recursion the semantics should be the same.
	 * @param clazz the class in/on which to search
	 * @param testClass the original test class
	 * @param handlers the list of handlers found
	 * @param localFieldsOnly whether to search only on local fields within the type hierarchy
	 * @param fromNestedClass whether the search originated from a nested test class
	 * @param visitedTypes the set of types already visited
	 * @since 6.2.2
	 */
	private static void findHandlers(Class<?> clazz, Class<?> testClass, List<BeanOverrideHandler> handlers,
			boolean localFieldsOnly, boolean fromNestedClass, Set<Class<?>> visitedTypes) {

		// 0) Ensure that we do not process the same class or interface multiple times.
		if (!visitedTypes.add(clazz)) {
			return;
		}

		// 1) Search enclosing class hierarchy.
		if (!localFieldsOnly && TestContextAnnotationUtils.searchEnclosingClass(clazz)) {
			findHandlers(clazz.getEnclosingClass(), testClass, handlers, localFieldsOnly, true, visitedTypes);
		}

		// 2) Search class hierarchy.
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null && superclass != Object.class) {
			findHandlers(superclass, testClass, handlers, localFieldsOnly, false, visitedTypes);
		}

		if (!localFieldsOnly) {
			// 3) Search interfaces.
			for (Class<?> ifc : clazz.getInterfaces()) {
				findHandlers(ifc, testClass, handlers, localFieldsOnly, false, visitedTypes);
			}

			// 4) Process current class.
			processClass(clazz, testClass, handlers);

			// 5) Process test class constructor parameters.
			// Specifically, we only process the constructor for the current test class
			// and enclosing test classes. In other words, we do not process constructors
			// for superclasses.
			if (testClass == clazz || fromNestedClass) {
				Constructor<?> constructor = findConstructorWithParameters(clazz);
				if (constructor != null) {
					for (Parameter parameter : constructor.getParameters()) {
						processParameter(parameter, testClass, handlers);
					}
				}
			}
		}

		// 6) Process fields in current class.
		ReflectionUtils.doWithLocalFields(clazz, field -> processField(field, testClass, handlers));
	}

	private static void processClass(Class<?> clazz, Class<?> testClass, List<BeanOverrideHandler> handlers) {
		processElement(clazz, (processor, composedAnnotation) ->
				processor.createHandlers(composedAnnotation, testClass).forEach(handlers::add));
	}

	private static void processParameter(Parameter parameter, Class<?> testClass, List<BeanOverrideHandler> handlers) {
		BeanOverrideHandler handler = resolveHandlerForParameter(parameter, testClass);
		if (handler != null) {
			handlers.add(handler);
		}
	}

	private static void processField(Field field, Class<?> testClass, List<BeanOverrideHandler> handlers) {
		Class<?> declaringClass = field.getDeclaringClass();
		// For Java records, the Java compiler propagates @BeanOverride annotations from
		// canonical constructor parameters to the corresponding component fields, resulting
		// in duplicates. Similarly for Kotlin types, the Kotlin compiler propagates
		// @BeanOverride annotations from primary constructor parameters to their corresponding
		// backing fields, resulting in duplicates. Thus, if we detect either of those scenarios,
		// we ignore the field.
		if (declaringClass.isRecord() || (KOTLIN_REFLECT_PRESENT &&
				KotlinDetector.isKotlinType(declaringClass) &&
				KotlinDelegate.isFieldForBeanOverrideConstructorParameter(field))) {
			return;
		}

		AtomicBoolean overrideAnnotationFound = new AtomicBoolean();
		processElement(field, (processor, composedAnnotation) -> {
			Assert.state(!Modifier.isStatic(field.getModifiers()),
					() -> "@BeanOverride field must not be static: " + field);
			Assert.state(!overrideAnnotationFound.getPlain(),
					() -> "Multiple @BeanOverride annotations found on field: " + field);
			overrideAnnotationFound.setPlain(true);
			handlers.add(processor.createHandler(composedAnnotation, testClass, field));
		});
	}

	private static void processElement(AnnotatedElement element, BiConsumer<BeanOverrideProcessor, Annotation> consumer) {
		MergedAnnotations.from(element)
				.stream(BeanOverride.class)
				.sorted(reversedMetaDistance)
				.forEach(mergedAnnotation -> {
					MergedAnnotation<?> metaSource = mergedAnnotation.getMetaSource();
					Assert.state(metaSource != null, "@BeanOverride annotation must be meta-present");

					BeanOverride beanOverride = mergedAnnotation.synthesize();
					BeanOverrideProcessor processor = BeanUtils.instantiateClass(beanOverride.value());
					Annotation composedAnnotation = metaSource.synthesize();
					consumer.accept(processor, composedAnnotation);
				});
	}

	/**
	 * Find a single constructor for the supplied test class that declares parameters.
	 * <p>If the test class declares multiple constructors, this method returns
	 * {@code null}.
	 * @param testClass the test class to process
	 * @return the candidate constructor, or {@code null} if no suitable candidate
	 * was found
	 */
	private static @Nullable Constructor<?> findConstructorWithParameters(Class<?> testClass) {
		List<Constructor<?>> constructors = Arrays.stream(testClass.getDeclaredConstructors())
				.filter(constructor -> !constructor.isSynthetic() && constructor.getParameterCount() > 0)
				.toList();
		return (constructors.size() == 1 ? constructors.get(0) : null);
	}


	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static class KotlinDelegate {

		/**
		 * Determine if the supplied field corresponds to a primary constructor
		 * parameter in the field's declaring Kotlin class, where the primary
		 * constructor parameter is annotated with {@link BeanOverride @BeanOverride}.
		 */
		static boolean isFieldForBeanOverrideConstructorParameter(Field field) {
			KClass<?> kClass = JvmClassMappingKt.getKotlinClass(field.getDeclaringClass());
			KFunction<?> primaryConstructor = KClasses.getPrimaryConstructor(kClass);
			if (primaryConstructor == null) {
				return false;
			}
			Constructor<?> javaConstructor = ReflectJvmMapping.getJavaConstructor(primaryConstructor);
			if (javaConstructor == null) {
				return false;
			}
			String fieldName = field.getName();
			for (Parameter parameter : javaConstructor.getParameters()) {
				if (parameter.getName().equals(fieldName) &&
						MergedAnnotations.from(parameter).isPresent(BeanOverride.class)) {
					return true;
				}
			}
			return false;
		}
	}

}
