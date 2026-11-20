/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.concurrent;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;

import org.jspecify.annotations.Nullable;

import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ReflectionUtils;

/**
 * {@link Callable} adapter for an {@link ErrorHandler}.
 *
 * @author Juergen Hoeller
 * @since 6.2
 * @param <V> the value type
 */
class DelegatingErrorHandlingCallable<V> implements Callable<V> {

	private final Callable<V> delegate;

	private final ErrorHandler errorHandler;


	public DelegatingErrorHandlingCallable(Callable<V> delegate, @Nullable ErrorHandler errorHandler) {
		this.delegate = delegate;
		this.errorHandler = (errorHandler != null ? errorHandler :
				TaskUtils.getDefaultErrorHandler(false));
	}


	@Override
	public @Nullable V call() throws Exception {
		try {
			return this.delegate.call();
		}
		catch (Throwable ex) {
			try {
				this.errorHandler.handleError(ex);
			}
			catch (UndeclaredThrowableException exToPropagate) {
				ReflectionUtils.rethrowException(exToPropagate.getUndeclaredThrowable());
			}
			return null;
		}
	}

}
