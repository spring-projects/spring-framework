/*
 * Copyright 2002-2009 the original author or authors.
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

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionImplementor;

import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Callback for resource cleanup at the end of a Spring-managed JTA transaction,
 * that is, when participating in a JtaTransactionManager transaction.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see SessionFactoryUtils
 * @see org.springframework.transaction.jta.JtaTransactionManager
 */
class SpringSessionSynchronization implements TransactionSynchronization, Ordered {

	private final SessionHolder sessionHolder;

	private final SessionFactory sessionFactory;

	private final SQLExceptionTranslator jdbcExceptionTranslator;

	private final boolean newSession;

	/**
	 * Whether Hibernate has a looked-up JTA TransactionManager that it will
	 * automatically register CacheSynchronizations with on Session connect.
	 */
	private boolean hibernateTransactionCompletion = false;

	private Transaction jtaTransaction;

	private boolean holderActive = true;


	public SpringSessionSynchronization(
			SessionHolder sessionHolder, SessionFactory sessionFactory,
			SQLExceptionTranslator jdbcExceptionTranslator, boolean newSession) {

		this.sessionHolder = sessionHolder;
		this.sessionFactory = sessionFactory;
		this.jdbcExceptionTranslator = jdbcExceptionTranslator;
		this.newSession = newSession;

		// Check whether the SessionFactory has a JTA TransactionManager.
		TransactionManager jtaTm =
				SessionFactoryUtils.getJtaTransactionManager(sessionFactory, sessionHolder.getAnySession());
		if (jtaTm != null) {
			this.hibernateTransactionCompletion = true;
			// Fetch current JTA Transaction object
			// (just necessary for JTA transaction suspension, with an individual
			// Hibernate Session per currently active/suspended transaction).
			try {
				this.jtaTransaction = jtaTm.getTransaction();
			}
			catch (SystemException ex) {
				throw new DataAccessResourceFailureException("Could not access JTA transaction", ex);
			}
		}
	}

	/**
	 * Check whether there is a Hibernate Session for the current JTA
	 * transaction. Else, fall back to the default thread-bound Session.
	 */
	private Session getCurrentSession() {
		Session session = null;
		if (this.jtaTransaction != null) {
			session = this.sessionHolder.getSession(this.jtaTransaction);
		}
		if (session == null) {
			session = this.sessionHolder.getSession();
		}
		return session;
	}


	public int getOrder() {
		return SessionFactoryUtils.SESSION_SYNCHRONIZATION_ORDER;
	}

	public void suspend() {
		if (this.holderActive) {
			TransactionSynchronizationManager.unbindResource(this.sessionFactory);
			// Eagerly disconnect the Session here, to make release mode "on_close" work on JBoss.
			getCurrentSession().disconnect();
		}
	}

	public void resume() {
		if (this.holderActive) {
			TransactionSynchronizationManager.bindResource(this.sessionFactory, this.sessionHolder);
		}
	}

	public void flush() {
		try {
			SessionFactoryUtils.logger.debug("Flushing Hibernate Session on explicit request");
			getCurrentSession().flush();
		}
		catch (HibernateException ex) {
			throw translateException(ex);
		}
	}

	public void beforeCommit(boolean readOnly) throws DataAccessException {
		if (!readOnly) {
			Session session = getCurrentSession();
			// Read-write transaction -> flush the Hibernate Session.
			// Further check: only flush when not FlushMode.NEVER/MANUAL.
			if (!session.getFlushMode().lessThan(FlushMode.COMMIT)) {
				try {
					SessionFactoryUtils.logger.debug("Flushing Hibernate Session on transaction synchronization");
					session.flush();
				}
				catch (HibernateException ex) {
					throw translateException(ex);
				}
			}
		}
	}

	private DataAccessException translateException(HibernateException ex) {
		if (this.jdbcExceptionTranslator != null && ex instanceof JDBCException) {
			JDBCException jdbcEx = (JDBCException) ex;
			return this.jdbcExceptionTranslator.translate(
					"Hibernate flushing: " + jdbcEx.getMessage(), jdbcEx.getSQL(), jdbcEx.getSQLException());
		}
		return SessionFactoryUtils.convertHibernateAccessException(ex);
	}

