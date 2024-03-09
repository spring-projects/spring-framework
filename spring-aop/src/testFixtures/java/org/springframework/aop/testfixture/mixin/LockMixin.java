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

package org.springframework.aop.testfixture.mixin;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.DelegatingIntroductionInterceptor;

/**
 * Mixin to provide stateful locking functionality.
 * Test/demonstration of AOP mixin support rather than a
 * useful interceptor in its own right.
 *
 * @author Rod Johnson
 * @since 10.07.2003
 */
@SuppressWarnings("serial")
public class LockMixin extends DelegatingIntroductionInterceptor implements Lockable {

	/** This field demonstrates additional state in the mixin */
	private boolean locked;

	@Override
	public void lock() {
		this.locked = true;
	}

	@Override
	public void unlock() {
		this.locked = false;
	}

	@Override
	public boolean locked() {
		return this.locked;
	}

	/**
	 * Note that we need to override around advice.
	 * If the method is a setter, and we're locked, prevent execution.
	 * Otherwise, let super.invoke() handle it.
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (locked() && invocation.getMethod().getName().indexOf("set") == 0) {
			throw new LockedException();
		}
		return super.invoke(invocation);
	}

}
