
/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.tests.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Trivial interceptor that can be introduced in a chain to display it.
 *
 * @author Rod Johnson
 */
public class NopInterceptor implements MethodInterceptor {

	private int count;

	/**
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(MethodInvocation)
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		increment();
		return invocation.proceed();
	}

	public int getCount() {
		return this.count;
	}

	protected void increment() {
		++count;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof NopInterceptor)) {
			return false;
		}
		if (this == other) {
			return true;
		}
		return this.count == ((NopInterceptor) other).count;
	}

}
