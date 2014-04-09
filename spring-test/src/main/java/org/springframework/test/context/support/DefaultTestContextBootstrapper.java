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

package org.springframework.test.context.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.TestExecutionListener;

/**
 * Default implementation of the {@link TestContextBootstrapper} SPI.
 *
 * <ul>
 * <li>Uses the following default {@link TestExecutionListener TestExecutionListeners}:
 * <ol>
 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener}
 * <li>{@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}
 * </ol>
 * <li>Uses {@link DelegatingSmartContextLoader} as the default {@link ContextLoader}.
 * <li>Builds a standard {@link MergedContextConfiguration}.
 * </ul>
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class DefaultTestContextBootstrapper extends AbstractTestContextBootstrapper {

	private static final List<String> DEFAULT_TEST_EXECUTION_LISTENER_CLASS_NAMES = Collections.unmodifiableList(Arrays.asList(
		"org.springframework.test.context.support.DependencyInjectionTestExecutionListener",
		"org.springframework.test.context.support.DirtiesContextTestExecutionListener",
		"org.springframework.test.context.transaction.TransactionalTestExecutionListener"));


	/**
	 * Returns an unmodifiable list of fully qualified class names for the following
	 * default {@link TestExecutionListener TestExecutionListeners}:
	 * <ol>
	 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener}
	 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener}
	 * <li>{@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}
	 * </ol>
	 */
	protected List<String> getDefaultTestExecutionListenerClassNames() {
		return DEFAULT_TEST_EXECUTION_LISTENER_CLASS_NAMES;
	}

	/**
	 * Returns {@link DelegatingSmartContextLoader}.
	 */
	@Override
	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
		return DelegatingSmartContextLoader.class;
	}

	/**
	 * Builds a standard {@link MergedContextConfiguration}.
	 */
	protected MergedContextConfiguration buildMergedContextConfiguration(
			Class<?> testClass,
			String[] locations,
			Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses,
			String[] activeProfiles, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, MergedContextConfiguration parentConfig) {

		return new MergedContextConfiguration(testClass, locations, classes, initializerClasses, activeProfiles,
			contextLoader, cacheAwareContextLoaderDelegate, parentConfig);
	}

}
