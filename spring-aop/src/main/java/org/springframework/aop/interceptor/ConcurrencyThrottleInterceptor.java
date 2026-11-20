/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.interceptor;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import org.springframework.util.ConcurrencyThrottleSupport;

/**
 * Interceptor that throttles concurrent access, blocking invocations
 * if a specified concurrency limit is reached.
 *
 * <p>Can be applied to methods of local services that involve heavy use
 * of system resources, in a scenario where it is more efficient to
 * throttle concurrency for a specific service rather than restrict
 * the entire thread pool (for example, the web container's thread pool).
 *
 * <p>The default concurrency limit of this interceptor is 1.
 * Specify the "concurrencyLimit" bean property to change this value.
 *
 * @author Juergen Hoeller
 * @since 11.02.2004
 * @see #setConcurrencyLimit
 */
@SuppressWarnings("serial")
public class ConcurrencyThrottleInterceptor extends ConcurrencyThrottleSupport
		implements MethodInterceptor, Serializable {

	/**
	 * Create a default {@code ConcurrencyThrottleInterceptor}
	 * with concurrency limit 1.
	 */
	public ConcurrencyThrottleInterceptor() {
		this(1);
	}

	/**
	 * Create a {@code ConcurrencyThrottleInterceptor}
	 * with the given concurrency limit.
	 * @since 7.0
	 */
	public ConcurrencyThrottleInterceptor(int concurrencyLimit) {
		setConcurrencyLimit(concurrencyLimit);
	}


	@Override
	public @Nullable Object invoke(MethodInvocation methodInvocation) throws Throwable {
		beforeAccess();
		try {
			return methodInvocation.proceed();
		}
		finally {
			afterAccess();
		}
	}

}
