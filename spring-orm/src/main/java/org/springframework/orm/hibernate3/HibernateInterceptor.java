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

package org.springframework.orm.hibernate3;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This interceptor binds a new Hibernate Session to the thread before a method
 * call, closing and removing it afterwards in case of any method outcome.
 * If there already is a pre-bound Session (e.g. from HibernateTransactionManager,
 * or from a surrounding Hibernate-intercepted method), the interceptor simply
 * participates in it.
 *
 * <p>Application code must retrieve a Hibernate Session via the
 * {@code SessionFactoryUtils.getSession} method or - preferably -
 * Hibernate's own {@code SessionFactory.getCurrentSession()} method, to be
 * able to detect a thread-bound Session. Typically, the code will look like as follows:
 *
 * <pre class="code">
 * public void doSomeDataAccessAction() {
 *   Session session = this.sessionFactory.getCurrentSession();
 *   ...
 *   // No need to close the Session or translate exceptions!
 * }</pre>
 *
 * Note that this interceptor automatically translates HibernateExceptions,
 * via delegating to the {@code SessionFactoryUtils.convertHibernateAccessException}
 * method that converts them to exceptions that are compatible with the
 * {@code org.springframework.dao} exception hierarchy (like HibernateTemplate does).
 * This can be turned off if the raw exceptions are preferred.
 *
 * <p>This class can be considered a declarative alternative to HibernateTemplate's
 * callback approach. The advantages are:
 * <ul>
 * <li>no anonymous classes necessary for callback implementations;
 * <li>the possibility to throw any application exceptions from within data access code.
 * </ul>
 *
 * <p>The drawback is the dependency on interceptor configuration. However, note
 * that this interceptor is usually <i>not</i> necessary in scenarios where the
 * data access code always executes within transactions. A transaction will always
 * have a thread-bound Session in the first place, so adding this interceptor to the
 * configuration just adds value when fine-tuning Session settings like the flush mode
 * - or when relying on exception translation.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.hibernate.SessionFactory#getCurrentSession()
 * @see HibernateTransactionManager
 * @see HibernateTemplate
 */
public class HibernateInterceptor extends HibernateAccessor implements MethodInterceptor {

	private boolean exceptionConversionEnabled = true;


	/**
	 * Set whether to convert any HibernateException raised to a Spring DataAccessException,
	 * compatible with the {@code org.springframework.dao} exception hierarchy.
	 * <p>Default is "true". Turn this flag off to let the caller receive raw exceptions
	 * as-is, without any wrapping.
	 * @see org.springframework.dao.DataAccessException
	 */
	public void setExceptionConversionEnabled(boolean exceptionConversionEnabled) {
		this.exceptionConversionEnabled = exceptionConversionEnabled;
	}


	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		Session session = getSession();
		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(getSessionFactory());

		boolean existingTransaction = (sessionHolder != null && sessionHolder.containsSession(session));
		if (existingTransaction) {
			logger.debug("Found thread-bound Session for HibernateInterceptor");
		}
		else {
			if (sessionHolder != null) {
				sessionHolder.addSession(session);
			}
			else {
				TransactionSynchronizationManager.bindResource(getSessionFactory(), new SessionHolder(session));
			}
		}

		FlushMode previousFlushMode = null;
		try {
			previousFlushMode = applyFlushMode(session, existingTransaction);
			enableFilters(session);
			Object retVal = methodInvocation.proceed();
			flushIfNecessary(session, existingTransaction);
			return retVal;
		}
		catch (HibernateException ex) {
			if (this.exceptionConversionEnabled) {
				throw convertHibernateAccessException(ex);
			}
			else {
				throw ex;
			}
		}
		finally {
			if (existingTransaction) {
				logger.debug("Not closing pre-bound Hibernate Session after HibernateInterceptor");
				disableFilters(session);
				if (previousFlushMode != null) {
					session.setFlushMode(previousFlushMode);
				}
			}
			else {
				SessionFactoryUtils.closeSessionOrRegisterDeferredClose(session, getSessionFactory());
				if (sessionHolder == null || sessionHolder.doesNotHoldNonDefaultSession()) {
					TransactionSynchronizationManager.unbindResource(getSessionFactory());
				}
			}
		}
	}

	/**
	 * Return a Session for use by this interceptor.
	 * @see SessionFactoryUtils#getSession
	 */
	protected Session getSession() {
		return SessionFactoryUtils.getSession(
				getSessionFactory(), getEntityInterceptor(), getJdbcExceptionTranslator());
	}

}
