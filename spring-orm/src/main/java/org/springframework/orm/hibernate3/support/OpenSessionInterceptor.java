/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.orm.hibernate3.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Simple AOP Alliance {@link MethodInterceptor} implementation that binds a new
 * Hibernate {@link Session} for each method invocation, if none bound before.
 *
 * <p>This is a simple Hibernate Session scoping interceptor along the lines of
 * {@link OpenSessionInViewInterceptor}, just for use with AOP setup instead of
 * MVC setup. It opens a new {@link Session} with flush mode "MANUAL" since the
 * Session is only meant for reading, except when participating in a transaction.
 *
 * <p>Note: This can serve as a streamlined alternative to the outdated
 * {@link org.springframework.orm.hibernate3.HibernateInterceptor}, providing
 * plain Session binding without any automatic exception translation or the like.
 *
 * @author Juergen Hoeller
 * @since 4.0.2
 * @see OpenSessionInViewInterceptor
 * @see OpenSessionInViewFilter
 * @see org.springframework.orm.hibernate3.HibernateTransactionManager
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 * @see org.hibernate.SessionFactory#getCurrentSession()
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class OpenSessionInterceptor implements MethodInterceptor, InitializingBean {

	private SessionFactory sessionFactory;


	/**
	 * Set the Hibernate SessionFactory that should be used to create Hibernate Sessions.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Return the Hibernate SessionFactory that should be used to create Hibernate Sessions.
	 */
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("Property 'sessionFactory' is required");
		}
	}


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		SessionFactory sf = getSessionFactory();
		if (!TransactionSynchronizationManager.hasResource(sf)) {
			// New Session to be bound for the current method's scope...
			Session session = openSession();
			try {
				TransactionSynchronizationManager.bindResource(sf, new org.springframework.orm.hibernate3.SessionHolder(session));
				return invocation.proceed();
			}
			finally {
				org.springframework.orm.hibernate3.SessionFactoryUtils.closeSession(session);
				TransactionSynchronizationManager.unbindResource(sf);
			}
		}
		else {
			// Pre-bound Session found -> simply proceed.
			return invocation.proceed();
		}
	}

	/**
	 * Open a Session for the SessionFactory that this interceptor uses.
	 * <p>The default implementation delegates to the {@link SessionFactory#openSession}
	 * method and sets the {@link Session}'s flush mode to "MANUAL".
	 * @return the Session to use
	 * @throws DataAccessResourceFailureException if the Session could not be created
	 * @see org.hibernate.FlushMode#MANUAL
	 */
	protected Session openSession() throws DataAccessResourceFailureException {
		try {
			Session session = getSessionFactory().openSession();
			session.setFlushMode(FlushMode.MANUAL);
			return session;
		}
		catch (HibernateException ex) {
			throw new DataAccessResourceFailureException("Could not open Hibernate Session", ex);
		}
	}

}
