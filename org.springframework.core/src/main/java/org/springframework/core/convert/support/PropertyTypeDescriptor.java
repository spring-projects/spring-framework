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
 * @since 3.0.2
 */
public class PropertyTypeDescriptor extends TypeDescriptor {

	private final PropertyDescriptor propertyDescriptor;

	private Annotation[] cachedAnnotations;


	/**
	 * Create a new BeanTypeDescriptor for the given bean property.
	 * @param propertyDescriptor the corresponding JavaBean PropertyDescriptor
	 * @param methodParameter the target method parameter
	 */
	public PropertyTypeDescriptor(PropertyDescriptor propertyDescriptor, MethodParameter methodParameter) {
		super(methodParameter);
		this.propertyDescriptor = propertyDescriptor;
	}

	/**
	 * Create a new BeanTypeDescriptor for the given bean property.
	 * @param propertyDescriptor the corresponding JavaBean PropertyDescriptor
	 * @param methodParameter the target method parameter
	 * @param type the specific type to expose (may be an array/collection element)
	 */
	public PropertyTypeDescriptor(PropertyDescriptor propertyDescriptor, MethodParameter methodParameter, Class<?> type) {
		super(methodParameter, type);
		this.propertyDescriptor = propertyDescriptor;
	}


	/**
	 * Return the underlying PropertyDescriptor.
	 */
	public PropertyDescriptor getPropertyDescriptor() {
		return this.propertyDescriptor;
	}

	public Annotation[] getAnnotations() {
		Annotation[] anns = this.cachedAnnotations;
		if (anns == null) {
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
			anns = annMap.values().toArray(new Annotation[annMap.size()]);
			this.cachedAnnotations = anns;
		}
		return anns;
	}

	@Override
	public TypeDescriptor forElementType(Class<?> elementType) {
		if (elementType != null) {
			MethodParameter nested = new MethodParameter(getMethodParameter());
			nested.increaseNestingLevel();
			return new PropertyTypeDescriptor(this.propertyDescriptor, nested, elementType);
		}
		else {
			return super.forElementType(null);
		}
	}

}
