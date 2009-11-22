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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Converts an entity identifier to a entity reference by calling a static finder method on the target entity type.
 * Also converts a entity reference to a String by printing its id property to String.
 * For id-to-entity conversion to match, the finder method must be public, static, have the signature 'find[EntityName]([IdType])', and return an instance of the desired entity type.
 * For entity-to-string conversion to match, a getter method for the 'id' property must be defined.
 * @author Keith Donald
 * @since 3.0
 */
final class EntityConverter implements ConditionalGenericConverter {

	private GenericConversionService conversionService;

	public EntityConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}
	
	public Class<?>[][] getConvertibleTypes() {
		return new Class[][] { 
				{ Object.class, Object.class },
				{ Object.class, String.class }
		};
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (String.class.equals(targetType.getType())) {
			return getIdAccessor(sourceType.getType()) != null;
		} else {
			return getFinder(targetType.getType()) != null;
		}
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}
		if (String.class.equals(targetType.getType())) {
			Method idAccessor = getIdAccessor(sourceType.getType());
			Object id = ReflectionUtils.invokeMethod(idAccessor, source);
			return this.conversionService.convert(id, String.class);
		} else {
			Method finder = getFinder(targetType.getType());			
			Object id = conversionService.convert(source, sourceType, TypeDescriptor.valueOf(finder.getParameterTypes()[0]));
			return ReflectionUtils.invokeMethod(finder, source, id);			
		}
	}

	private Method getIdAccessor(Class<?> entityClass) {
		return ClassUtils.getMethodIfAvailable(entityClass, "getId");
	}
	
	private Method getFinder(Class<?> entityClass) {
		String finderMethod = "find" + getEntityName(entityClass);
		Method[] methods = entityClass.getDeclaredMethods();
		for (Method method : methods) {
			if (Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == 1) {
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
		} else {
			return shortName;
		}
	}

}
