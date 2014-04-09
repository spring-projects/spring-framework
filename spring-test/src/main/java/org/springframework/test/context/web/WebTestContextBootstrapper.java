/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

/**
 * Web-specific implementation of the {@link TestContextBootstrapper} SPI.
 *
 * <ul>
 * <li>Prepends {@link org.springframework.test.context.web.ServletTestExecutionListener}
 * to the list of default {@link TestExecutionListener TestExecutionListeners} supported by
 * the superclass.
 * <li>Uses {@link WebDelegatingSmartContextLoader} as the default {@link ContextLoader}
 * if the test class is annotated with {@link WebAppConfiguration @WebAppConfiguration}
 * and otherwise delegates to the superclass.
 * <li>Builds a {@link WebMergedContextConfiguration} if the test class is annotated
 * with {@link WebAppConfiguration @WebAppConfiguration} and otherwise delegates to
 * the superclass.
 * </ul>
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class WebTestContextBootstrapper extends DefaultTestContextBootstrapper {

	/**
	 * Prepends {@link org.springframework.test.context.web.ServletTestExecutionListener}
	 * to the list of default {@link TestExecutionListener TestExecutionListeners}
	 * supported by the superclass and returns an unmodifiable, updated list.
	 */
	@Override
	protected List<String> getDefaultTestExecutionListenerClassNames() {
		List<String> classNames = new ArrayList<String>(super.getDefaultTestExecutionListenerClassNames());
		classNames.add(0, "org.springframework.test.context.web.ServletTestExecutionListener");
		return Collections.unmodifiableList(classNames);
	}

	/**
	 * Returns {@link WebDelegatingSmartContextLoader} if the supplied class is
	 * annotated with {@link WebAppConfiguration @WebAppConfiguration} and
	 * otherwise delegates to the superclass.
	 */
	@Override
	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
		if (AnnotationUtils.findAnnotation(testClass, WebAppConfiguration.class) != null) {
			return WebDelegatingSmartContextLoader.class;
		}

		// else...
		return super.getDefaultContextLoaderClass(testClass);
	}

	/**
	 * Builds a {@link WebMergedContextConfiguration} if the supplied class is
	 * annotated with {@link WebAppConfiguration @WebAppConfiguration} and
	 * otherwise delegates to the superclass.
	 */
	@Override
	protected MergedContextConfiguration buildMergedContextConfiguration(
			Class<?> testClass,
			String[] locations,
			Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses,
			String[] activeProfiles, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, MergedContextConfiguration parentConfig) {

		WebAppConfiguration webAppConfiguration = AnnotationUtils.findAnnotation(testClass, WebAppConfiguration.class);
		if (webAppConfiguration != null) {
			String resourceBasePath = webAppConfiguration.value();

			return new WebMergedContextConfiguration(testClass, locations, classes, initializerClasses, activeProfiles,
				resourceBasePath, contextLoader, cacheAwareContextLoaderDelegate, parentConfig);
		}

		// else...
		return super.buildMergedContextConfiguration(testClass, locations, classes, initializerClasses, activeProfiles,
			contextLoader, cacheAwareContextLoaderDelegate, parentConfig);
	}

}
