/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Default implementation of the {@link MethodInvoker} API.
 *
 * <p>This implementation never provides arguments to a {@link Method}.
 *
 * @author Sam Brannen
 * @since 6.1
 */
final class DefaultMethodInvoker implements MethodInvoker {

	private static final Log logger = LogFactory.getLog(DefaultMethodInvoker.class);


	@Override
	public @Nullable Object invoke(Method method, @Nullable Object target) throws Exception {
		Assert.notNull(method, "Method must not be null");

		try {
			ReflectionUtils.makeAccessible(method);
			return method.invoke(target);
		}
		catch (InvocationTargetException ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Exception encountered while invoking method [%s] on target [%s]"
						.formatted(method, target), ex.getTargetException());
			}
			ReflectionUtils.rethrowException(ex.getTargetException());
			// appease the compiler
			return null;
		}
	}

}
