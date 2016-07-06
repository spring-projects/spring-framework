/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.reactive.result;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bouncycastle.util.Arrays;

import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Convenience class for use in tests to resolve a {@link Method} and/or any of
 * its {@link MethodParameter}s based on some hints.
 *
 * <p>In tests we often create a class (e.g. TestController) with diverse method
 * signatures and annotations to test with. Use of descriptive method and argument
 * names combined with using reflection, it becomes challenging to read and write
 * tests and it becomes necessary to navigate to the actual method declaration
 * which is cumbersome and involves several steps.
 *
 * <p>The idea here is to provide enough hints to resolving a method uniquely
 * where the hints document exactly what is being tested and there is usually no
 * need to navigate to the actual method declaration. For example if testing
 * response handling, the return type may be used as a hint:
 *
 * <pre>
 * ResolvableMethod resolvableMethod = ResolvableMethod.on(TestController.class);

 * ResolvableType type = ResolvableType.forClassWithGenerics(Mono.class, View.class);
 * Method method = resolvableMethod.returning(type).resolve();
 *
 * type = ResolvableType.forClassWithGenerics(Mono.class, String.class);
 * method = resolvableMethod.returning(type).resolve();
 *
 * // ...
 * </pre>
 *
 * <p>Additional {@code resolve} methods provide options to obtain one of the method
 * arguments or return type as a {@link MethodParameter}.
 *
 * @author Rossen Stoyanchev
 */
public class ResolvableMethod {

	private final Class<?> targetClass;

	private String methodName;

	private Class<?>[] argumentTypes;

	private ResolvableType returnType;

	private final List<Class<? extends Annotation>> annotationTypes = new ArrayList<>(4);


	private ResolvableMethod(Class<?> targetClass) {
		this.targetClass = targetClass;
	}


	public ResolvableMethod name(String methodName) {
		this.methodName = methodName;
		return this;
	}

	public ResolvableMethod argumentTypes(Class<?>... argumentTypes) {
		this.argumentTypes = argumentTypes;
		return this;
	}

	public ResolvableMethod returning(ResolvableType resolvableType) {
		this.returnType = resolvableType;
		return this;
	}

	public ResolvableMethod annotated(Class<? extends Annotation> annotationType) {
		this.annotationTypes.add(annotationType);
		return this;
	}


	public Method resolve() {
		// String comparison (ResolvableType's with different providers)
		String expectedReturnType = getReturnType();

		Set<Method> methods = MethodIntrospector.selectMethods(this.targetClass,
				(ReflectionUtils.MethodFilter) method -> {
					if (this.methodName != null && !this.methodName.equals(method.getName())) {
						return false;
					}
					if (getReturnType() != null) {
						String actual = ResolvableType.forMethodReturnType(method).toString();
						if (!actual.equals(getReturnType()) && !Object.class.equals(method.getDeclaringClass())) {
							return false;
						}
					}
					if (!ObjectUtils.isEmpty(this.argumentTypes)) {
						if (!Arrays.areEqual(this.argumentTypes, method.getParameterTypes())) {
							return false;
						}
					}
					for (Class<? extends Annotation> annotationType : this.annotationTypes) {
						if (AnnotationUtils.findAnnotation(method, annotationType) == null) {
							return false;
						}
					}
					return true;
				});

		Assert.isTrue(!methods.isEmpty(), "No matching method: " + this);
		Assert.isTrue(methods.size() == 1, "Multiple matching methods: " + this);

		return methods.iterator().next();
	}

	private String getReturnType() {
		return this.returnType != null ? this.returnType.toString() : null;
	}

	public MethodParameter resolveReturnType() {
		Method method = resolve();
		return new MethodParameter(method, -1);
	}


	@Override
	public String toString() {
		return "Class=" + this.targetClass + ", name= " + this.methodName +
				", returnType=" + this.returnType + ", annotations=" + this.annotationTypes;
	}


	public static ResolvableMethod on(Class<?> clazz) {
		return new ResolvableMethod(clazz);
	}

}