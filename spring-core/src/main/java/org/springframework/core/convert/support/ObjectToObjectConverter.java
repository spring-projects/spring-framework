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

package org.springframework.core.convert.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * Generic converter that uses conventions to convert a source object to a
 * {@code targetType} by delegating to a method on the source object or to
 * a static factory method or constructor on the {@code targetType}.
 *
 * <h3>Conversion Algorithm</h3>
 * <ol>
 * <li>Invoke a non-static {@code to[targetType.simpleName]()} method on the
 * source object that has a return type assignable to {@code targetType}, if such
 * a method exists. For example, {@code org.example.Bar Foo#toBar()} is a
 * method that follows this convention.
 * <li>Otherwise invoke a <em>static</em> {@code valueOf(sourceType)} or Java
 * 8 style <em>static</em> {@code of(sourceType)} or {@code from(sourceType)}
 * method on the {@code targetType} that has a return type <em>related</em> to
 * {@code targetType}, if such a method exists. For example, a static
 * {@code Foo.of(sourceType)} method that returns a {@code Foo},
 * {@code SuperFooType}, or {@code SubFooType} is a method that follows this
 * convention. {@link java.time.ZoneId#of(String)} is a concrete example of
 * such a static factory method which returns a subtype of {@code ZoneId}.
 * <li>Otherwise invoke a constructor on the {@code targetType} that accepts
 * a single {@code sourceType} argument, if such a constructor exists.
 * <li>Otherwise throw a {@link ConversionFailedException} or
 * {@link IllegalStateException}.
 * </ol>
 *
 * <p><strong>Warning</strong>: this converter does <em>not</em> support the
 * {@link Object#toString()} or {@link String#valueOf(Object)} methods for converting
 * from a {@code sourceType} to {@code java.lang.String}. For {@code toString()}
 * support, use {@link FallbackObjectToStringConverter} instead.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see FallbackObjectToStringConverter
 */
final class ObjectToObjectConverter implements ConditionalGenericConverter {

	// Cache for the latest to-method, static factory method, or factory constructor
	// resolved on a given Class
	private static final Map<Class<?>, Executable> conversionExecutableCache =
			new ConcurrentReferenceHashMap<>(32);


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (sourceType.getType() != targetType.getType() &&
				hasConversionMethodOrConstructor(targetType.getType(), sourceType.getType()));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		Class<?> sourceClass = sourceType.getType();
		Class<?> targetClass = targetType.getType();
		Executable executable = getValidatedExecutable(targetClass, sourceClass);

		try {
			if (executable instanceof Method) {
				Method method = (Method) executable;
				ReflectionUtils.makeAccessible(method);
				if (!Modifier.isStatic(method.getModifiers())) {
					return method.invoke(source);
				}
				else {
					return method.invoke(null, source);
				}
			}
			else if (executable instanceof Constructor) {
				Constructor<?> ctor = (Constructor<?>) executable;
				ReflectionUtils.makeAccessible(ctor);
				return ctor.newInstance(source);
			}
		}
		catch (InvocationTargetException ex) {
			throw new ConversionFailedException(sourceType, targetType, source, ex.getTargetException());
		}
		catch (Throwable ex) {
			throw new ConversionFailedException(sourceType, targetType, source, ex);
		}

		// If sourceClass is Number and targetClass is Integer, the following message should expand to:
		// No toInteger() method exists on java.lang.Number, and no static valueOf/of/from(java.lang.Number)
		// method or Integer(java.lang.Number) constructor exists on java.lang.Integer.
		throw new IllegalStateException(String.format("No to%3$s() method exists on %1$s, " +
				"and no static valueOf/of/from(%1$s) method or %3$s(%1$s) constructor exists on %2$s.",
				sourceClass.getName(), targetClass.getName(), targetClass.getSimpleName()));
	}


	static boolean hasConversionMethodOrConstructor(Class<?> targetClass, Class<?> sourceClass) {
		return (getValidatedExecutable(targetClass, sourceClass) != null);
	}

	@Nullable
	private static Executable getValidatedExecutable(Class<?> targetClass, Class<?> sourceClass) {
		Executable executable = conversionExecutableCache.get(targetClass);
		if (isApplicable(executable, sourceClass)) {
			return executable;
		}

		executable = determineToMethod(targetClass, sourceClass);
		if (executable == null) {
			executable = determineFactoryMethod(targetClass, sourceClass);
			if (executable == null) {
				executable = determineFactoryConstructor(targetClass, sourceClass);
				if (executable == null) {
					return null;
				}
			}
		}

		conversionExecutableCache.put(targetClass, executable);
		return executable;
	}

	private static boolean isApplicable(Executable executable, Class<?> sourceClass) {
		if (executable instanceof Method) {
			Method method = (Method) executable;
			return (!Modifier.isStatic(method.getModifiers()) ?
					ClassUtils.isAssignable(method.getDeclaringClass(), sourceClass) :
					method.getParameterTypes()[0] == sourceClass);
		}
		else if (executable instanceof Constructor) {
			Constructor<?> ctor = (Constructor<?>) executable;
			return (ctor.getParameterTypes()[0] == sourceClass);
		}
		else {
			return false;
		}
	}

	@Nullable
	private static Method determineToMethod(Class<?> targetClass, Class<?> sourceClass) {
		if (String.class == targetClass || String.class == sourceClass) {
			// Do not accept a toString() method or any to methods on String itself
			return null;
		}

		Method method = ClassUtils.getMethodIfAvailable(sourceClass, "to" + targetClass.getSimpleName());
		return (method != null && !Modifier.isStatic(method.getModifiers()) &&
				ClassUtils.isAssignable(targetClass, method.getReturnType()) ? method : null);
	}

	@Nullable
	private static Method determineFactoryMethod(Class<?> targetClass, Class<?> sourceClass) {
		if (String.class == targetClass) {
			// Do not accept the String.valueOf(Object) method
			return null;
		}

		Method method = ClassUtils.getStaticMethod(targetClass, "valueOf", sourceClass);
		if (method == null) {
			method = ClassUtils.getStaticMethod(targetClass, "of", sourceClass);
			if (method == null) {
				method = ClassUtils.getStaticMethod(targetClass, "from", sourceClass);
			}
		}

		return (method != null && areRelatedTypes(targetClass, method.getReturnType()) ? method : null);
	}

	/**
	 * Determine if the two types reside in the same type hierarchy (i.e., type 1
	 * is assignable to type 2 or vice versa).
	 * @since 5.3.21
	 * @see ClassUtils#isAssignable(Class, Class)
	 */
	private static boolean areRelatedTypes(Class<?> type1, Class<?> type2) {
		return (ClassUtils.isAssignable(type1, type2) || ClassUtils.isAssignable(type2, type1));
	}

	@Nullable
	private static Constructor<?> determineFactoryConstructor(Class<?> targetClass, Class<?> sourceClass) {
		return ClassUtils.getConstructorIfAvailable(targetClass, sourceClass);
	}

}
