/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.example;


import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideStrategy;
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.util.StringUtils;

import static org.springframework.test.context.bean.override.example.ExampleBeanOverrideAnnotation.DEFAULT_VALUE;

class TestOverrideMetadata extends OverrideMetadata {

	@Nullable
	private final Method method;

	@Nullable
	private final String beanName;

	private final String methodName;

	@Nullable
	private static Method findMethod(AnnotatedElement element, String methodName) {
		if (DEFAULT_VALUE.equals(methodName)) {
			return null;
		}
		if (element instanceof Field f) {
			for (Method m : f.getDeclaringClass().getDeclaredMethods()) {
				if (!Modifier.isStatic(m.getModifiers())) {
					continue;
				}
				if (m.getName().equals(methodName)) {
					return m;
				}
			}
			throw new IllegalStateException("Expected a static method named <" + methodName + "> alongside annotated field <" + f.getName() + ">");
		}
		if (element instanceof Method m) {
			if (m.getName().equals(methodName) && Modifier.isStatic(m.getModifiers())) {
				return m;
			}
			throw new IllegalStateException("Expected the annotated method to be static and named <" + methodName + ">");
		}
		if (element instanceof Class c) {
			for (Method m : c.getDeclaredMethods()) {
				if (!Modifier.isStatic(m.getModifiers())) {
					continue;
				}
				if (m.getName().equals(methodName)) {
					return m;
				}
			}
			throw new IllegalStateException("Expected a static method named <" + methodName + "> on annotated class <" + c.getSimpleName() + ">");
		}
		throw new IllegalStateException("Expected the annotated element to be a Field, Method or Class");
	}

	public TestOverrideMetadata(Field field, ExampleBeanOverrideAnnotation overrideAnnotation, ResolvableType typeToOverride) {
		super(field, typeToOverride, overrideAnnotation.createIfMissing() ?
				BeanOverrideStrategy.REPLACE_OR_CREATE_DEFINITION: BeanOverrideStrategy.REPLACE_DEFINITION);
		this.method = findMethod(field, overrideAnnotation.value());
		this.methodName = overrideAnnotation.value();
		this.beanName = overrideAnnotation.beanName();
	}

	//Used to trigger duplicate detection in parser test
	TestOverrideMetadata(String duplicateTrigger) {
		super(null, null, null);
		this.method = null;
		this.methodName = duplicateTrigger;
		this.beanName = duplicateTrigger;
	}

	@Override
	protected String getBeanName() {
		if (StringUtils.hasText(this.beanName)) {
			return this.beanName;
		}
		return getField().getName();
	}

	String getAnnotationMethodName() {
		return this.methodName;
	}

	@Override
	protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition, @Nullable Object existingBeanInstance) {
		if (this.method == null) {
			return DEFAULT_VALUE;
		}
		try {
			this.method.setAccessible(true);
			return this.method.invoke(null);
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this.method == null) {
			return obj instanceof TestOverrideMetadata tem &&
					tem.beanName != null &&
					tem.beanName.startsWith(ExampleBeanOverrideAnnotation.DUPLICATE_TRIGGER);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		if (this.method == null) {
			return ExampleBeanOverrideAnnotation.DUPLICATE_TRIGGER.hashCode();
		}
		return super.hashCode();
	}

	@Override
	public String toString() {
		if (this.method == null) {
			return "{" + this.beanName + "}";
		}
		return this.methodName;
	}
}
