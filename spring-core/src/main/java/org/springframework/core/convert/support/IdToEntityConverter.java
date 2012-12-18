/*
 * Copyright 2002-2009 the original author or authors.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Converts an entity identifier to a entity reference by calling a static finder method
 * on the target entity type.
 *
 * <p>For this converter to match, the finder method must be public, static, have the signature
 * {@code find[EntityName]([IdType])}, and return an instance of the desired entity type.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class IdToEntityConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;

	public IdToEntityConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Method finder = getFinder(targetType.getType());
		return finder != null && this.conversionService.canConvert(sourceType, TypeDescriptor.valueOf(finder.getParameterTypes()[0]));
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		Method finder = getFinder(targetType.getType());
		Object id = this.conversionService.convert(source, sourceType, TypeDescriptor.valueOf(finder.getParameterTypes()[0]));
		return ReflectionUtils.invokeMethod(finder, source, id);
	}

	private Method getFinder(Class<?> entityClass) {
		String finderMethod = "find" + getEntityName(entityClass);
		Method[] methods = entityClass.getDeclaredMethods();
		for (Method method : methods) {
			if (Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == 1 && method.getReturnType().equals(entityClass)) {
				if (method.getName().equals(finderMethod)) {
					return method;
				}
			}
		}
		return null;
	}

	private String getEntityName(Class<?> entityClass) {
		String shortName = ClassUtils.getShortName(entityClass);
		int lastDot = shortName.lastIndexOf('.');
		if (lastDot != -1) {
			return shortName.substring(lastDot + 1);
		}
		else {
			return shortName;
		}
	}

}
