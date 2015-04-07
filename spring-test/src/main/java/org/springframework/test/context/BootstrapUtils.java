/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ClassUtils;

import static org.springframework.beans.BeanUtils.*;
import static org.springframework.core.annotation.AnnotationUtils.*;

/**
 * {@code BootstrapUtils} is a collection of utility methods to assist with
 * bootstrapping the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see BootstrapWith
 * @see BootstrapContext
 * @see TestContextBootstrapper
 */
abstract class BootstrapUtils {

	private static final String DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME = "org.springframework.test.context.support.DefaultTestContextBootstrapper";

	private static final Log logger = LogFactory.getLog(BootstrapUtils.class);


	private BootstrapUtils() {
		/* no-op */
	}

	/**
	 * Resolve the {@link TestContextBootstrapper} type for the test class in the
	 * supplied {@link BootstrapContext}, instantiate it, and provide it a reference
	 * to the {@link BootstrapContext}.
	 *
	 * <p>If the {@link BootstrapWith @BootstrapWith} annotation is present on
	 * the test class, either directly or as a meta-annotation, then its
	 * {@link BootstrapWith#value value} will be used as the bootstrapper type.
	 * Otherwise, the {@link org.springframework.test.context.support.DefaultTestContextBootstrapper
	 * DefaultTestContextBootstrapper} will be used.
	 *
	 * @param bootstrapContext the bootstrap context to use
	 * @return a fully configured {@code TestContextBootstrapper}
	 */
	@SuppressWarnings("unchecked")
	static TestContextBootstrapper resolveTestContextBootstrapper(BootstrapContext bootstrapContext) {
		Class<?> testClass = bootstrapContext.getTestClass();

		Class<? extends TestContextBootstrapper> clazz = null;
		try {
			BootstrapWith bootstrapWith = findAnnotation(testClass, BootstrapWith.class);
			if (bootstrapWith != null && !TestContextBootstrapper.class.equals(bootstrapWith.value())) {
				clazz = bootstrapWith.value();
			}
			else {
				clazz = (Class<? extends TestContextBootstrapper>) ClassUtils.forName(
					DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME, BootstrapUtils.class.getClassLoader());
			}

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating TestContextBootstrapper from class [%s]", clazz.getName()));
			}

			TestContextBootstrapper testContextBootstrapper = instantiateClass(clazz, TestContextBootstrapper.class);
			testContextBootstrapper.setBootstrapContext(bootstrapContext);

			return testContextBootstrapper;
		}
		catch (Throwable t) {
			throw new IllegalStateException("Could not load TestContextBootstrapper [" + clazz
					+ "]. Specify @BootstrapWith's 'value' attribute "
					+ "or make the default bootstrapper class available.", t);
		}
	}

}
