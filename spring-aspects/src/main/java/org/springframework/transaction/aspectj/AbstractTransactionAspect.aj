/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.transaction.aspectj;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

/**
 * Abstract superaspect for AspectJ transaction aspects. Concrete
 * subaspects will implement the <code>transactionalMethodExecution()</code>
 * pointcut using a strategy such as Java 5 annotations.
 *
 * <p>Suitable for use inside or outside the Spring IoC container.
 * Set the "transactionManager" property appropriately, allowing
 * use of any transaction implementation supported by Spring.
 *
 * <p><b>NB:</b> If a method implements an interface that is itself
 * transactionally annotated, the relevant Spring transaction attribute
 * will <i>not</i> be resolved. This behavior will vary from that of Spring AOP
 * if proxying an interface (but not when proxying a class). We recommend that
 * transaction annotations should be added to classes, rather than business
 * interfaces, as they are an implementation detail rather than a contract
 * specification validation.
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract aspect AbstractTransactionAspect extends TransactionAspectSupport {

	/**
	 * Construct the aspect using the given transaction metadata retrieval strategy.
	 * @param tas TransactionAttributeSource implementation, retrieving Spring
	 * transaction metadata for each joinpoint. Implement the subclass to pass in
	 * {@code null} if it is intended to be configured through Setter Injection.
	 */
	protected AbstractTransactionAspect(TransactionAttributeSource tas) {
		setTransactionAttributeSource(tas);
	}

	@SuppressAjWarnings("adviceDidNotMatch")
	Object around(final Object txObject): transactionalMethodExecution(txObject) {
		MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
		// Adapt to TransactionAspectSupport's invokeWithinTransaction...
		try {
			return invokeWithinTransaction(methodSignature.getMethod(), txObject.getClass(), new InvocationCallback() {
				public Object proceedWithInvocation() throws Throwable {
					return proceed(txObject);
				}
			});
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Error err) {
			throw err;
		}
		catch (Throwable thr) {
			Rethrower.rethrow(thr);
			throw new IllegalStateException("Should never get here", thr);
		}
	}

	/**
	 * Concrete subaspects must implement this pointcut, to identify
	 * transactional methods. For each selected joinpoint, TransactionMetadata
	 * will be retrieved using Spring's TransactionAttributeSource interface.
	 */
	protected abstract pointcut transactionalMethodExecution(Object txObject);


	/**
	 * Ugly but safe workaround: We need to be able to propagate checked exceptions,
	 * despite AspectJ around advice supporting specifically declared exceptions only.
	 */
	private static class Rethrower {

		public static void rethrow(final Throwable exception) {
			class CheckedExceptionRethrower<T extends Throwable> {
				@SuppressWarnings("unchecked")
				private void rethrow(Throwable exception) throws T {
					throw (T) exception;
				}
			}
			new CheckedExceptionRethrower<RuntimeException>().rethrow(exception);
		}
	}

}
