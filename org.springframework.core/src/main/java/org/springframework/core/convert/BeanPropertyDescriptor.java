/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.core.convert;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

class BeanPropertyDescriptor extends AbstractDescriptor {

	private final Class<?> beanClass;
	
	private final PropertyDescriptor property;

	private final MethodParameter methodParameter;
	
	private final Annotation[] annotations;
	
	public BeanPropertyDescriptor(Class<?> beanClass, PropertyDescriptor property) {
		super(property.getPropertyType());
		this.beanClass = beanClass;
		this.property = property;
		this.methodParameter = resolveMethodParameter();
		this.annotations = resolveAnnotations();
	}

	@Override
	public Annotation[] getAnnotations() {
		return annotations;
	}
	
	@Override
	protected Class<?> resolveCollectionElementType() {
		return GenericCollectionTypeResolver.getCollectionParameterType(methodParameter);
	}

	@Override
	protected Class<?> resolveMapKeyType() {
		return GenericCollectionTypeResolver.getMapKeyParameterType(methodParameter);
	}

	@Override
	protected Class<?> resolveMapValueType() {
		return GenericCollectionTypeResolver.getMapValueParameterType(methodParameter);
	}

	@Override
	protected AbstractDescriptor nested(Class<?> type, int typeIndex) {
		MethodParameter methodParameter = new MethodParameter(this.methodParameter);
		methodParameter.increaseNestingLevel();
		methodParameter.setTypeIndexForCurrentLevel(typeIndex);			
		return new BeanPropertyDescriptor(type, beanClass, property, methodParameter, annotations);
	}
	
	// internal

	private MethodParameter resolveMethodParameter() {
		MethodParameter parameter = parameterForPropertyMethod();
		// needed to resolve generic property types that parameterized by sub-classes e.g. T getFoo();
		GenericTypeResolver.resolveParameterType(parameter, beanClass);
		return parameter;
	}
	
	private MethodParameter parameterForPropertyMethod() {
		if (property.getReadMethod() != null) {
			return new MethodParameter(property.getReadMethod(), -1);
		} else if (property.getWriteMethod() != null) {
			return new MethodParameter(property.getWriteMethod(), 0);
		} else {
			throw new IllegalArgumentException("Property is neither readable or writeable");
		}
	}
	
	private Annotation[] resolveAnnotations() {
		Map<Class<?>, Annotation> annMap = new LinkedHashMap<Class<?>, Annotation>();
		Method readMethod = this.property.getReadMethod();
		if (readMethod != null) {
			for (Annotation ann : readMethod.getAnnotations()) {
				annMap.put(ann.annotationType(), ann);
			}
		}
		Method writeMethod = this.property.getWriteMethod();
		if (writeMethod != null) {
			for (Annotation ann : writeMethod.getAnnotations()) {
				annMap.put(ann.annotationType(), ann);
			}
		}			
		Field field = getField();
		if (field != null) {
			for (Annotation ann : field.getAnnotations()) {
				annMap.put(ann.annotationType(), ann);
			}
		}
		return annMap.values().toArray(new Annotation[annMap.size()]);			
	}
	
	private Field getField() {
		String name = this.property.getName();
		if (!StringUtils.hasLength(name)) {
			return null;
		}
		Class<?> declaringClass = declaringClass();
		Field field = ReflectionUtils.findField(declaringClass, name);
		if (field == null) {
			// Same lenient fallback checking as in CachedIntrospectionResults...
			field = ReflectionUtils.findField(declaringClass, name.substring(0, 1).toLowerCase() + name.substring(1));
			if (field == null) {
				field = ReflectionUtils.findField(declaringClass, name.substring(0, 1).toUpperCase() + name.substring(1));
			}
		}
		return field;
	}
	
	private Class<?> declaringClass() {
		if (this.property.getReadMethod() != null) {
			return this.property.getReadMethod().getDeclaringClass();
		} else {
			return this.property.getWriteMethod().getDeclaringClass();
		}
	}
	
	private BeanPropertyDescriptor(Class<?> type, Class<?> beanClass, java.beans.PropertyDescriptor propertyDescriptor, MethodParameter methodParameter, Annotation[] annotations) {
		super(type);
		this.beanClass = beanClass;
		this.property = propertyDescriptor;
		this.methodParameter = methodParameter;
		this.annotations = annotations;
	}
	
}