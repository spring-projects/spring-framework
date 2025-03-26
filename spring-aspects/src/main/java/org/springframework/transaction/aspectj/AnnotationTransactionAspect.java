/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.transaction.aspectj;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.annotation.RequiredTypes;
import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Concrete AspectJ transaction aspect using Spring's
 * {@link org.springframework.transaction.annotation.Transactional} annotation.
 *
 * <p>When using this aspect, you <i>must</i> annotate the implementation class
 * (and/or methods within that class), <i>not</i> the interface (if any) that
 * the class implements. AspectJ follows Java's rule that annotations on
 * interfaces are <i>not</i> inherited.
 *
 * <p>An @Transactional annotation on a class specifies the default transaction
 * semantics for the execution of any <b>public</b> operation in the class.
 *
 * <p>An @Transactional annotation on a method within the class overrides the
 * default transaction semantics given by the class annotation (if present).
 * Any method may be annotated (regardless of visibility). Annotating
 * non-public methods directly is the only way to get transaction demarcation
 * for the execution of such operations.
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Adrian Colyer
 * @author Joshua Chen
 * @since 2.0
 * @see org.springframework.transaction.annotation.Transactional
 */
@Aspect
@RequiredTypes("org.springframework.transaction.annotation.Transactional")
public class AnnotationTransactionAspect extends TransactionAspectSupport implements DisposableBean {

	/**
	 * Construct the aspect using the default transaction metadata retrieval strategy.
	 */
	public AnnotationTransactionAspect() {
		new AnnotationTransactionAttributeSource(false);  // Use AnnotationTransactionAttributeSource
	}

	@Override
	public void destroy() {
		// An aspect is basically a singleton -> cleanup on destruction
		clearTransactionManagerCache();
	}

	/**
	 * Around advice: Intercepts transactional method execution.
	 * @param joinPoint proceedingJoinPoint context for the joinpoint
	 * @param txObject the object passed to the joinpoint, defined by the subclass
	 * @return the result of the target method execution
	 * @throws Throwable exceptions are rethrown via the Rethrower class
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	@Around(value = "transactionalMethodExecution(txObject)", argNames = "joinPoint,txObject")
	public Object around(final ProceedingJoinPoint joinPoint, final Object txObject) throws Throwable {
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature(); try {
			// Proceed with the method execution
			return invokeWithinTransaction(methodSignature.getMethod(), txObject.getClass(), joinPoint::proceed);
		}
		catch (RuntimeException | Error ex) {
			throw ex;  // Rethrow runtime exceptions or errors
		}
		catch (Throwable thr) {
			Rethrower.rethrow(thr);  // Rethrow checked exceptions as runtime exceptions
			throw new IllegalStateException("Should never get here", thr);
		}
	}

	/**
	 * Matches the execution of any public method in a type with the Transactional
	 * annotation, or any subtype of a type with the Transactional annotation.
	 */
	@Pointcut("execution(public * ((@org.springframework.transaction.annotation.Transactional *)+).*(..)) && within(@org.springframework.transaction.annotation.Transactional *)")
	private void executionOfAnyPublicMethodInAtTransactionalType() {
	}

	/**
	 * Matches the execution of any method with the Transactional annotation.
	 */
	@Pointcut("execution(@org.springframework.transaction.annotation.Transactional * *(..))")
	private void executionOfTransactionalMethod() {
	}

	/**
	 * Definition of pointcut from super aspect - matched join points
	 * will have Spring transaction management applied.
	 */
	@Pointcut(value = "(executionOfAnyPublicMethodInAtTransactionalType() || executionOfTransactionalMethod()) && this(txObject)", argNames = "txObject")
	public void transactionalMethodExecution(Object txObject) {
	}

	/**
	 * Helper class to rethrow checked exceptions as runtime exceptions (workaround
	 * for AspectJ's restriction on checked exceptions in around advice).
	 */
	private static class Rethrower {
		public static void rethrow(final Throwable exception) {
			class CheckedExceptionRethrower<T extends Throwable> {
				@SuppressWarnings("unchecked")
				private void rethrow(Throwable exception) throws T {
					throw (T) exception;  // Rethrow the exception as unchecked
				}
			} new CheckedExceptionRethrower<RuntimeException>().rethrow(exception);
		}
	}
}
