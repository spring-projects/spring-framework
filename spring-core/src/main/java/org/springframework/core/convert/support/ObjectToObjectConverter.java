/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.convert.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Generic converter that uses conventions to convert a source object to a
 * {@code targetType} by delegating to a method on the source object or to
 * a static factory method or constructor on the {@code targetType}.
 *
 * <h3>Conversion Algorithm</h3>
 * <ol>
 * <li>Invoke a {@code to[targetType.simpleName]()} method on the source object
 * that has a return type equal to {@code targetType}, if such a method exists.
 * For example, {@code org.example.Bar Foo#toBar()} is a method that follows this
 * convention.
 * <li>Otherwise invoke a <em>static</em> {@code valueOf(sourceType)} or Java
 * 8 style <em>static</em> {@code of(sourceType)} or {@code from(sourceType)}
 * method on the {@code targetType}, if such a method exists.
 * <li>Otherwise invoke a constructor on the {@code targetType} that accepts
 * a single {@code sourceType} argument, if such a constructor exists.
 * <li>Otherwise throw a {@link ConversionFailedException}.
 * </ol>
 *
 * <p><strong>Warning</strong>: this converter does <em>not</em> support the
 * {@link Object#toString()} method for converting from a {@code sourceType}
 * to {@code java.lang.String}. For {@code toString()} support, use
 * {@link FallbackObjectToStringConverter} instead.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see FallbackObjectToStringConverter
 */
final class ObjectToObjectConverter implements ConditionalGenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.getType().equals(targetType.getType())) {
			// no conversion required
			return false;
		}
		return (String.class == targetType.getType() ?
				hasFactoryConstructor(String.class, sourceType.getType()) :
				hasToMethodOrFactoryMethodOrConstructor(targetType.getType(), sourceType.getType()));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		Class<?> sourceClass = sourceType.getType();
		Class<?> targetClass = targetType.getType();
		try {
			// Do not invoke a toString() method
			if (String.class != targetClass) {
				Method method = getToMethod(targetClass, sourceClass);
				if (method != null) {
					ReflectionUtils.makeAccessible(method);
					return method.invoke(source);
				}
			}

			Method method = getFactoryMethod(targetClass, sourceClass);
			if (method != null) {
				ReflectionUtils.makeAccessible(method);
				return method.invoke(null, source);
			}

			Constructor<?> constructor = getFactoryConstructor(targetClass, sourceClass);
			if (constructor != null) {
				return constructor.newInstance(source);
			}
		}
		catch (InvocationTargetException ex) {
			throw new ConversionFailedException(sourceType, targetType, source, ex.getTargetException());
		}
		catch (Throwable ex) {
			throw new ConversionFailedException(sourceType, targetType, source, ex);
		}

		// If sourceClass is Number and targetClass is Integer, then the following message
		// format should expand to:
		// No toInteger() method exists on java.lang.Number, and no static
		// valueOf/of/from(java.lang.Number) method or Integer(java.lang.Number)
		// constructor exists on java.lang.Integer.
		String message = String.format(
			"No to%3$s() method exists on %1$s, and no static valueOf/of/from(%1$s) method or %3$s(%1$s) constructor exists on %2$s.",
			sourceClass.getName(), targetClass.getName(), targetClass.getSimpleName());

		throw new IllegalStateException(message);
	}


	private static Method getToMethod(Class<?> targetClass, Class<?> sourceClass) {
		Method method = ClassUtils.getMethodIfAvailable(sourceClass, "to" + targetClass.getSimpleName());
		return (method != null && targetClass.equals(method.getReturnType()) ? method : null);
	}

	private static Method getFactoryMethod(Class<?> targetClass, Class<?> sourceClass) {
		Method method = ClassUtils.getStaticMethod(targetClass, "valueOf", sourceClass);
		if (method == null) {
			method = ClassUtils.getStaticMethod(targetClass, "of", sourceClass);
			if (method == null) {
				method = ClassUtils.getStaticMethod(targetClass, "from", sourceClass);
			}
		}
		return method;
	}

	private static Constructor<?> getFactoryConstructor(Class<?> targetClass, Class<?> sourceClass) {
		return ClassUtils.getConstructorIfAvailable(targetClass, sourceClass);
	}

	private static boolean hasToMethodOrFactoryMethodOrConstructor(Class<?> targetClass,
			Class<?> sourceClass) {
		return (hasToMethod(targetClass, sourceClass) ||
				hasFactoryMethod(targetClass, sourceClass) ||
				hasFactoryConstructor(targetClass, sourceClass));
	}

	static boolean hasToMethod(Class<?> targetClass, Class<?> sourceClass) {
		return getToMethod(targetClass, sourceClass) != null;
	}

	static boolean hasFactoryMethod(Class<?> targetClass, Class<?> sourceClass) {
		return getFactoryMethod(targetClass, sourceClass) != null;
	}

	static boolean hasFactoryConstructor(Class<?> targetClass, Class<?> sourceClass) {
		return getFactoryConstructor(targetClass, sourceClass) != null;
	}

}
