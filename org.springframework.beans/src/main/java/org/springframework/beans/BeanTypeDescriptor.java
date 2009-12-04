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

package org.springframework.beans;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ReflectionUtils;

/**
 * {@link TypeDescriptor} extension that exposes additional annotations
 * as conversion metadata: namely, annotations on other accessor methods
 * (getter/setter) and on the underlying field, if found.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
class BeanTypeDescriptor extends TypeDescriptor {

	private final PropertyDescriptor propertyDescriptor;

	private Annotation[] cachedAnnotations;


	/**
	 * Create a new BeanTypeDescriptor for the given bean property.
	 * @param propertyDescriptor the corresponding JavaBean PropertyDescriptor
	 */
	public BeanTypeDescriptor(PropertyDescriptor propertyDescriptor) {
		super(BeanUtils.getWriteMethodParameter(propertyDescriptor));
		this.propertyDescriptor = propertyDescriptor;
	}

	/**
	 * Create a new BeanTypeDescriptor for the given bean property.
	 * @param propertyDescriptor the corresponding JavaBean PropertyDescriptor
	 * @param methodParameter the target method parameter
	 * @param type the specific type to expose (may be an array/collection element)
	 */
	public BeanTypeDescriptor(PropertyDescriptor propertyDescriptor, MethodParameter methodParameter, Class type) {
		super(methodParameter, type);
		this.propertyDescriptor = propertyDescriptor;
	}


	/**
	 * Return the underlying PropertyDescriptor.
	 */
	public PropertyDescriptor getPropertyDescriptor() {
		return this.propertyDescriptor;
	}

	@Override
	public Annotation[] getAnnotations() {
		Annotation[] anns = this.cachedAnnotations;
		if (anns == null) {
			Field underlyingField = ReflectionUtils.findField(
					getMethodParameter().getMethod().getDeclaringClass(), this.propertyDescriptor.getName());
			Map<Class, Annotation> annMap = new LinkedHashMap<Class, Annotation>();
			if (underlyingField != null) {
				for (Annotation ann : underlyingField.getAnnotations()) {
					annMap.put(ann.annotationType(), ann);
				}
			}
			Method targetMethod = getMethodParameter().getMethod();
			Method writeMethod = this.propertyDescriptor.getWriteMethod();
			Method readMethod = this.propertyDescriptor.getReadMethod();
			if (writeMethod != null && writeMethod != targetMethod) {
				for (Annotation ann : writeMethod.getAnnotations()) {
					annMap.put(ann.annotationType(), ann);
				}
			}
			if (readMethod != null && readMethod != targetMethod) {
				for (Annotation ann : readMethod.getAnnotations()) {
					annMap.put(ann.annotationType(), ann);
				}
			}
			for (Annotation ann : targetMethod.getAnnotations()) {
				annMap.put(ann.annotationType(), ann);
			}
			anns = annMap.values().toArray(new Annotation[annMap.size()]);
			this.cachedAnnotations = anns;
		}
		return anns;
	}

}
