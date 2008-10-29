/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.aop.framework.autoproxy;

import java.lang.reflect.Method;

import org.springframework.aop.framework.CountingBeforeAdvice;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Before advisor that allow us to manipulate ordering to check
 * that superclass sorting works correctly.
 *
 * <p>It doesn't actually <i>do</i> anything except count
 * method invocations and check for presence of transaction context.
 * <br>Matches setters.
 *
 * @author Rod Johnson
 */
public class OrderedTxCheckAdvisor extends StaticMethodMatcherPointcutAdvisor implements InitializingBean {

	/**
	 * Should we insist on the presence of a transaction attribute or refuse to accept one?
	 */
	private boolean requireTransactionContext = false;


	public void setRequireTransactionContext(boolean requireTransactionContext) {
		this.requireTransactionContext = requireTransactionContext;
	}

	public boolean isRequireTransactionContext() {
		return requireTransactionContext;
	}


	public CountingBeforeAdvice getCountingBeforeAdvice() {
		return (CountingBeforeAdvice) getAdvice();
	}

	public void afterPropertiesSet() throws Exception {
		setAdvice(new TxCountingBeforeAdvice());
	}

	public boolean matches(Method method, Class targetClass) {
		return method.getName().startsWith("setAge");
	}


	private class TxCountingBeforeAdvice extends CountingBeforeAdvice {

		public void before(Method method, Object[] args, Object target) throws Throwable {
			// do transaction checks
			if (requireTransactionContext) {
				TransactionInterceptor.currentTransactionStatus();
			}
			else {
				try {
					TransactionInterceptor.currentTransactionStatus();
					throw new RuntimeException("Shouldn't have a transaction");
				}
				catch (NoTransactionException ex) {
					// this is Ok
				}
			}
			super.before(method, args, target);
		}
	}

}
