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

package org.springframework.util;

import kotlin.Metadata;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Miscellaneous Kotlin utility methods.
 *
 * @author Raman Gupta
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class KotlinUtils {

	private static final boolean kotlinPresent = ClassUtils.isPresent("kotlin.Unit", KotlinUtils.class.getClassLoader());

	/**
	 * Return whether Kotlin is available on the classpath or not.
	 */
	public static boolean isKotlinPresent() {
		return kotlinPresent;
	}

	/**
	 * Return whether the specified type is a Kotlin class or not.
	 */
	public static boolean isKotlinClass(Class<?> type) {
		Assert.notNull(type, "Type must not be null");
		return isKotlinPresent() && type.getDeclaredAnnotation(Metadata.class) != null;
	}

	/**
	 * Check whether the specified {@link MethodParameter} represents a nullable Kotlin type or not.
	 */
	public static boolean isNullable(MethodParameter methodParameter) {
		Method method = methodParameter.getMethod();
		int parameterIndex = methodParameter.getParameterIndex();
		if (isKotlinClass(methodParameter.getContainingClass())) {
			if (parameterIndex < 0) {
				KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
				return function != null && function.getReturnType().isMarkedNullable();
			}
			else {
				KFunction<?> function = (method != null ? ReflectJvmMapping.getKotlinFunction(method) :
					ReflectJvmMapping.getKotlinFunction(methodParameter.getConstructor()));
				if (function != null) {
					List<KParameter> parameters = function.getParameters();
					return parameters
							.stream()
							.filter(p -> KParameter.Kind.VALUE.equals(p.getKind()))
							.collect(Collectors.toList())
							.get(parameterIndex)
							.getType()
							.isMarkedNullable();
				}
			}
		}
		return false;
	}

}
