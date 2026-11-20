/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.hibernate;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.context.internal.JTASessionContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Spring-specific subclass of Hibernate's JTASessionContext,
 * setting {@code FlushMode.MANUAL} for read-only transactions.
 *
 * @author Juergen Hoeller
 * @since 7.0
 */
@SuppressWarnings("serial")
class SpringJtaSessionContext extends JTASessionContext {

	public SpringJtaSessionContext(SessionFactoryImplementor factory) {
		super(factory);
	}

	@Override
	protected Session buildOrObtainSession() {
		Session session = super.buildOrObtainSession();
		if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			session.setHibernateFlushMode(FlushMode.MANUAL);
		}
		return session;
	}

}
