/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

/**
 * Trivial interceptor that can be introduced in a chain to display it.
 *
 * @author Rod Johnson
 */
public class NopInterceptor implements MethodInterceptor {

	private int count;


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		increment();
		return invocation.proceed();
	}

	protected void increment() {
		this.count++;
	}

	public int getCount() {
		return this.count;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof NopInterceptor that && this.count == that.count));
	}

	@Override
	public int hashCode() {
		return NopInterceptor.class.hashCode();
	}

}
