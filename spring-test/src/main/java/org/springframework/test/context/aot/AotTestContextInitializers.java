/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.aot;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.aot.AotDetector;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;

/**
 * {@code AotTestContextInitializers} provides mappings from test classes to
 * AOT-optimized context initializers.
 *
 * <p>Intended solely for internal use within the framework.
 *
 * <p>If we are not running in {@linkplain AotDetector#useGeneratedArtifacts()
 * AOT mode} or if a test class is not {@linkplain #isSupportedTestClass(Class)
 * supported} in AOT mode, {@link #getContextInitializer(Class)} will return
 * {@code null}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public class AotTestContextInitializers {

	private final Map<String, Supplier<ApplicationContextInitializer<ConfigurableApplicationContext>>> contextInitializers;


	public AotTestContextInitializers() {
		this(AotTestContextInitializersFactory.getContextInitializers());
	}

	AotTestContextInitializers(Map<String, Supplier<ApplicationContextInitializer<ConfigurableApplicationContext>>> contextInitializers) {
		this.contextInitializers = contextInitializers;
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
	 */
	@Nullable
	public ApplicationContextInitializer<ConfigurableApplicationContext> getContextInitializer(Class<?> testClass) {
		Supplier<ApplicationContextInitializer<ConfigurableApplicationContext>> supplier =
				this.contextInitializers.get(testClass.getName());
		return (supplier != null ? supplier.get() : null);
	}

}
