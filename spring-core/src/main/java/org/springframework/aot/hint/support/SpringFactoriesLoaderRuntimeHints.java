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

package org.springframework.aot.hint.support;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} to register hints for {@code spring.factories}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 6.0
 * @see SpringFactoriesLoader
 */
class SpringFactoriesLoaderRuntimeHints implements RuntimeHintsRegistrar {

	private static final List<String> RESOURCE_LOCATIONS =
			List.of(SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION);

	private static final Log logger = LogFactory.getLog(SpringFactoriesLoaderRuntimeHints.class);


	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		ClassLoader classLoaderToUse = (classLoader != null ? classLoader :
				SpringFactoriesLoaderRuntimeHints.class.getClassLoader());
		for (String resourceLocation : RESOURCE_LOCATIONS) {
			registerHints(hints, classLoaderToUse, resourceLocation);
		}
	}

	private void registerHints(RuntimeHints hints, ClassLoader classLoader, String resourceLocation) {
		hints.resources().registerPattern(resourceLocation);
		Map<String, List<String>> factories =
				ExtendedSpringFactoriesLoader.accessLoadFactoriesResource(classLoader, resourceLocation);
		factories.forEach((factoryClassName, implementationClassNames) ->
				registerHints(hints, classLoader, factoryClassName, implementationClassNames));
	}

	private void registerHints(RuntimeHints hints, ClassLoader classLoader,
			String factoryClassName, List<String> implementationClassNames) {

		Class<?> factoryClass = resolveClassName(classLoader, factoryClassName);
		if (factoryClass == null) {
			if (logger.isTraceEnabled()) {
				logger.trace(LogMessage.format("Skipping factories for [%s]", factoryClassName));
			}
			return;
		}
		if (logger.isTraceEnabled()) {
			logger.trace(LogMessage.format("Processing factories for [%s]", factoryClassName));
		}
		hints.reflection().registerType(factoryClass, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		for (String implementationClassName : implementationClassNames) {
			Class<?> implementationType = resolveClassName(classLoader, implementationClassName);
			if (logger.isTraceEnabled()) {
				logger.trace(LogMessage.format("%s factory type [%s] and implementation [%s]",
						(implementationType != null ? "Processing" : "Skipping"), factoryClassName,
						implementationClassName));
			}
			if (implementationType != null) {
				hints.reflection().registerType(implementationType, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			}
		}
	}

	@Nullable
	private Class<?> resolveClassName(ClassLoader classLoader, String factoryClassName) {
		try {
			Class<?> clazz = ClassUtils.resolveClassName(factoryClassName, classLoader);
			// Force resolution of all constructors to cache
			clazz.getDeclaredConstructors();
			return clazz;
		}
		catch (Throwable ex) {
			return null;
		}
	}


	private static class ExtendedSpringFactoriesLoader extends SpringFactoriesLoader {

		ExtendedSpringFactoriesLoader(@Nullable ClassLoader classLoader, Map<String, List<String>> factories) {
			super(classLoader, factories);
		}

		static Map<String, List<String>> accessLoadFactoriesResource(ClassLoader classLoader, String resourceLocation) {
			return SpringFactoriesLoader.loadFactoriesResource(classLoader, resourceLocation);
		}
	}

}
