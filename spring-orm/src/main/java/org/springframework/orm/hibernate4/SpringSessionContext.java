/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.orm.hibernate4;

import javax.transaction.TransactionManager;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.context.spi.CurrentSessionContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.jta.platform.spi.JtaPlatform;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Implementation of Hibernate 3.1's CurrentSessionContext interface
 * that delegates to Spring's SessionFactoryUtils for providing a
 * Spring-managed current Session.
 *
 * <p>This CurrentSessionContext implementation can also be specified in custom
 * SessionFactory setup through the "hibernate.current_session_context_class"
 * property, with the fully qualified name of this class as value.
 *
 * @author Juergen Hoeller
 * @since 3.1
 */
@SuppressWarnings("serial")
public class SpringSessionContext implements CurrentSessionContext {

	private final SessionFactoryImplementor sessionFactory;

	private final CurrentSessionContext jtaSessionContext;


	/**
	 * Create a new SpringSessionContext for the given Hibernate SessionFactory.
	 * @param sessionFactory the SessionFactory to provide current Sessions for
	 */
	public SpringSessionContext(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		JtaPlatform jtaPlatform = sessionFactory.getServiceRegistry().getService(JtaPlatform.class);
		TransactionManager transactionManager = jtaPlatform.retrieveTransactionManager();
		this.jtaSessionContext = (transactionManager != null ? new SpringJtaSessionContext(sessionFactory) : null);
	}


	/**
	 * Retrieve the Spring-managed Session for the current thread, if any.
	 */
	public Session currentSession() throws HibernateException {
		Object value = TransactionSynchronizationManager.getResource(this.sessionFactory);
		if (value instanceof Session) {
			return (Session) value;
		}
		else if (value instanceof SessionHolder) {
			SessionHolder sessionHolder = (SessionHolder) value;
			Session session = sessionHolder.getSession();
			if (TransactionSynchronizationManager.isSynchronizationActive() &&
					!sessionHolder.isSynchronizedWithTransaction()) {
				TransactionSynchronizationManager.registerSynchronization(
						new SpringSessionSynchronization(sessionHolder, this.sessionFactory));
				sessionHolder.setSynchronizedWithTransaction(true);
				// Switch to FlushMode.AUTO, as we have to assume a thread-bound Session
				// with FlushMode.MANUAL, which needs to allow flushing within the transaction.
				FlushMode flushMode = session.getFlushMode();
				if (FlushMode.isManualFlushMode(flushMode) &&
						!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
					session.setFlushMode(FlushMode.AUTO);
					sessionHolder.setPreviousFlushMode(flushMode);
				}
			}
			return session;
		}
		else if (this.jtaSessionContext != null) {
			Session session = this.jtaSessionContext.currentSession();
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				TransactionSynchronizationManager.registerSynchronization(new SpringFlushSynchronization(session));
			}
			return session;
		}
		else {
			throw new HibernateException("No Session found for current thread");
		}
	}

}