	public void beforeCompletion() {
		if (this.jtaTransaction != null) {
			// Typically in case of a suspended JTA transaction:
			// Remove the Session for the current JTA transaction, but keep the holder.
			Session session = this.sessionHolder.removeSession(this.jtaTransaction);
			if (session != null) {
				if (this.sessionHolder.isEmpty()) {
					// No Sessions for JTA transactions bound anymore -> could remove it.
					TransactionSynchronizationManager.unbindResourceIfPossible(this.sessionFactory);
					this.holderActive = false;
				}
				// Do not close a pre-bound Session. In that case, we'll find the
				// transaction-specific Session the same as the default Session.
				if (session != this.sessionHolder.getSession()) {
					SessionFactoryUtils.closeSessionOrRegisterDeferredClose(session, this.sessionFactory);
				}
				else {
					if (this.sessionHolder.getPreviousFlushMode() != null) {
						// In case of pre-bound Session, restore previous flush mode.
						session.setFlushMode(this.sessionHolder.getPreviousFlushMode());
					}
					// Eagerly disconnect the Session here, to make release mode "on_close" work nicely.
					session.disconnect();
				}
				return;
			}
		}
		// We'll only get here if there was no specific JTA transaction to handle.
		if (this.newSession) {
			// Default behavior: unbind and close the thread-bound Hibernate Session.
			TransactionSynchronizationManager.unbindResource(this.sessionFactory);
			this.holderActive = false;
			if (this.hibernateTransactionCompletion) {
				// Close the Hibernate Session here in case of a Hibernate TransactionManagerLookup:
				// Hibernate will automatically defer the actual closing until JTA transaction completion.
				// Else, the Session will be closed in the afterCompletion method, to provide the
				// correct transaction status for releasing the Session's cache locks.
				SessionFactoryUtils.closeSessionOrRegisterDeferredClose(this.sessionHolder.getSession(), this.sessionFactory);
			}
		}
		else  {
			Session session = this.sessionHolder.getSession();
			if (this.sessionHolder.getPreviousFlushMode() != null) {
				// In case of pre-bound Session, restore previous flush mode.
				session.setFlushMode(this.sessionHolder.getPreviousFlushMode());
			}
			if (this.hibernateTransactionCompletion) {
				// Eagerly disconnect the Session here, to make release mode "on_close" work nicely.
				// We know that this is appropriate if a TransactionManagerLookup has been specified.
				session.disconnect();
			}
		}
	}

	public void afterCommit() {
	}

	public void afterCompletion(int status) {
		if (!this.hibernateTransactionCompletion || !this.newSession) {
			// No Hibernate TransactionManagerLookup: apply afterTransactionCompletion callback.
			// Always perform explicit afterTransactionCompletion callback for pre-bound Session,
			// even with Hibernate TransactionManagerLookup (which only applies to new Sessions).
			Session session = this.sessionHolder.getSession();
			// Provide correct transaction status for releasing the Session's cache locks,
			// if possible. Else, closing will release all cache locks assuming a rollback.
			if (session instanceof SessionImplementor) {
				((SessionImplementor) session).afterTransactionCompletion(status == STATUS_COMMITTED, null);
			}
			// Close the Hibernate Session here if necessary
			// (closed in beforeCompletion in case of TransactionManagerLookup).
			if (this.newSession) {
				SessionFactoryUtils.closeSessionOrRegisterDeferredClose(session, this.sessionFactory);
			}
			else if (!this.hibernateTransactionCompletion) {
				session.disconnect();
			}
		}
		if (!this.newSession && status != STATUS_COMMITTED) {
			// Clear all pending inserts/updates/deletes in the Session.
			// Necessary for pre-bound Sessions, to avoid inconsistent state.
			this.sessionHolder.getSession().clear();
		}
		if (this.sessionHolder.doesNotHoldNonDefaultSession()) {
			this.sessionHolder.setSynchronizedWithTransaction(false);
		}
	}

}
