/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.scheduling.concurrent;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;

import org.springframework.lang.Nullable;
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
	@Nullable
	public V call() throws Exception {
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
