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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;

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
 * ResolvableMethod resolvableMethod = ResolvableMethod.onClass(TestController.class);

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

	private final Class<?> objectClass;

	private final Object object;


	private String methodName;

	private Class<?>[] argumentTypes;

	private ResolvableType returnType;

	private final List<Class<? extends Annotation>> annotationTypes = new ArrayList<>(4);

	private final List<Predicate<Method>> predicates = new ArrayList<>(4);



	private ResolvableMethod(Class<?> objectClass) {
		Assert.notNull(objectClass);
		this.objectClass = objectClass;
		this.object = null;
	}

	private ResolvableMethod(Object object) {
		Assert.notNull(object);
		this.object = object;
		this.objectClass = object.getClass();
	}


	/**
	 * Methods that match the given name (regardless of arguments).
	 */
	public ResolvableMethod name(String methodName) {
		this.methodName = methodName;
		return this;
	}

	/**
	 * Methods that match the given argument types.
	 */
	public ResolvableMethod argumentTypes(Class<?>... argumentTypes) {
		this.argumentTypes = argumentTypes;
		return this;
	}

	/**
	 * Methods declared to return the given type.
	 */
	public ResolvableMethod returning(ResolvableType resolvableType) {
		this.returnType = resolvableType;
		return this;
	}

	/**
	 * Methods with the given annotation.
	 */
	public ResolvableMethod annotated(Class<? extends Annotation> annotationType) {
		this.annotationTypes.add(annotationType);
		return this;
	}

	/**
	 * Methods matching the given predicate.
	 */
	public final ResolvableMethod matching(Predicate<Method> methodPredicate) {
		this.predicates.add(methodPredicate);
		return this;
	}

	// Resolve methods

	public Method resolve() {
		Set<Method> methods = MethodIntrospector.selectMethods(this.objectClass,
				(ReflectionUtils.MethodFilter) method -> {
					if (this.methodName != null && !this.methodName.equals(method.getName())) {
						return false;
					}
					if (getReturnType() != null) {
						// String comparison (ResolvableType's with different providers)
						String actual = ResolvableType.forMethodReturnType(method).toString();
						if (!actual.equals(getReturnType()) && !Object.class.equals(method.getDeclaringClass())) {
							return false;
						}
					}
					else if (!ObjectUtils.isEmpty(this.argumentTypes)) {
						if (!Arrays.equals(this.argumentTypes, method.getParameterTypes())) {
							return false;
						}
					}
					else if (this.annotationTypes.stream()
							.filter(annotType -> AnnotationUtils.findAnnotation(method, annotType) == null)
							.findFirst()
							.isPresent()) {
						return false;
					}
					else if (this.predicates.stream().filter(p -> !p.test(method)).findFirst().isPresent()) {
						return false;
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

	public InvocableHandlerMethod resolveHandlerMethod() {
		Assert.notNull(this.object);
		return new InvocableHandlerMethod(this.object, resolve());
	}

	public MethodParameter resolveReturnType() {
		Method method = resolve();
		return new MethodParameter(method, -1);
	}

	@SafeVarargs
	public final MethodParameter resolveParam(Predicate<MethodParameter>... predicates) {
		return resolveParam(null, predicates);
	}

	@SafeVarargs
	public final MethodParameter resolveParam(ResolvableType type,
			Predicate<MethodParameter>... predicates) {

		List<MethodParameter> matches = new ArrayList<>();

		Method method = resolve();
		for (int i = 0; i < method.getParameterCount(); i++) {
			MethodParameter param = new MethodParameter(method, i);
			if (type != null) {
				if (!ResolvableType.forMethodParameter(param).toString().equals(type.toString())) {
					continue;
				}
			}
			if (!ObjectUtils.isEmpty(predicates)) {
				if (Arrays.stream(predicates).filter(p -> !p.test(param)).findFirst().isPresent()) {
					continue;
				}
			}
			matches.add(param);
		}

		Assert.isTrue(!matches.isEmpty(), "No matching arg on " + method.toString());
		Assert.isTrue(matches.size() == 1, "Multiple matching args: " + matches + " on " + method.toString());

		return matches.get(0);
	}

	@Override
	public String toString() {
		return "Class=" + this.objectClass +
				", name=" + (this.methodName != null ? this.methodName : "<not specified>") +
				", returnType=" + (this.returnType != null ? this.returnType : "<not specified>") +
				", annotations=" + this.annotationTypes;
	}


	public static ResolvableMethod onClass(Class<?> clazz) {
		return new ResolvableMethod(clazz);
	}

	public static ResolvableMethod on(Object object) {
		return new ResolvableMethod(object);
	}

}