
package org.springframework.aop.aspectj;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author robh
 */
class CallCountingInterceptor implements MethodInterceptor {

	private int count;

	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		count++;
		return methodInvocation.proceed();
	}

	public int getCount() {
		return count;
	}

	public void reset() {
		this.count = 0;
	}
}
