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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertySourceDescriptor;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ClassUtils;

import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.util.ResourceUtils.CLASSPATH_URL_PREFIX;

/**
 * {@code MergedContextConfigurationRuntimeHints} registers run-time hints for
 * standard functionality in the <em>Spring TestContext Framework</em> based on
 * {@link MergedContextConfiguration}.
 *
 * <p>This class interacts with {@code org.springframework.test.context.web.WebMergedContextConfiguration}
 * via reflection to avoid a package cycle.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class MergedContextConfigurationRuntimeHints {

	private static final String SLASH = "/";

	private static final String WEB_MERGED_CONTEXT_CONFIGURATION_CLASS_NAME =
			"org.springframework.test.context.web.WebMergedContextConfiguration";

	private static final String GET_RESOURCE_BASE_PATH_METHOD_NAME = "getResourceBasePath";

	private static final Class<?> webMergedContextConfigurationClass = loadWebMergedContextConfigurationClass();

	private static final Method getResourceBasePathMethod = loadGetResourceBasePathMethod();


	@SuppressWarnings("deprecation")
	public void registerHints(RuntimeHints runtimeHints, MergedContextConfiguration mergedConfig, ClassLoader classLoader) {
		// @ContextConfiguration(loader = ...)
		ContextLoader contextLoader = mergedConfig.getContextLoader();
		if (contextLoader != null) {
			registerDeclaredConstructors(contextLoader.getClass(), runtimeHints);
		}

		// @ContextConfiguration(initializers = ...)
		mergedConfig.getContextInitializerClasses()
				.forEach(clazz -> registerDeclaredConstructors(clazz, runtimeHints));

		// @ContextConfiguration(locations = ...)
		registerClasspathResources(mergedConfig.getLocations(), runtimeHints, classLoader);

		for (PropertySourceDescriptor descriptor : mergedConfig.getPropertySourceDescriptors()) {
			// @TestPropertySource(locations = ...)
			registerClasspathResources(descriptor.locations().stream(), runtimeHints, classLoader);

			// @TestPropertySource(factory = ...)
			Class<?> factoryClass = descriptor.propertySourceFactory();
			if (factoryClass != null) {
				registerDeclaredConstructors(factoryClass, runtimeHints);
			}
		}

		// @WebAppConfiguration(value = ...)
		if (webMergedContextConfigurationClass.isInstance(mergedConfig)) {
			String resourceBasePath = null;
			try {
				resourceBasePath = (String) getResourceBasePathMethod.invoke(mergedConfig);
			}
			catch (Exception ex) {
				throw new IllegalStateException(
						"Failed to invoke WebMergedContextConfiguration#getResourceBasePath()", ex);
			}
			registerClasspathResourceDirectoryStructure(resourceBasePath, runtimeHints);
		}
	}

	private void registerDeclaredConstructors(Class<?> type, RuntimeHints runtimeHints) {
		runtimeHints.reflection().registerType(type, INVOKE_DECLARED_CONSTRUCTORS);
	}

	private void registerClasspathResources(String[] locations, RuntimeHints runtimeHints, ClassLoader classLoader) {
		registerClasspathResources(Arrays.stream(locations), runtimeHints, classLoader);
	}

	private void registerClasspathResources(Stream<String> locations, RuntimeHints runtimeHints, ClassLoader classLoader) {
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader(classLoader);
		ResourceHints resourceHints = runtimeHints.resources();
		locations.map(resourceLoader::getResource)
				.filter(ClassPathResource.class::isInstance)
				.filter(Resource::exists)
				.forEach(resourceHints::registerResource);
	}

	private void registerClasspathResourceDirectoryStructure(String directory, RuntimeHints runtimeHints) {
		if (directory.startsWith(CLASSPATH_URL_PREFIX)) {
			String pattern = directory.substring(CLASSPATH_URL_PREFIX.length());
			if (pattern.startsWith(SLASH)) {
				pattern = pattern.substring(1);
			}
			if (!pattern.endsWith(SLASH)) {
				pattern += SLASH;
			}
			pattern += "*";
			runtimeHints.resources().registerPattern(pattern);
		}
	}

	@SuppressWarnings("unchecked")
	private static Class<?> loadWebMergedContextConfigurationClass() {
		try {
			return ClassUtils.forName(WEB_MERGED_CONTEXT_CONFIGURATION_CLASS_NAME,
					MergedContextConfigurationRuntimeHints.class.getClassLoader());
		}
		catch (ClassNotFoundException | LinkageError ex) {
			throw new IllegalStateException(
					"Failed to load class " + WEB_MERGED_CONTEXT_CONFIGURATION_CLASS_NAME, ex);
		}
	}

	private static Method loadGetResourceBasePathMethod() {
		try {
			return webMergedContextConfigurationClass.getMethod(GET_RESOURCE_BASE_PATH_METHOD_NAME);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Failed to load method WebMergedContextConfiguration#getResourceBasePath()", ex);
		}
	}

}
