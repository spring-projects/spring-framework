/*
 * Copyright 2002-2013 the original author or authors.
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
 * Generic converter that attempts to convert a source Object to a target type
 * by delegating to methods on the target type.
 *
 * <p>Calls a static {@code valueOf(sourceType)} or Java 8 style {@code of|from(sourceType)}
 * method on the target type to perform the conversion, if such a method exists. Otherwise,
 * it checks for a {@code to[targetType.simpleName]} method on the source type calls
 * the target type's constructor that accepts a single {@code sourceType} argument, if such
 * a constructor exists. If neither strategy works, it throws a ConversionFailedException.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
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
		return (String.class.equals(targetType.getType()) ?
				(ClassUtils.getConstructorIfAvailable(String.class, sourceType.getType()) != null) :
				hasToMethodOrOfMethodOrConstructor(targetType.getType(), sourceType.getType()));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		Class<?> sourceClass = sourceType.getType();
		Class<?> targetClass = targetType.getType();
		try {
			if (!String.class.equals(targetClass)) {
				Method method = getToMethod(targetClass, sourceClass);
				if (method != null) {
					ReflectionUtils.makeAccessible(method);
					return method.invoke(source);
				}
				method = getOfMethod(targetClass, sourceClass);
				if (method != null) {
					ReflectionUtils.makeAccessible(method);
					return method.invoke(null, source);
				}
			}
			Constructor<?> constructor = ClassUtils.getConstructorIfAvailable(targetClass, sourceClass);
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
		throw new IllegalStateException("No static valueOf/of/from(" + sourceClass.getName() +
				") method or Constructor(" + sourceClass.getName() + ") exists on " + targetClass.getName());
	}


	private static boolean hasToMethodOrOfMethodOrConstructor(Class<?> targetClass, Class<?> sourceClass) {
		return (getToMethod(targetClass, sourceClass) != null ||
				getOfMethod(targetClass, sourceClass) != null ||
				ClassUtils.getConstructorIfAvailable(targetClass, sourceClass) != null);
	}

	private static Method getToMethod(Class<?> targetClass, Class<?> sourceClass) {
		Method method = ClassUtils.getMethodIfAvailable(sourceClass, "to" + targetClass.getSimpleName());
		return (method != null && targetClass.equals(method.getReturnType()) ? method : null);
	}

	static Method getOfMethod(Class<?> targetClass, Class<?> sourceClass) {
		Method method = ClassUtils.getStaticMethod(targetClass, "valueOf", sourceClass);
		if (method == null) {
			method = ClassUtils.getStaticMethod(targetClass, "of", sourceClass);
			if (method == null) {
				method = ClassUtils.getStaticMethod(targetClass, "from", sourceClass);
			}
		}
		return method;
	}

}
