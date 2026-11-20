/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;

import org.springframework.util.ClassUtils;

/**
 * Descriptor for a {@link Method Method} which holds a
 * reference to the method's {@linkplain #declaringClass declaring class},
 * {@linkplain #methodName name}, and {@linkplain #parameterTypes parameter types}.
 *
 * @author Sam Brannen
 * @since 6.0.11
 * @param declaringClass the method's declaring class
 * @param methodName the name of the method
 * @param parameterTypes the types of parameters accepted by the method
 */
record MethodDescriptor(Class<?> declaringClass, String methodName, Class<?>... parameterTypes) {

	/**
	 * Create a {@link MethodDescriptor} for the supplied bean class and method name.
	 * <p>The supplied {@code methodName} may be a {@linkplain Method#getName()
	 * simple method name} or a {@linkplain ClassUtils#getQualifiedMethodName(Method)
	 * qualified method name}.
	 * <p>If the method name is fully qualified, this utility will parse the
	 * method name and its declaring class from the qualified method name and then
	 * attempt to load the method's declaring class using the {@link ClassLoader}
	 * of the supplied {@code beanClass}. Otherwise, the returned descriptor will
	 * reference the supplied {@code beanClass} and {@code methodName}.
	 * @param beanName the bean name in the factory (for debugging purposes)
	 * @param beanClass the bean class
	 * @param methodName the name of the method
	 * @return a new {@code MethodDescriptor}; never {@code null}
	 */
	static MethodDescriptor create(String beanName, Class<?> beanClass, String methodName) {
		try {
			Class<?> declaringClass = beanClass;
			String methodNameToUse = methodName;

			// Parse fully-qualified method name if necessary.
			int indexOfDot = methodName.lastIndexOf('.');
			if (indexOfDot > 0) {
				String className = methodName.substring(0, indexOfDot);
				methodNameToUse = methodName.substring(indexOfDot + 1);
				if (!beanClass.getName().equals(className)) {
					declaringClass = ClassUtils.forName(className, beanClass.getClassLoader());
				}
			}
			return new MethodDescriptor(declaringClass, methodNameToUse);
		}
		catch (Exception | LinkageError ex) {
			throw new BeanDefinitionValidationException(
					"Could not create MethodDescriptor for method '%s' on bean with name '%s': %s"
						.formatted(methodName, beanName, ex.getMessage()));
		}
	}

}
