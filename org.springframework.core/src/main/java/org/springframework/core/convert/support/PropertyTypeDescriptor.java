/*
 * Copyright 2002-2010 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link TypeDescriptor} extension that exposes additional annotations
 * as conversion metadata: namely, annotations on other accessor methods
 * (getter/setter) and on the underlying field, if found.
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @since 3.0.2
 */
public class PropertyTypeDescriptor extends TypeDescriptor {

	private final PropertyDescriptor propertyDescriptor;

	/**
	 * Create a new type descriptor for the given bean property.
	 * @param methodParameter the target method parameter
	 * @param propertyDescriptor the corresponding JavaBean PropertyDescriptor
	 */
	public PropertyTypeDescriptor(MethodParameter methodParameter, PropertyDescriptor propertyDescriptor) {
		super(methodParameter);
		this.propertyDescriptor = propertyDescriptor;
	}

	/**
	 * Create a new type descriptor for a nested type declared on an array, collection, or map-based property.
	 * Use this factory method when you've resolved a nested source object such as a collection element or map value and wish to have it converted.
	 * Builds in protection for increasing the nesting level of the provided MethodParameter if the nestedType is itself a collection.
	 * @param methodParameter the method parameter
	 * @return the property descriptor
	 */
	public static PropertyTypeDescriptor forNestedType(MethodParameter methodParameter, PropertyDescriptor propertyDescriptor) {
		return forNestedType(resolveNestedType(methodParameter), methodParameter, propertyDescriptor);
	}

	/**
	 * Create a new type descriptor for a nested type declared on an array, collection, or map-based property.
	 * Use this factory method when you've resolved a nested source object such as a collection element or map value and wish to have it converted.
	 * Builds in protection for increasing the nesting level of the provided MethodParameter if the nestedType is itself a collection.
	 * @param nestedType the nested type
	 * @param methodParameter the method parameter
	 * @return the property descriptor
	 */
	public static PropertyTypeDescriptor forNestedType(Class<?> nestedType, MethodParameter methodParameter, PropertyDescriptor propertyDescriptor) {
		return new PropertyTypeDescriptor(nestedType, methodParameter, propertyDescriptor);
	}
	
	/**
	 * Return the underlying PropertyDescriptor.
	 */
	public PropertyDescriptor getPropertyDescriptor() {
		return this.propertyDescriptor;
	}

	protected Annotation[] resolveAnnotations() {
		Map<Class<?>, Annotation> annMap = new LinkedHashMap<Class<?>, Annotation>();
		String name = this.propertyDescriptor.getName();
		if (StringUtils.hasLength(name)) {
			Class<?> clazz = getMethodParameter().getMethod().getDeclaringClass();
			Field field = ReflectionUtils.findField(clazz, name);
			if (field == null) {
				// Same lenient fallback checking as in CachedIntrospectionResults...
				field = ReflectionUtils.findField(clazz, name.substring(0, 1).toLowerCase() + name.substring(1));
				if (field == null) {
					field = ReflectionUtils.findField(clazz, name.substring(0, 1).toUpperCase() + name.substring(1));
				}
			}
			if (field != null) {
				for (Annotation ann : field.getAnnotations()) {
					annMap.put(ann.annotationType(), ann);
				}
			}
		}
		Method writeMethod = this.propertyDescriptor.getWriteMethod();
		Method readMethod = this.propertyDescriptor.getReadMethod();
		if (writeMethod != null && writeMethod != getMethodParameter().getMethod()) {
			for (Annotation ann : writeMethod.getAnnotations()) {
				annMap.put(ann.annotationType(), ann);
			}
		}
		if (readMethod != null && readMethod != getMethodParameter().getMethod()) {
			for (Annotation ann : readMethod.getAnnotations()) {
				annMap.put(ann.annotationType(), ann);
			}
		}
		for (Annotation ann : getMethodParameter().getMethodAnnotations()) {
			annMap.put(ann.annotationType(), ann);
		}
		for (Annotation ann : getMethodParameter().getParameterAnnotations()) {
			annMap.put(ann.annotationType(), ann);
		}
		return annMap.values().toArray(new Annotation[annMap.size()]);
	}

	protected TypeDescriptor newNestedTypeDescriptor(Class<?> nestedType, MethodParameter nested) {
		return new PropertyTypeDescriptor(nestedType, nested, this.propertyDescriptor);
	}

	// internal constructors
	
	private PropertyTypeDescriptor(Class<?> nestedType, MethodParameter methodParameter, PropertyDescriptor propertyDescriptor) {
		super(nestedType, methodParameter);
		this.propertyDescriptor = propertyDescriptor;
	}

}