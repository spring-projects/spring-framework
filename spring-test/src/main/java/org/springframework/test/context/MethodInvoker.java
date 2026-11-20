/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

/**
 * {@code MethodInvoker} defines a generic API for invoking a {@link Method}
 * within the <em>Spring TestContext Framework</em>.
 *
 * <p>Specifically, a {@code MethodInvoker} is made available to a
 * {@link TestExecutionListener} via {@link TestContext#getMethodInvoker()}, and
 * a {@code TestExecutionListener} can use the invoker to transparently benefit
 * from any special method invocation features of the underlying testing framework.
 *
 * <p>For example, when the underlying testing framework is JUnit Jupiter, a
 * {@code TestExecutionListener} can use a {@code MethodInvoker} to invoke
 * arbitrary methods with JUnit Jupiter's
 * {@linkplain org.junit.jupiter.api.extension.ExecutableInvoker parameter resolution
 * mechanism}. For other testing frameworks, the {@link #DEFAULT_INVOKER} will be
 * used.
 *
 * @author Sam Brannen
 * @since 6.1
 * @see org.junit.jupiter.api.extension.ExecutableInvoker
 * @see org.springframework.util.MethodInvoker
 */
public interface MethodInvoker {

	/**
	 * Shared instance of the default {@link MethodInvoker}.
	 * <p>This invoker never provides arguments to a {@link Method}.
	 */
	MethodInvoker DEFAULT_INVOKER = new DefaultMethodInvoker();


	/**
	 * Invoke the supplied {@link Method} on the supplied {@code target}.
	 * <p>When the {@link #DEFAULT_INVOKER} is used &mdash; for example, when
	 * the underlying testing framework is JUnit 4 or TestNG &mdash; the method
	 * must not declare any formal parameters. When the underlying testing
	 * framework is JUnit Jupiter, parameters will be dynamically resolved via
	 * registered {@link org.junit.jupiter.api.extension.ParameterResolver
	 * ParameterResolvers} (such as the
	 * {@link org.springframework.test.context.junit.jupiter.SpringExtension
	 * SpringExtension}).
	 * @param method the method to invoke
	 * @param target the object on which to invoke the method, may be {@code null}
	 * if the method is {@code static}
	 * @return the value returned from the method invocation, potentially {@code null}
	 * @throws Exception if any error occurs
	 */
	@Nullable Object invoke(Method method, @Nullable Object target) throws Exception;

}
