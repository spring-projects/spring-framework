/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.util.MetaAnnotationUtils;

import java.lang.reflect.Method;

import static java.lang.String.format;
import static org.springframework.test.context.support.ContextLoaderUtils.buildContextHierarchyMap;
import static org.springframework.test.context.support.ContextLoaderUtils.resolveContextConfigurationAttributes;

/**
 * Default implementation of the {@link TestContextBootstrapper} SPI for supplied test method.
 * <p>Uses {@link DelegatingSmartContextLoader} as the default {@link ContextLoader}.
 *
 * @author Sergei Ustimenko
 * @since 5.0
 */
public class DefaultTestMethodContextBootstrapper extends DefaultTestContextBootstrapper {
	private final Log logger = LogFactory.getLog(getClass());

	private final Method testMethod;

	public DefaultTestMethodContextBootstrapper(Method testMethod) {
		this.testMethod = testMethod;
	}

	/**
	 * Build a new {@link DefaultTestContext} using the {@linkplain Method test method}.
	 * {@link #getCacheAwareContextLoaderDelegate()}.
	 */
	@Override
	public TestContext buildTestContext() {
		return new DefaultTestContext(getBootstrapContext().getTestClass(),
				buildMergedContextConfiguration(testMethod),
				getCacheAwareContextLoaderDelegate());
	}

	/**
	 * Build the {@linkplain MergedContextConfiguration merged context configuration}
	 * for the supplied test testMethod.
	 *
	 * @param testMethod supplied testMethod for which to create merged context configuration
	 *
	 * @return the merged context configuration, never {@code null}
	 *
	 * @see #buildMergedContextConfiguration()
	 * @see #buildTestContext()
	 */
	protected MergedContextConfiguration buildMergedContextConfiguration(Method testMethod) {
		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = getCacheAwareContextLoaderDelegate();

		if (MetaAnnotationUtils.findAnnotationDescriptorForTypes(
				testMethod, ContextConfiguration.class, ContextHierarchy.class) == null) {
			String msg = format("Neither @ContextConfiguration nor @ContextHierarchy found for test method [%s]",
					testMethod.getName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}

		if (AnnotationUtils.findAnnotation(testMethod, ContextHierarchy.class) != null) {
			return buildMergedConfigFromHierarchyMap(() -> buildContextHierarchyMap(testMethod));
		} else {
			return buildMergedContextConfiguration(testMethod.getDeclaringClass(),
					resolveContextConfigurationAttributes(testMethod), null, cacheAwareContextLoaderDelegate, true);
		}
	}
}
