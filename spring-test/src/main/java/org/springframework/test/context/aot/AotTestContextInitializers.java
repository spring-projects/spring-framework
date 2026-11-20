/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot;

import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.AotDetector;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@code AotTestContextInitializers} provides mappings from test classes to
 * AOT-optimized context initializers.
 *
 * <p>Intended solely for internal use within the framework.
 *
 * <p>If we are not running in {@linkplain AotDetector#useGeneratedArtifacts()
 * AOT mode} or if a test class is not {@linkplain #isSupportedTestClass(Class)
 * supported} in AOT mode, {@link #getContextInitializer(Class)} and
 * {@link #getContextInitializerClass(Class)} will return {@code null}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public class AotTestContextInitializers {

	private final Map<String, Supplier<ApplicationContextInitializer<ConfigurableApplicationContext>>> contextInitializers;

	private final Map<String, Class<ApplicationContextInitializer<?>>> contextInitializerClasses;


	public AotTestContextInitializers() {
		this(AotTestContextInitializersFactory.getContextInitializers(),
			AotTestContextInitializersFactory.getContextInitializerClasses());
	}

	AotTestContextInitializers(
			Map<String, Supplier<ApplicationContextInitializer<ConfigurableApplicationContext>>> contextInitializers,
			Map<String, Class<ApplicationContextInitializer<?>>> contextInitializerClasses) {

		this.contextInitializers = contextInitializers;
		this.contextInitializerClasses = contextInitializerClasses;
	}


	/**
	 * Determine if the specified test class has an AOT-optimized application context
	 * initializer.
	 * <p>If this method returns {@code true}, {@link #getContextInitializer(Class)}
	 * should not return {@code null}.
	 */
	public boolean isSupportedTestClass(Class<?> testClass) {
		return this.contextInitializers.containsKey(testClass.getName());
	}

	/**
	 * Get the AOT {@link ApplicationContextInitializer} for the specified test class.
	 * @return the AOT context initializer, or {@code null} if there is no AOT context
	 * initializer for the specified test class
	 * @see #isSupportedTestClass(Class)
	 * @see #getContextInitializerClass(Class)
	 */
	public @Nullable ApplicationContextInitializer<ConfigurableApplicationContext> getContextInitializer(Class<?> testClass) {
		Supplier<ApplicationContextInitializer<ConfigurableApplicationContext>> supplier =
				this.contextInitializers.get(testClass.getName());
		return (supplier != null ? supplier.get() : null);
	}

	/**
	 * Get the AOT {@link ApplicationContextInitializer} {@link Class} for the
	 * specified test class.
	 * @return the AOT context initializer class, or {@code null} if there is no
	 * AOT context initializer for the specified test class
	 * @see #isSupportedTestClass(Class)
	 * @see #getContextInitializer(Class)
	 */
	public @Nullable Class<ApplicationContextInitializer<?>> getContextInitializerClass(Class<?> testClass) {
		return this.contextInitializerClasses.get(testClass.getName());
	}

}
