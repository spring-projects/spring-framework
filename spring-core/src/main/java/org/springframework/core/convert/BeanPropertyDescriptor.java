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

import java.lang.annotation.Annotation;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;

/**
 * @author Keith Donald
 * @since 3.1
 */
class BeanPropertyDescriptor extends AbstractDescriptor {

	private final Property property;

	private final MethodParameter methodParameter;

	private final Annotation[] annotations;


	public BeanPropertyDescriptor(Property property) {
		super(property.getType());
		this.property = property;
		this.methodParameter = property.getMethodParameter();
		this.annotations = property.getAnnotations();
	}


	@Override
	public Annotation[] getAnnotations() {
		return this.annotations;
	}

	@Override
	protected Class<?> resolveCollectionElementType() {
		return GenericCollectionTypeResolver.getCollectionParameterType(this.methodParameter);
	}

	@Override
	protected Class<?> resolveMapKeyType() {
		return GenericCollectionTypeResolver.getMapKeyParameterType(this.methodParameter);
	}

	@Override
	protected Class<?> resolveMapValueType() {
		return GenericCollectionTypeResolver.getMapValueParameterType(this.methodParameter);
	}

	@Override
	protected AbstractDescriptor nested(Class<?> type, int typeIndex) {
		MethodParameter methodParameter = new MethodParameter(this.methodParameter);
		methodParameter.increaseNestingLevel();
		methodParameter.setTypeIndexForCurrentLevel(typeIndex);
		return new BeanPropertyDescriptor(type, this.property, methodParameter, this.annotations);
	}


	// internal

	private BeanPropertyDescriptor(Class<?> type, Property propertyDescriptor, MethodParameter methodParameter, Annotation[] annotations) {
		super(type);
		this.property = propertyDescriptor;
		this.methodParameter = methodParameter;
		this.annotations = annotations;
	}

}
