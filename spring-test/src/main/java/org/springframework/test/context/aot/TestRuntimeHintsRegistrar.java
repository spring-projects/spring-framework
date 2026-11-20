/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot;

import org.springframework.aot.hint.RuntimeHints;

/**
 * Contract for registering {@link RuntimeHints} for integration tests run with
 * the <em>Spring TestContext Framework</em> based on the {@link ClassLoader}
 * of the deployment unit. Implementations should, if possible, use the specified
 * {@link ClassLoader} to determine if hints have to be contributed.
 *
 * <p>Implementations of this interface must be registered statically in
 * {@code META-INF/spring/aot.factories} by using the fully qualified name of this
 * interface as the key. A standard no-arg constructor is required for implementations.
 *
 * <p>This API serves as a companion to the core
 * {@link org.springframework.aot.hint.RuntimeHintsRegistrar RuntimeHintsRegistrar}
 * API. If you need to register global hints for testing support that are not
 * specific to particular test classes, favor implementing {@code RuntimeHintsRegistrar}
 * over this API.
 *
 * <p>As an alternative to implementing and registering a {@code TestRuntimeHintsRegistrar},
 * you may choose to annotate a test class with
 * {@link org.springframework.aot.hint.annotation.Reflective @Reflective},
 * {@link org.springframework.aot.hint.annotation.RegisterReflection @RegisterReflection},
 * or {@link org.springframework.context.annotation.ImportRuntimeHints @ImportRuntimeHints}.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see org.springframework.aot.hint.RuntimeHintsRegistrar
 */
public interface TestRuntimeHintsRegistrar {

	/**
	 * Contribute hints to the given {@link RuntimeHints} instance.
	 * @param runtimeHints the {@code RuntimeHints} to use
	 * @param testClass the test class to process
	 * @param classLoader the classloader to use
	 */
	void registerHints(RuntimeHints runtimeHints, Class<?> testClass, ClassLoader classLoader);

}
