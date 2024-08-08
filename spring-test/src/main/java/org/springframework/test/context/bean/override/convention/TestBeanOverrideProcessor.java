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

package org.springframework.test.context.bean.override.convention;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.MethodIntrospector;
import org.springframework.core.ResolvableType;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.bean.override.BeanOverrideProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.util.StringUtils;

/**
 * {@link BeanOverrideProcessor} implementation for {@link TestBean @TestBean}
 * support, which creates metadata for annotated fields in a given class and
 * ensures that a corresponding static factory method exists, according to the
 * {@linkplain TestBean documented conventions}.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 6.2
 */
class TestBeanOverrideProcessor implements BeanOverrideProcessor {

	@Override
	public TestBeanOverrideMetadata createMetadata(Annotation overrideAnnotation, Class<?> testClass, Field field) {
		if (!(overrideAnnotation instanceof TestBean testBeanAnnotation)) {
			throw new IllegalStateException("Invalid annotation passed to %s: expected @TestBean on field %s.%s"
					.formatted(getClass().getSimpleName(), field.getDeclaringClass().getName(), field.getName()));
		}
		Method overrideMethod;
		String methodName = testBeanAnnotation.methodName();
		if (!methodName.isBlank()) {
			// If the user specified an explicit method name, search for that.
			overrideMethod = findTestBeanFactoryMethod(testClass, field.getType(), methodName);
		}
		else {
			// Otherwise, search for candidate factory methods the field name
			// or explicit bean name (if any).
			List<String> candidateMethodNames = new ArrayList<>();
			candidateMethodNames.add(field.getName());

			String beanName = testBeanAnnotation.name();
			if (StringUtils.hasText(beanName)) {
				candidateMethodNames.add(beanName);
			}
			overrideMethod = findTestBeanFactoryMethod(testClass, field.getType(), candidateMethodNames);
		}
		String beanName = (StringUtils.hasText(testBeanAnnotation.name()) ? testBeanAnnotation.name() : null);
		return new TestBeanOverrideMetadata(field, ResolvableType.forField(field, testClass), beanName, overrideMethod);
	}

	/**
	 * Find a test bean factory {@link Method} for the given {@link Class}.
	 * <p>Delegates to {@link #findTestBeanFactoryMethod(Class, Class, Collection)}.
	 */
	Method findTestBeanFactoryMethod(Class<?> clazz, Class<?> methodReturnType, String... methodNames) {
		return findTestBeanFactoryMethod(clazz, methodReturnType, List.of(methodNames));
	}

	/**
	 * Find a test bean factory {@link Method} for the given {@link Class}, which
	 * meets the following criteria.
	 * <ul>
	 * <li>The method is static.</li>
	 * <li>The method does not accept any arguments.</li>
	 * <li>The method's return type matches the supplied {@code methodReturnType}.</li>
	 * <li>The method's name is one of the supplied {@code methodNames}.</li>
	 * </ul>
	 * <p>This method traverses up the type hierarchy of the given class in search
	 * of the factory method, beginning with the class itself and then searching
	 * implemented interfaces and superclasses. If a factory method is not found
	 * in the type hierarchy, this method will also search the enclosing class
	 * hierarchy if the class is a nested class.
	 * <p>If multiple factory methods are found that match the search criteria,
	 * an exception is thrown.
	 * @param clazz the class in which to search for the factory method
	 * @param methodReturnType the return type for the factory method
	 * @param methodNames a set of supported names for the factory method
	 * @return the corresponding factory method
	 * @throws IllegalStateException if a matching factory method cannot
	 * be found or multiple methods match
	 */
	Method findTestBeanFactoryMethod(Class<?> clazz, Class<?> methodReturnType, Collection<String> methodNames) {
		Assert.notEmpty(methodNames, "At least one candidate method name is required");
		Set<Method> methods = new LinkedHashSet<>();
		Set<String> originalNames = new LinkedHashSet<>(methodNames);

		// Process fully-qualified method names first.
		for (String methodName : methodNames) {
			int indexOfHash = methodName.lastIndexOf('#');
			if (indexOfHash != -1) {
				String className = methodName.substring(0, indexOfHash).trim();
				Assert.hasText(className, () -> "No class name present in fully-qualified method name: " + methodName);
				String methodNameToUse = methodName.substring(indexOfHash + 1).trim();
				Assert.hasText(methodNameToUse, () -> "No method name present in fully-qualified method name: " + methodName);
				Class<?> declaringClass;
				try {
					declaringClass = ClassUtils.forName(className, getClass().getClassLoader());
				}
				catch (ClassNotFoundException | LinkageError ex) {
					throw new IllegalStateException(
							"Failed to load class for fully-qualified method name: " + methodName, ex);
				}
				Method externalMethod = ReflectionUtils.findMethod(declaringClass, methodNameToUse);
				Assert.state(externalMethod != null && Modifier.isStatic(externalMethod.getModifiers()) &&
						methodReturnType.isAssignableFrom(externalMethod.getReturnType()), () ->
								"No static method found named %s in %s with return type %s".formatted(
										methodNameToUse, className, methodReturnType.getName()));
				methods.add(externalMethod);
				originalNames.remove(methodName);
			}
		}

		Set<String> supportedNames = new LinkedHashSet<>(originalNames);
		MethodFilter methodFilter = method -> (Modifier.isStatic(method.getModifiers()) &&
				supportedNames.contains(method.getName()) &&
				methodReturnType.isAssignableFrom(method.getReturnType()));
		findMethods(methods, clazz, methodFilter);

		String methodNamesDescription = supportedNames.stream()
				.map(name -> name + "()").collect(Collectors.joining(" or "));
		Assert.state(!methods.isEmpty(), () ->
				"No static method found named %s in %s with return type %s".formatted(
						methodNamesDescription, clazz.getName(), methodReturnType.getName()));

		long uniqueMethodNameCount = methods.stream().map(Method::getName).distinct().count();
		Assert.state(uniqueMethodNameCount == 1, () ->
				"Found %d competing static methods named %s in %s with return type %s".formatted(
						uniqueMethodNameCount, methodNamesDescription, clazz.getName(), methodReturnType.getName()));

		return methods.iterator().next();
	}

	private static Set<Method> findMethods(Set<Method> methods, Class<?> clazz, MethodFilter methodFilter) {
		methods.addAll(MethodIntrospector.selectMethods(clazz, methodFilter));
		if (methods.isEmpty() && TestContextAnnotationUtils.searchEnclosingClass(clazz)) {
			findMethods(methods, clazz.getEnclosingClass(), methodFilter);
		}
		return methods;
	}

}
