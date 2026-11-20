/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.hibernate;

import org.hibernate.Session;
import org.jspecify.annotations.Nullable;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Simple synchronization adapter that propagates a {@code flush()} call
 * to the underlying Hibernate Session. Used in combination with JTA.
 *
 * @author Juergen Hoeller
 * @since 7.0
 */
class SpringFlushSynchronization implements TransactionSynchronization {

	static final HibernateExceptionTranslator exceptionTranslator = new HibernateExceptionTranslator();

	private final Session session;


	public SpringFlushSynchronization(Session session) {
		this.session = session;
	}


	@Override
	public void flush() {
		try {
			this.session.flush();
		}
		catch (RuntimeException ex) {
			throw DataAccessUtils.translateIfNecessary(ex, exceptionTranslator);
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof SpringFlushSynchronization that && this.session == that.session));
	}

	@Override
	public int hashCode() {
		return this.session.hashCode();
	}

}
