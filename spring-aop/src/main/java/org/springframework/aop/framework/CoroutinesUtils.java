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

package org.springframework.aop.framework;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kotlin.coroutines.Continuation;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KType;
import kotlin.reflect.KTypeParameter;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.KTypesJvm;
import kotlin.reflect.jvm.ReflectJvmMapping;
import kotlinx.coroutines.reactive.ReactiveFlowKt;
import kotlinx.coroutines.reactor.MonoKt;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * Package-visible class designed to avoid a hard dependency on Kotlin and Coroutines dependency at runtime.
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
abstract class CoroutinesUtils {

	private static final String COROUTINES_FLOW_CLASS_NAME = "kotlinx.coroutines.flow.Flow";

	private static final Method NO_UNBOX_METHOD;

	private static final Map<Method, Method> unboxMethodCache = new ConcurrentReferenceHashMap<>();

	static {
		try {
			NO_UNBOX_METHOD = CoroutinesUtils.class.getDeclaredMethod("noUnboxMethod");
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Expected method not found: " + ex);
		}
	}

	static Object asFlow(@Nullable Object publisher) {
		if (publisher instanceof Publisher<?> rsPublisher) {
			return ReactiveFlowKt.asFlow(rsPublisher);
		}
		else {
			throw new IllegalArgumentException("Not a Reactive Streams Publisher: " + publisher);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static @Nullable Object awaitSingleOrNull(@Nullable Object value, Object continuation) {
		return MonoKt.awaitSingleOrNull(value instanceof Mono mono ? mono : Mono.justOrEmpty(value),
				(Continuation<Object>) continuation);
	}

	/**
	 * Adapt the return value of a proxied suspending function: convert it to a
	 * {@code Flow} or await its completion, unboxing Kotlin value classes when the
	 * caller expects their unboxed representation.
	 * @param method the suspending function
	 * @param returnValue the return value, typically a {@code Publisher}
	 * @param continuation the {@code Continuation} argument of the suspending function
	 * @return the adapted return value
	 */
	static @Nullable Object adaptReturnValue(Method method, @Nullable Object returnValue, Object continuation) {
		MethodParameter returnParameter = new MethodParameter(method, -1);
		Class<?> returnParameterType = returnParameter.getParameterType();
		if (COROUTINES_FLOW_CLASS_NAME.equals(returnParameterType.getName())) {
			return asFlow(returnValue);
		}
		Object result = awaitSingleOrNull(returnValue, continuation);
		if (KotlinDetector.isInlineClass(returnParameterType) && returnParameterType.isInstance(result)) {
			Method unboxMethod = unboxMethodCache.computeIfAbsent(method, key ->
					findUnboxMethod(key, returnParameterType, returnParameter.isOptional()));
			if (unboxMethod != NO_UNBOX_METHOD) {
				return ReflectionUtils.invokeMethod(unboxMethod, result);
			}
		}
		return result;
	}

	private static Method findUnboxMethod(Method method, Class<?> valueClass, boolean nullableReturnType) {
		Method unboxMethod = ReflectionUtils.findMethod(valueClass, "unbox-impl");
		if (unboxMethod == null || unboxMethod.getReturnType().isPrimitive() ||
				(nullableReturnType && isUnderlyingTypeNullable(valueClass)) ||
				overridesFunctionWithDifferentReturnType(method, valueClass)) {
			return NO_UNBOX_METHOD;
		}
		ReflectionUtils.makeAccessible(unboxMethod);
		return unboxMethod;
	}

	private static boolean overridesFunctionWithDifferentReturnType(Method method, Class<?> valueClass) {
		KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
		if (function == null) {
			return false;
		}
		KClass<?> valueKClass = JvmClassMappingKt.getKotlinClass(valueClass);
		for (KType superType : KClasses.getAllSupertypes(JvmClassMappingKt.getKotlinClass(method.getDeclaringClass()))) {
			if (superType.getClassifier() instanceof KClass<?> superClass) {
				for (KFunction<?> candidate : KClasses.getDeclaredMemberFunctions(superClass)) {
					if (candidate.getName().equals(function.getName()) && candidate.isSuspend() &&
							hasSameParameterTypes(function, candidate, superType) &&
							candidate.getReturnType().getClassifier() != valueKClass) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean hasSameParameterTypes(KFunction<?> function, KFunction<?> candidate, KType superType) {
		List<KParameter> parameters = function.getParameters();
		List<KParameter> candidateParameters = candidate.getParameters();
		if (parameters.size() != candidateParameters.size()) {
			return false;
		}
		for (int i = 1; i < parameters.size(); i++) {
			KType candidateType = candidateParameters.get(i).getType();
			if (candidateType.getClassifier() instanceof KTypeParameter typeParameter &&
					superType.getClassifier() instanceof KClass<?> superClass) {
				int index = superClass.getTypeParameters().indexOf(typeParameter);
				KType argumentType = (index != -1 ? superType.getArguments().get(index).getType() : null);
				if (argumentType != null) {
					candidateType = argumentType;
				}
			}
			if (!KTypesJvm.getJvmErasure(candidateType).equals(KTypesJvm.getJvmErasure(parameters.get(i).getType()))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isUnderlyingTypeNullable(Class<?> valueClass) {
		KFunction<?> constructor = Objects.requireNonNull(
				KClasses.getPrimaryConstructor(JvmClassMappingKt.getKotlinClass(valueClass)));
		return isNullable(constructor.getParameters().get(0).getType());
	}

	private static boolean isNullable(KType type) {
		if (type.isMarkedNullable()) {
			return true;
		}
		if (type.getClassifier() instanceof KTypeParameter typeParameter) {
			return typeParameter.getUpperBounds().stream().anyMatch(CoroutinesUtils::isNullable);
		}
		return (type.getClassifier() instanceof KClass<?> kClass &&
				KotlinDetector.isInlineClass(JvmClassMappingKt.getJavaClass(kClass)) &&
				isUnderlyingTypeNullable(JvmClassMappingKt.getJavaClass(kClass)));
	}

	/**
	 * For the {@link #NO_UNBOX_METHOD} constant.
	 */
	@SuppressWarnings("unused")
	private static void noUnboxMethod() {
	}

}
