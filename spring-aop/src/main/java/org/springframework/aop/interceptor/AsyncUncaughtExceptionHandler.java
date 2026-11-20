/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.interceptor;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

/**
 * A strategy for handling uncaught exceptions thrown from asynchronous methods.
 *
 * <p>An asynchronous method usually returns a {@link java.util.concurrent.Future}
 * instance that gives access to the underlying exception. When the method does
 * not provide that return type, this handler can be used to manage such
 * uncaught exceptions.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@FunctionalInterface
public interface AsyncUncaughtExceptionHandler {

	/**
	 * Handle the given uncaught exception thrown from an asynchronous method.
	 * @param ex the exception thrown from the asynchronous method
	 * @param method the asynchronous method
	 * @param params the parameters used to invoke the method
	 */
	void handleUncaughtException(Throwable ex, Method method, @Nullable Object... params);

}
