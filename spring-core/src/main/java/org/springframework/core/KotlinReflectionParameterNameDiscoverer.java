/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.jspecify.annotations.Nullable;

/**
 * {@link ParameterNameDiscoverer} implementation which uses Kotlin's reflection
 * facilities for introspecting parameter names.
 *
 * <p>In contrast to {@link StandardReflectionParameterNameDiscoverer}, this
 * discoverer can also determine interface parameter names without requiring Java's
 * {@code -parameters} compiler flag.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see StandardReflectionParameterNameDiscoverer
 * @see DefaultParameterNameDiscoverer
 */
public class KotlinReflectionParameterNameDiscoverer implements ParameterNameDiscoverer {

	@Override
	public @Nullable String @Nullable [] getParameterNames(Method method) {
		if (KotlinDetector.isKotlinType(method.getDeclaringClass())) {
			if (!method.isSynthetic() && !KotlinDetector.isSuspendingFunction(method)) {
				String[] parameterNames = getStandardParameterNames(method.getParameters());
				if (parameterNames != null) {
					return parameterNames;
				}
			}
			try {
				KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
				return (function != null ? getParameterNames(function.getParameters()) : null);
			}
			catch (UnsupportedOperationException ignored) {
			}
		}
		return null;
	}

	@Override
	public @Nullable String @Nullable [] getParameterNames(Constructor<?> ctor) {
		Class<?> declaringClass = ctor.getDeclaringClass();
		if (!declaringClass.isEnum() && KotlinDetector.isKotlinType(declaringClass)) {
			if (!ctor.isSynthetic()) {
				String[] parameterNames = getStandardParameterNames(ctor.getParameters());
				if (parameterNames != null) {
					return parameterNames;
				}
			}
			try {
				KFunction<?> function = findKotlinConstructor(declaringClass, ctor);
				if (function != null) {
					return getParameterNames(function.getParameters());
				}
			}
			catch (UnsupportedOperationException ignored) {
			}
		}
		return null;
	}

	private static @Nullable KFunction<?> findKotlinConstructor(Class<?> declaringClass, Constructor<?> ctor) {
		if (!ctor.isSynthetic() && !KotlinDetector.isInlineClass(declaringClass) &&
				declaringClass.getDeclaredConstructors().length == 1) {
			Collection<? extends KFunction<?>> constructors =
					JvmClassMappingKt.getKotlinClass(declaringClass).getConstructors();
			if (constructors.size() == 1) {
				return constructors.iterator().next();
			}
		}
		return ReflectJvmMapping.getKotlinFunction(ctor);
	}

	private static String @Nullable [] getStandardParameterNames(Parameter[] parameters) {
		String[] parameterNames = new String[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];
			if (!param.isNamePresent() || param.isImplicit() || param.isSynthetic()) {
				return null;
			}
			String name = param.getName();
			if (name.startsWith("$")) {
				return null;
			}
			parameterNames[i] = name;
		}
		return parameterNames;
	}

	private @Nullable String @Nullable [] getParameterNames(List<KParameter> parameters) {
		int size = parameters.size();
		String[] names = new String[size];
		int count = 0;
		for (int i = 0; i < size; i++) {
			KParameter param = parameters.get(i);
			KParameter.Kind kind = param.getKind();
			if (kind == KParameter.Kind.VALUE) {
				String name = param.getName();
				if (name == null) {
					return null;
				}
				names[count++] = name;
			}
			else if (kind == KParameter.Kind.EXTENSION_RECEIVER) {
				names[count++] = "$receiver";
			}
		}
		return (count == size ? names : Arrays.copyOf(names, count));
	}

}
