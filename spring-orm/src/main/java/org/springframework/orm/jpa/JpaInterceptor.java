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

package org.springframework.orm.jpa;

import javax.persistence.EntityManager;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This interceptor binds a new JPA EntityManager to the thread before a method
 * call, closing and removing it afterwards in case of any method outcome.
 * If there already is a pre-bound EntityManager (e.g. from JpaTransactionManager,
 * or from a surrounding JPA-intercepted method), the interceptor simply participates in it.
 *
 * <p>Application code must retrieve a JPA EntityManager via the
 * {@code EntityManagerFactoryUtils.getEntityManager} method or - preferably -
 * via a shared {@code EntityManager} reference, to be able to detect a
 * thread-bound EntityManager. Typically, the code will look like as follows:
 *
 * <pre class="code">
 * public void doSomeDataAccessAction() {
 *   this.entityManager...
 * }</pre>
 *
 * <p>Note that this interceptor automatically translates PersistenceExceptions,
 * via delegating to the {@code EntityManagerFactoryUtils.convertJpaAccessException}
 * method that converts them to exceptions that are compatible with the
 * {@code org.springframework.dao} exception hierarchy (like JpaTemplate does).
 *
 * <p>This class can be considered a declarative alternative to JpaTemplate's
 * callback approach. The advantages are:
 * <ul>
 * <li>no anonymous classes necessary for callback implementations;
 * <li>the possibility to throw any application exceptions from within data access code.
 * </ul>
 *
 * <p>The drawback is the dependency on interceptor configuration. However, note
 * that this interceptor is usually <i>not</i> necessary in scenarios where the
 * data access code always executes within transactions. A transaction will always
 * have a thread-bound EntityManager in the first place, so adding this interceptor
 * to the configuration just adds value when fine-tuning EntityManager settings
 * like the flush mode - or when relying on exception translation.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see JpaTransactionManager
 * @see JpaTemplate
 * @deprecated as of Spring 3.1, in favor of native EntityManager usage
 * (typically obtained through {@code @PersistenceContext}) and
 * AOP-driven exception translation through
 * {@link org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor}
 */
@Deprecated
public class JpaInterceptor extends JpaAccessor implements MethodInterceptor {

	private boolean exceptionConversionEnabled = true;


	/**
	 * Set whether to convert any PersistenceException raised to a Spring DataAccessException,
	 * compatible with the {@code org.springframework.dao} exception hierarchy.
	 * <p>Default is "true". Turn this flag off to let the caller receive raw exceptions
	 * as-is, without any wrapping.
	 * @see org.springframework.dao.DataAccessException
	 */
	public void setExceptionConversionEnabled(boolean exceptionConversionEnabled) {
		this.exceptionConversionEnabled = exceptionConversionEnabled;
	}


	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		// Determine current EntityManager: either the transactional one
		// managed by the factory or a temporary one for the given invocation.
		EntityManager em = getTransactionalEntityManager();
		boolean isNewEm = false;
		if (em == null) {
			logger.debug("Creating new EntityManager for JpaInterceptor invocation");
			em = createEntityManager();
			isNewEm = true;
			TransactionSynchronizationManager.bindResource(getEntityManagerFactory(), new EntityManagerHolder(em));
		}

		try {
			Object retVal = methodInvocation.proceed();
			flushIfNecessary(em, !isNewEm);
			return retVal;
		}
		catch (RuntimeException rawException) {
			if (this.exceptionConversionEnabled) {
				// Translation enabled. Translate if we understand the exception.
				throw translateIfNecessary(rawException);
			}
			else {
				// Translation not enabled. Don't try to translate.
				throw rawException;
			}
		}
		finally {
			if (isNewEm) {
				TransactionSynchronizationManager.unbindResource(getEntityManagerFactory());
				EntityManagerFactoryUtils.closeEntityManager(em);
			}
		}
	}

}
