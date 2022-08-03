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

package org.springframework.context.aot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Initializes a {@link ConfigurableApplicationContext} using AOT optimizations.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class ApplicationContextAotInitializer {

	private static final Log logger = LogFactory.getLog(ApplicationContextAotInitializer.class);

	/**
	 * Initialize the specified application context using the specified
	 * {@link ApplicationContextInitializer} class names. Each class is
	 * expected to have a default constructor.
	 * @param context the context to initialize
	 * @param initializerClassNames the application context initializer class names
	 */
	public void initialize(ConfigurableApplicationContext context, String... initializerClassNames) {
		if (logger.isDebugEnabled()) {
			logger.debug("Initializing ApplicationContext with AOT");
		}
		for (String initializerClassName : initializerClassNames) {
			if (logger.isTraceEnabled()) {
				logger.trace("Applying " + initializerClassName);
			}
			loadInitializer(initializerClassName, context.getClassLoader()).initialize(context);
		}
	}

	@SuppressWarnings("unchecked")
	private ApplicationContextInitializer<ConfigurableApplicationContext> loadInitializer(
			String className, @Nullable ClassLoader classLoader) {
		Object initializer = instantiate(className, classLoader);
		if (!(initializer instanceof ApplicationContextInitializer)) {
			throw new IllegalArgumentException("Not an ApplicationContextInitializer: " + className);
		}
		return (ApplicationContextInitializer<ConfigurableApplicationContext>) initializer;
	}

	private static Object instantiate(String className, @Nullable ClassLoader classLoader) {
		try {
			Class<?> type = ClassUtils.forName(className, classLoader);
			return BeanUtils.instantiateClass(type);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Failed to instantiate ApplicationContextInitializer: " + className, ex);
		}
	}

}
