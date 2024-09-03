/*
 * Copyright 2002-2023 the original author or authors.
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
 * Factory for {@link AotTestContextInitializers}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
final class AotTestContextInitializersFactory {

	@Nullable
	private static volatile Map<String, Supplier<ApplicationContextInitializer<ConfigurableApplicationContext>>> contextInitializers;

	@Nullable
	private static volatile Map<String, Class<ApplicationContextInitializer<?>>> contextInitializerClasses;


	private AotTestContextInitializersFactory() {
	}

	/**
	 * Get the underlying map.
	 * <p>If the map is not already loaded, this method loads the map from the
	 * generated class when running in {@linkplain AotDetector#useGeneratedArtifacts()
	 * AOT execution mode} and otherwise creates an immutable, empty map.
	 */
	static Map<String, Supplier<ApplicationContextInitializer<ConfigurableApplicationContext>>> getContextInitializers() {
		Map<String, Supplier<ApplicationContextInitializer<ConfigurableApplicationContext>>> initializers = contextInitializers;
		if (initializers == null) {
			synchronized (AotTestContextInitializersFactory.class) {
				initializers = contextInitializers;
				if (initializers == null) {
					initializers = (AotDetector.useGeneratedArtifacts() ? loadContextInitializersMap() : Map.of());
					contextInitializers = initializers;
				}
			}
		}
		return initializers;
	}

	static Map<String, Class<ApplicationContextInitializer<?>>> getContextInitializerClasses() {
		Map<String, Class<ApplicationContextInitializer<?>>> initializerClasses = contextInitializerClasses;
		if (initializerClasses == null) {
			synchronized (AotTestContextInitializersFactory.class) {
				initializerClasses = contextInitializerClasses;
				if (initializerClasses == null) {
					initializerClasses = (AotDetector.useGeneratedArtifacts() ? loadContextInitializerClassesMap() : Map.of());
					contextInitializerClasses = initializerClasses;
				}
			}
		}
		return initializerClasses;
	}

	/**
	 * Reset the factory.
	 * <p>Only for internal use.
	 */
	static void reset() {
		synchronized (AotTestContextInitializersFactory.class) {
			contextInitializers = null;
			contextInitializerClasses = null;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Supplier<ApplicationContextInitializer<ConfigurableApplicationContext>>> loadContextInitializersMap() {
		String className = AotTestContextInitializersCodeGenerator.GENERATED_MAPPINGS_CLASS_NAME;
		String methodName = AotTestContextInitializersCodeGenerator.GET_CONTEXT_INITIALIZERS_METHOD_NAME;
		return GeneratedMapUtils.loadMap(className, methodName);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Class<ApplicationContextInitializer<?>>> loadContextInitializerClassesMap() {
		String className = AotTestContextInitializersCodeGenerator.GENERATED_MAPPINGS_CLASS_NAME;
		String methodName = AotTestContextInitializersCodeGenerator.GET_CONTEXT_INITIALIZER_CLASSES_METHOD_NAME;
		return GeneratedMapUtils.loadMap(className, methodName);
	}

}
