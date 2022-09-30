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

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Specialized {@link ApplicationContextInitializer} used to initialize a
 * {@link ConfigurableApplicationContext} using artifacts that were generated
 * ahead-of-time.
 * <p>
 * Instances of this initializer are usually created using
 * {@link #forInitializerClasses(String...)}, passing in the names of code
 * generated initializer classes.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @param <C> the application context type
 */
@FunctionalInterface
public interface AotApplicationContextInitializer<C extends ConfigurableApplicationContext>
		extends ApplicationContextInitializer<C> {

	/**
	 * Factory method to create a new {@link AotApplicationContextInitializer}
	 * instance that delegates to other initializers loaded from the given set
	 * of class names.
	 * @param <C> the application context type
	 * @param initializerClassNames the class names of the initializers to load
	 * @return a new {@link AotApplicationContextInitializer} instance
	 */
	static <C extends ConfigurableApplicationContext> AotApplicationContextInitializer<C> forInitializerClasses(
			String... initializerClassNames) {

		Assert.noNullElements(initializerClassNames, "'initializerClassNames' must not contain null elements");
		return applicationContext -> initialize(applicationContext, initializerClassNames);
	}

	private static <C extends ConfigurableApplicationContext> void initialize(
			C applicationContext, String... initializerClassNames) {
		Log logger = LogFactory.getLog(AotApplicationContextInitializer.class);
		ClassLoader classLoader = applicationContext.getClassLoader();
		logger.debug("Initializing ApplicationContext with AOT");
		for (String initializerClassName : initializerClassNames) {
			logger.trace(LogMessage.format("Applying %s", initializerClassName));
			instantiateInitializer(initializerClassName, classLoader)
					.initialize(applicationContext);
		}
	}

	@SuppressWarnings("unchecked")
	static <C extends ConfigurableApplicationContext> ApplicationContextInitializer<C> instantiateInitializer(
			String initializerClassName, @Nullable ClassLoader classLoader) {
		try {
			Class<?> initializerClass = ClassUtils.resolveClassName(initializerClassName, classLoader);
			Assert.isAssignable(ApplicationContextInitializer.class, initializerClass);
			return (ApplicationContextInitializer<C>) BeanUtils.instantiateClass(initializerClass);
		}
		catch (BeanInstantiationException ex) {
			throw new IllegalArgumentException(
					"Failed to instantiate ApplicationContextInitializer: " + initializerClassName, ex);
		}
	}

}
