/*
 * Copyright 2002-2010 the original author or authors.
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

import java.lang.reflect.Method;

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
 * @since 2.0
 */
public abstract aspect AbstractTransactionAspect extends TransactionAspectSupport {

	/**
	 * Construct object using the given transaction metadata retrieval strategy.
	 * @param tas TransactionAttributeSource implementation, retrieving Spring
	 * transaction metadata for each joinpoint. Write the subclass to pass in null
	 * if it's intended to be configured by Setter Injection.
	 */
	protected AbstractTransactionAspect(TransactionAttributeSource tas) {
		setTransactionAttributeSource(tas);
	}

	@SuppressAjWarnings("adviceDidNotMatch")
	before(Object txObject) : transactionalMethodExecution(txObject) {
		MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
		Method method = methodSignature.getMethod();
		TransactionInfo txInfo = createTransactionIfNecessary(method, txObject.getClass());
	}

	@SuppressAjWarnings("adviceDidNotMatch")
	after(Object txObject) throwing(Throwable t) : transactionalMethodExecution(txObject) {
		try {
			completeTransactionAfterThrowing(TransactionAspectSupport.currentTransactionInfo(), t);
		}
		catch (Throwable t2) {
			logger.error("Failed to close transaction after throwing in a transactional method", t2);
		}
	}

	@SuppressAjWarnings("adviceDidNotMatch")
	after(Object txObject) returning() : transactionalMethodExecution(txObject) {
		commitTransactionAfterReturning(TransactionAspectSupport.currentTransactionInfo());
	}

	@SuppressAjWarnings("adviceDidNotMatch")
	after(Object txObject) : transactionalMethodExecution(txObject) {
		cleanupTransactionInfo(TransactionAspectSupport.currentTransactionInfo());
	}

	/**
	 * Concrete subaspects must implement this pointcut, to identify
	 * transactional methods. For each selected joinpoint, TransactionMetadata
	 * will be retrieved using Spring's TransactionAttributeSource interface.
	 */
	protected abstract pointcut transactionalMethodExecution(Object txObject);

}
