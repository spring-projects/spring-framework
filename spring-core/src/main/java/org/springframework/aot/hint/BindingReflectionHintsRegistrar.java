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

package org.springframework.aot.hint;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;

import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Register the necessary reflection hints so that the specified type can be
 * bound at runtime. Fields, constructors, properties and record components
 * are registered, except for a set of types like those in the {@code java.}
 * package where just the type is registered. Types are discovered transitively
 * on properties and record components, and generic types are registered as well.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
public class BindingReflectionHintsRegistrar {

	private static final String KOTLIN_COMPANION_SUFFIX = "$Companion";

	private static final String JACKSON_ANNOTATION = "com.fasterxml.jackson.annotation.JacksonAnnotation";

	private static final boolean jacksonAnnotationPresent = ClassUtils.isPresent(JACKSON_ANNOTATION,
			BindingReflectionHintsRegistrar.class.getClassLoader());

	/**
	 * Register the necessary reflection hints to bind the specified types.
	 * @param hints the hints instance to use
	 * @param types the types to register
	 */
	public void registerReflectionHints(ReflectionHints hints, Type... types) {
		Set<Type> seen = new LinkedHashSet<>();
		for (Type type : types) {
			registerReflectionHints(hints, seen, type);
		}
	}

	private boolean shouldSkipType(Class<?> type) {
		return type.isPrimitive() || type == Object.class;
	}

	private boolean shouldSkipMembers(Class<?> type) {
		return type.getCanonicalName().startsWith("java.") || type.isArray();
	}

	private void registerReflectionHints(ReflectionHints hints, Set<Type> seen, Type type) {
		if (seen.contains(type)) {
			return;
		}
		seen.add(type);
		if (type instanceof Class<?> clazz) {
			if (shouldSkipType(clazz)) {
				return;
			}
			hints.registerType(clazz, typeHint -> {
				if (!shouldSkipMembers(clazz)) {
					if (clazz.isRecord()) {
						typeHint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
						for (RecordComponent recordComponent : clazz.getRecordComponents()) {
							registerRecordHints(hints, seen, recordComponent.getAccessor());
						}
					}
					typeHint.withMembers(
							MemberCategory.DECLARED_FIELDS,
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
					for (Method method : clazz.getMethods()) {
						String methodName = method.getName();
						if (methodName.startsWith("set") && method.getParameterCount() == 1) {
							registerPropertyHints(hints, seen, method, 0);
						}
						else if ((methodName.startsWith("get") && method.getParameterCount() == 0 && method.getReturnType() != Void.TYPE) ||
								(methodName.startsWith("is") && method.getParameterCount() == 0 && method.getReturnType() == boolean.class)) {
							registerPropertyHints(hints, seen, method, -1);
						}
					}
					if (jacksonAnnotationPresent) {
						registerJacksonHints(hints, clazz);
					}
				}
				if (KotlinDetector.isKotlinType(clazz)) {
					KotlinDelegate.registerComponentHints(hints, clazz);
					registerKotlinSerializationHints(hints, clazz);
					// For Kotlin reflection
					typeHint.withMembers(MemberCategory.INTROSPECT_DECLARED_METHODS);
				}
			});
		}
		Set<Class<?>> referencedTypes = new LinkedHashSet<>();
		collectReferencedTypes(referencedTypes, ResolvableType.forType(type));
		referencedTypes.forEach(referencedType -> registerReflectionHints(hints, seen, referencedType));
	}

	private void registerRecordHints(ReflectionHints hints, Set<Type> seen, Method method) {
		hints.registerMethod(method, ExecutableMode.INVOKE);
		MethodParameter methodParameter = MethodParameter.forExecutable(method, -1);
		Type methodParameterType = methodParameter.getGenericParameterType();
		registerReflectionHints(hints, seen, methodParameterType);
	}

	private void registerPropertyHints(ReflectionHints hints, Set<Type> seen, @Nullable Method method, int parameterIndex) {
		if (method != null && method.getDeclaringClass() != Object.class &&
				method.getDeclaringClass() != Enum.class) {
			hints.registerMethod(method, ExecutableMode.INVOKE);
			MethodParameter methodParameter = MethodParameter.forExecutable(method, parameterIndex);
			Type methodParameterType = methodParameter.getGenericParameterType();
			registerReflectionHints(hints, seen, methodParameterType);
		}
	}

	private void registerKotlinSerializationHints(ReflectionHints hints, Class<?> clazz) {
		String companionClassName = clazz.getCanonicalName() + KOTLIN_COMPANION_SUFFIX;
		if (ClassUtils.isPresent(companionClassName, null)) {
			Class<?> companionClass = ClassUtils.resolveClassName(companionClassName, null);
			Method serializerMethod = ClassUtils.getMethodIfAvailable(companionClass, "serializer");
			if (serializerMethod != null) {
				hints.registerMethod(serializerMethod, ExecutableMode.INVOKE);
			}
		}
	}

	private void collectReferencedTypes(Set<Class<?>> types, ResolvableType resolvableType) {
		Class<?> clazz = resolvableType.resolve();
		if (clazz != null && !types.contains(clazz)) {
			types.add(clazz);
			for (ResolvableType genericResolvableType : resolvableType.getGenerics()) {
				collectReferencedTypes(types, genericResolvableType);
			}
		}
	}

	private void registerJacksonHints(ReflectionHints hints, Class<?> clazz) {
		ReflectionUtils.doWithFields(clazz, field ->
				forEachJacksonAnnotation(field, annotation -> {
							Field sourceField = (Field) annotation.getSource();
							if (sourceField != null) {
								hints.registerField(sourceField);
							}
							registerHintsForClassAttributes(hints, annotation);
						}));
		ReflectionUtils.doWithMethods(clazz, method ->
				forEachJacksonAnnotation(method, annotation -> {
							Method sourceMethod = (Method) annotation.getSource();
							if (sourceMethod != null) {
								hints.registerMethod(sourceMethod, ExecutableMode.INVOKE);
							}
							registerHintsForClassAttributes(hints, annotation);
						}));
		forEachJacksonAnnotation(clazz, annotation -> registerHintsForClassAttributes(hints, annotation));
	}

	private void forEachJacksonAnnotation(AnnotatedElement element, Consumer<MergedAnnotation<Annotation>> action) {
		MergedAnnotations
				.from(element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
				.stream(JACKSON_ANNOTATION)
				.filter(MergedAnnotation::isMetaPresent)
				.forEach(action::accept);
	}

	private void registerHintsForClassAttributes(ReflectionHints hints, MergedAnnotation<Annotation> annotation) {
		annotation.getRoot().asMap().values().forEach(value -> {
			if (value instanceof Class<?> classValue && value != Void.class) {
				hints.registerType(classValue, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			}
		});
	}

	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static class KotlinDelegate {

		public static void registerComponentHints(ReflectionHints hints, Class<?> type) {
			KClass<?> kClass = JvmClassMappingKt.getKotlinClass(type);
			if (kClass.isData()) {
				for (Method method : type.getMethods()) {
					String methodName = method.getName();
					if (methodName.startsWith("component") || methodName.equals("copy") || methodName.equals("copy$default")) {
						hints.registerMethod(method, ExecutableMode.INVOKE);
					}
				}
			}
		}
	}

}
