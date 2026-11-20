/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.util;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.NativeDetector;
import org.springframework.core.io.support.SpringFactoriesLoader.FailureHandler;

/**
 * Spring factories {@link FailureHandler} used within the <em>Spring TestContext
 * Framework</em>.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class TestContextFailureHandler implements FailureHandler {

	private final Log logger = LogFactory.getLog(TestContextSpringFactoriesUtils.class);

	@Override
	public void handleFailure(Class<?> factoryType, String factoryImplementationName, Throwable failure) {
		Throwable ex = (failure instanceof InvocationTargetException ite ? ite.getTargetException() : failure);
		if (ex instanceof ClassNotFoundException || ex instanceof NoClassDefFoundError) {
			if (logger.isDebugEnabled()) {
				logger.debug("""
						Skipping candidate %1$s [%2$s] due to a missing dependency. \
						Specify custom %1$s classes or make the default %1$s classes \
						and their required dependencies available. Offending class: %3$s"""
							.formatted(factoryType.getSimpleName(), factoryImplementationName, ex.getMessage()));
			}
		}
		else if (ex instanceof LinkageError) {
			if (logger.isDebugEnabled()) {
				logger.debug("""
						Could not load %1$s [%2$s]. Specify custom %1$s classes or make the default %1$s classes \
						available.""".formatted(factoryType.getSimpleName(), factoryImplementationName), ex);
			}
		}
		// Workaround for https://github.com/oracle/graal/issues/6691
		else if (NativeDetector.inNativeImage() && ex instanceof IllegalStateException) {
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping candidate %1$s [%2$s] due to an error when loading it in a native image."
						.formatted(factoryType.getSimpleName(), factoryImplementationName));
			}
		}
		else {
			if (ex instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (ex instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException(
				"Failed to load %s [%s]".formatted(factoryType.getSimpleName(), factoryImplementationName), ex);
		}
	}

}
