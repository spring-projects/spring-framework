/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.annotation;

import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

/**
 * A convenience {@link AsyncConfigurer} that implements all methods
 * so that the defaults are used. Provides a backward compatible alternative
 * of implementing {@link AsyncConfigurer} directly.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @deprecated as of 6.0 in favor of implementing {@link AsyncConfigurer} directly
 */
@Deprecated(since = "6.0")
public class AsyncConfigurerSupport implements AsyncConfigurer {

	@Override
	public @Nullable Executor getAsyncExecutor() {
		return null;
	}

	@Override
	public @Nullable AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return null;
	}

}
