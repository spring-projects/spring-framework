/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.function.Predicate;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KProperty;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Constants that indicate nullness, as well as related utility methods.
 *
 * <p>Nullness applies to type usage, a field, a method return type, or a parameter.
 * <a href="https://jspecify.dev/docs/user-guide/">JSpecify annotations</a> are
 * fully supported, as well as
 * <a href="https://kotlinlang.org/docs/null-safety.html">Kotlin null safety</a>,
 * {@code @Nullable} annotations regardless of their package, and Java primitive
 * types.
 *
 * <p>JSR-305 annotations as well as Spring null safety annotations in the
 * {@code org.springframework.lang} package such as {@code @NonNullApi},
 * {@code @NonNullFields}, and {@code @NonNull} are not supported by this API.
 * However, {@code @Nullable} is supported via the package-less check. Migrating
 * to JSpecify is recommended.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
public enum Nullness {

	/**
	 * Unspecified nullness (Java default for non-primitive types and JSpecify
	 * {@code @NullUnmarked} code).
	 */
	UNSPECIFIED,

	/**
	 * Can include null (typically specified with a {@code @Nullable} annotation).
	 */
	NULLABLE,

	/**
	 * Will not include null (Kotlin default and JSpecify {@code @NullMarked} code).
	 */
	NON_NULL;


	/**
	 * Return the nullness of the return type for the given method.
	 * @param method the source for the method return type
	 * @return the corresponding nullness
	 */
	public static Nullness forMethodReturnType(Method method) {
		if (KotlinDetector.isKotlinType(method.getDeclaringClass())) {
			return KotlinDelegate.forMethodReturnType(method);
		}
		return (hasNullableAnnotation(method) ? Nullness.NULLABLE :
				jSpecifyNullness(method, method.getDeclaringClass(), method.getAnnotatedReturnType()));
	}

	/**
	 * Return the nullness of the given parameter.
	 * @param parameter the parameter descriptor
	 * @return the corresponding nullness
	 */
	public static Nullness forParameter(Parameter parameter) {
		if (KotlinDetector.isKotlinType(parameter.getDeclaringExecutable().getDeclaringClass())) {
			// TODO Optimize when kotlin-reflect provide a more direct Parameter to KParameter resolution
			MethodParameter methodParameter = MethodParameter.forParameter(parameter);
			return KotlinDelegate.forParameter(methodParameter.getExecutable(), methodParameter.getParameterIndex());
		}
		Executable executable = parameter.getDeclaringExecutable();
		return (hasNullableAnnotation(parameter) ? Nullness.NULLABLE :
				jSpecifyNullness(executable, executable.getDeclaringClass(), parameter.getAnnotatedType()));
	}

	/**
	 * Return the nullness of the given method parameter.
	 * @param methodParameter the method parameter descriptor
	 * @return the corresponding nullness
	 */
	public static Nullness forMethodParameter(MethodParameter methodParameter) {
		return (methodParameter.getParameterIndex() < 0 ?
				forMethodReturnType(Objects.requireNonNull(methodParameter.getMethod())) :
				forParameter(methodParameter.getParameter()));
	}

	/**
	 * Return the nullness of the given field.
	 * @param field the field descriptor
	 * @return the corresponding nullness
	 */
	public static Nullness forField(Field field) {
		if (KotlinDetector.isKotlinType(field.getDeclaringClass())) {
			return KotlinDelegate.forField(field);
		}
		return (hasNullableAnnotation(field) ? Nullness.NULLABLE :
				jSpecifyNullness(field, field.getDeclaringClass(), field.getAnnotatedType()));
	}


	// Check method and parameter level @Nullable annotations regardless of the package
	// (including Spring and JSR 305 annotations)
	private static boolean hasNullableAnnotation(AnnotatedElement element) {
		for (Annotation annotation : element.getDeclaredAnnotations()) {
			if ("Nullable".equals(annotation.annotationType().getSimpleName())) {
				return true;
			}
		}
		return false;
	}

	private static Nullness jSpecifyNullness(
			AnnotatedElement annotatedElement, Class<?> declaringClass, AnnotatedType annotatedType) {

		if (annotatedType.getType() instanceof Class<?> clazz && clazz.isPrimitive()) {
			return (clazz != void.class ? Nullness.NON_NULL : Nullness.UNSPECIFIED);
		}
		if (annotatedType.isAnnotationPresent(Nullable.class)) {
			return Nullness.NULLABLE;
		}
		if (annotatedType.isAnnotationPresent(NonNull.class)) {
			return Nullness.NON_NULL;
		}
		Nullness nullness = Nullness.UNSPECIFIED;
		// Package level
		Package declaringPackage = declaringClass.getPackage();
		if (declaringPackage.isAnnotationPresent(NullMarked.class)) {
			nullness = Nullness.NON_NULL;
		}
		// Class level
		if (declaringClass.isAnnotationPresent(NullMarked.class)) {
			nullness = Nullness.NON_NULL;
		}
		else if (declaringClass.isAnnotationPresent(NullUnmarked.class)) {
			nullness = Nullness.UNSPECIFIED;
		}
		// Annotated element level
		if (annotatedElement.isAnnotationPresent(NullMarked.class)) {
			nullness = Nullness.NON_NULL;
		}
		else if (annotatedElement.isAnnotationPresent(NullUnmarked.class)) {
			nullness = Nullness.UNSPECIFIED;
		}
		return nullness;
	}

	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static class KotlinDelegate {

		public static Nullness forMethodReturnType(Method method) {
			KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
			if (function != null && function.getReturnType().isMarkedNullable()) {
				return Nullness.NULLABLE;
			}
			return Nullness.NON_NULL;
		}

		public static Nullness forParameter(Executable executable, int parameterIndex) {
			KFunction<?> function;
			Predicate<KParameter> predicate;
			if (executable instanceof Method method) {
				function = ReflectJvmMapping.getKotlinFunction(method);
				predicate = p -> KParameter.Kind.VALUE.equals(p.getKind());
			}
			else {
				function = ReflectJvmMapping.getKotlinFunction((Constructor<?>) executable);
				predicate = p -> (KParameter.Kind.VALUE.equals(p.getKind()) ||
						KParameter.Kind.INSTANCE.equals(p.getKind()));
			}
			if (function == null) {
				return Nullness.UNSPECIFIED;
			}
			int i = 0;
			for (KParameter kParameter : function.getParameters()) {
				if (predicate.test(kParameter) && parameterIndex == i++) {
					return (kParameter.getType().isMarkedNullable() ? Nullness.NULLABLE : Nullness.NON_NULL);
				}
			}
			return Nullness.UNSPECIFIED;
		}

		public static Nullness forField(Field field) {
			KProperty<?> property = ReflectJvmMapping.getKotlinProperty(field);
			if (property != null && property.getReturnType().isMarkedNullable()) {
				return Nullness.NULLABLE;
			}
			return Nullness.NON_NULL;
		}

	}

}
