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

package org.springframework.test.context.aot.hint;

import java.util.Arrays;
import java.util.List;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ActiveProfilesResolver;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.aot.TestRuntimeHintsRegistrar;
import org.springframework.test.context.web.WebMergedContextConfiguration;

import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY;
import static org.springframework.util.ResourceUtils.CLASSPATH_URL_PREFIX;

/**
 * {@link TestRuntimeHintsRegistrar} implementation that registers run-time hints
 * for standard functionality in the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see TestContextRuntimeHints
 */
class StandardTestRuntimeHints implements TestRuntimeHintsRegistrar {

	private static final String SLASH = "/";


	@Override
	public void registerHints(MergedContextConfiguration mergedConfig, List<Class<?>> testClasses,
			RuntimeHints runtimeHints, ClassLoader classLoader) {

		registerHintsForMergedContextConfiguration(mergedConfig, runtimeHints, classLoader);
		testClasses.forEach(testClass -> registerHintsForActiveProfilesResolvers(testClass, runtimeHints));
	}

	private void registerHintsForMergedContextConfiguration(
			MergedContextConfiguration mergedConfig, RuntimeHints runtimeHints, ClassLoader classLoader) {

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

		// @TestPropertySource(locations = ... )
		registerClasspathResources(mergedConfig.getPropertySourceLocations(), runtimeHints, classLoader);

		// @WebAppConfiguration(value = ...)
		if (mergedConfig instanceof WebMergedContextConfiguration webConfig) {
			registerClasspathResourceDirectoryStructure(webConfig.getResourceBasePath(), runtimeHints);
		}
	}

	private void registerHintsForActiveProfilesResolvers(Class<?> testClass, RuntimeHints runtimeHints) {
		// @ActiveProfiles(resolver = ...)
		MergedAnnotations.search(TYPE_HIERARCHY)
				.withEnclosingClasses(TestContextAnnotationUtils::searchEnclosingClass)
				.from(testClass)
				.stream(ActiveProfiles.class)
				.map(mergedAnnotation -> mergedAnnotation.getClass("resolver"))
				.filter(type -> type != ActiveProfilesResolver.class)
				.forEach(resolverClass -> registerDeclaredConstructors(resolverClass, runtimeHints));
	}

	private void registerDeclaredConstructors(Class<?> type, RuntimeHints runtimeHints) {
		runtimeHints.reflection().registerType(type, INVOKE_DECLARED_CONSTRUCTORS);
	}

	private void registerClasspathResources(String[] paths, RuntimeHints runtimeHints, ClassLoader classLoader) {
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader(classLoader);
		Arrays.stream(paths)
				.filter(path -> path.startsWith(CLASSPATH_URL_PREFIX))
				.map(resourceLoader::getResource)
				.forEach(runtimeHints.resources()::registerResource);
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

}
