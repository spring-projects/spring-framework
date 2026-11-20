/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.FailureHandler;

import static org.springframework.core.io.support.SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION;

/**
 * Collection of utilities for working with {@link SpringFactoriesLoader} within
 * the <em>Spring TestContext Framework</em>.
 *
 * <p>Primarily intended for use within the TestContext framework.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public abstract class TestContextSpringFactoriesUtils {

	private static final Log logger = LogFactory.getLog(TestContextSpringFactoriesUtils.class);


	private TestContextSpringFactoriesUtils() {
		// no-op
	}


	/**
	 * Load factory implementations of the given type via the
	 * {@link SpringFactoriesLoader} mechanism.
	 * <p>This method utilizes a custom {@link FailureHandler} and DEBUG/TRACE logging
	 * that are specific to the needs of the <em>Spring TestContext Framework</em>.
	 * <p>Specifically, this method looks up and instantiates all {@code factoryType}
	 * entries configured in all {@code META-INF/spring.factories} files on the classpath.
	 * <p>If a particular factory implementation cannot be loaded due to a {@link LinkageError}
	 * or {@link ClassNotFoundException}, a {@code DEBUG} message will be logged,
	 * but the associated exception will not be rethrown. A {@link RuntimeException}
	 * or any other {@link Error} will be rethrown. Any other exception will be
	 * thrown wrapped in an {@link IllegalStateException}.
	 * @param <T> the factory type
	 * @param factoryType the interface or abstract class representing the factory
	 * @return an unmodifiable list of factory implementations
	 * @see SpringFactoriesLoader#forDefaultResourceLocation(ClassLoader)
	 * @see SpringFactoriesLoader#load(Class, org.springframework.core.io.support.SpringFactoriesLoader.FailureHandler)
	 */
	public static <T> List<T> loadFactoryImplementations(Class<T> factoryType) {
		SpringFactoriesLoader loader = SpringFactoriesLoader.forDefaultResourceLocation(
			TestContextSpringFactoriesUtils.class.getClassLoader());
		List<T> implementations = loader.load(factoryType, new TestContextFailureHandler());
		if (logger.isTraceEnabled()) {
			logger.trace("Loaded %s implementations from location [%s]: %s"
					.formatted(factoryType.getSimpleName(), FACTORIES_RESOURCE_LOCATION, classNames(implementations)));
		}
		return Collections.unmodifiableList(implementations);
	}

	private static List<String> classNames(Collection<?> components) {
		return components.stream().map(Object::getClass).map(Class::getName).toList();
	}

}
