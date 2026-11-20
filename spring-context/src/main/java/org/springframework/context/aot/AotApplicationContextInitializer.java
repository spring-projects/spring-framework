/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.aot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.log.LogMessage;
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
			instantiateInitializer(initializerClassName, classLoader).initialize(applicationContext);
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
