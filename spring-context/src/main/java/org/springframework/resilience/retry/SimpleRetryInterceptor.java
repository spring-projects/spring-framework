/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.resilience.retry;

import java.lang.reflect.Method;

/**
 * A simple concrete retry interceptor based on a given {@link MethodRetrySpec}.
 *
 * @author Juergen Hoeller
 * @since 7.0
 */
public class SimpleRetryInterceptor extends AbstractRetryInterceptor {

	private final MethodRetrySpec retrySpec;


	/**
	 * Create a {@code SimpleRetryInterceptor} for the given {@link MethodRetrySpec}.
	 * @param retrySpec the specification to use for all method invocations
	 */
	public SimpleRetryInterceptor(MethodRetrySpec retrySpec) {
		this.retrySpec = retrySpec;
	}

	@Override
	protected MethodRetrySpec getRetrySpec(Method method, Class<?> targetClass) {
		return this.retrySpec;
	}

}
