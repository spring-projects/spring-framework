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

package org.springframework.orm.toplink;

import oracle.toplink.exceptions.TopLinkException;
import oracle.toplink.sessions.Session;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This interceptor binds a new TopLink Session to the thread before a method
 * call, closing and removing it afterwards in case of any method outcome.
 * If there already is a pre-bound Session (e.g. from TopLinkTransactionManager,
 * or from a surrounding TopLink-intercepted method), the interceptor simply
 * takes part in it.
 *
 * <p>Application code must retrieve a TopLink Session via the
 * <code>SessionFactoryUtils.getSession</code> method or - preferably -
 * TopLink's own <code>Session.getActiveSession()</code> method, to be able to
 * detect a thread-bound Session. Typically, the code will look like as follows:
 *
 * <pre>
 * public void doSomeDataAccessAction() {
 *   Session session = this.serverSession.getActiveSession();
 *   ...
 * }</pre>
 *
 * Note that this interceptor automatically translates TopLinkExceptions,
 * via delegating to the <code>SessionFactoryUtils.convertTopLikAccessException</code>
 * method that converts them to exceptions that are compatible with the
 * <code>org.springframework.dao</code> exception hierarchy (like TopLinkTemplate does).
 * This can be turned off if the raw exceptions are preferred.
 *
 * <p>This class can be considered a declarative alternative to TopLinkTemplate's
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
 * configuration just adds value when potentially executing outside of transactions
 * and/or when relying on exception translation.
 *
 * @author Juergen Hoeller
 * @since 1.2
 */
public class TopLinkInterceptor extends TopLinkAccessor implements MethodInterceptor {

	private boolean exceptionConversionEnabled = true;


	/**
	 * Set whether to convert any TopLinkException raised to a Spring DataAccessException,
	 * compatible with the <code>org.springframework.dao</code> exception hierarchy.
	 * <p>Default is "true". Turn this flag off to let the caller receive raw exceptions
	 * as-is, without any wrapping.
	 * @see org.springframework.dao.DataAccessException
	 */
	public void setExceptionConversionEnabled(boolean exceptionConversionEnabled) {
		this.exceptionConversionEnabled = exceptionConversionEnabled;
	}


	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		boolean existingTransaction = false;
		Session session = SessionFactoryUtils.getSession(getSessionFactory(), true);
		if (TransactionSynchronizationManager.hasResource(getSessionFactory())) {
			logger.debug("Found thread-bound Session for TopLink interceptor");
			existingTransaction = true;
		}
		else {
			logger.debug("Using new Session for TopLink interceptor");
			TransactionSynchronizationManager.bindResource(getSessionFactory(), new SessionHolder(session));
		}
		try {
			return methodInvocation.proceed();
		}
		catch (TopLinkException ex) {
			if (this.exceptionConversionEnabled) {
				throw convertTopLinkAccessException(ex);
			}
			else {
				throw ex;
			}
		}
		finally {
			if (existingTransaction) {
				logger.debug("Not closing pre-bound TopLink Session after interceptor");
			}
			else {
				TransactionSynchronizationManager.unbindResource(getSessionFactory());
				SessionFactoryUtils.releaseSession(session, getSessionFactory());
			}
		}
	}

}
