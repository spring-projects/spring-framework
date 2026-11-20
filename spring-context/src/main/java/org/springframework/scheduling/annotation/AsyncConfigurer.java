/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.annotation;

import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

/**
 * Interface to be implemented for customizing the {@link Executor} instance used when
 * processing async method invocations or the {@link AsyncUncaughtExceptionHandler}
 * instance used to process exceptions thrown from async methods with a {@code void}
 * return type.
 *
 * <p>Typically implemented by @{@link org.springframework.context.annotation.Configuration
 * Configuration} classes annotated with @{@link EnableAsync}.
 * See the @{@link EnableAsync} javadoc for usage examples.
 *
 * <p><b>NOTE: An {@code AsyncConfigurer} will get initialized early.</b>
 * Do not inject common dependencies into autowired fields directly; instead, consider
 * declaring a lazy {@link org.springframework.beans.factory.ObjectProvider} for those.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 * @see AbstractAsyncConfiguration
 * @see EnableAsync
 */
public interface AsyncConfigurer {

	/**
	 * The {@link Executor} instance to be used when processing async
	 * method invocations.
	 */
	default @Nullable Executor getAsyncExecutor() {
		return null;
	}

	/**
	 * The {@link AsyncUncaughtExceptionHandler} instance to be used
	 * when an exception is thrown during an asynchronous method execution
	 * with {@code void} return type.
	 */
	default @Nullable AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return null;
	}

}
