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

package org.springframework.orm.jpa.vendor;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.ejb.HibernateEntityManager;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.springframework.orm.jpa.JpaDialect} implementation for
 * Hibernate EntityManager. Developed against Hibernate 3.3;
 * tested against 3.3, 3.5, 3.6 and 4.0 (with the latter including
 * Hibernate EntityManager in the Hibernate core distribution).
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 */
public class HibernateJpaDialect extends DefaultJpaDialect {

	@Override
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
			getSession(entityManager).getTransaction().setTimeout(definition.getTimeout());
		}
		super.beginTransaction(entityManager, definition);
		return prepareTransaction(entityManager, definition.isReadOnly(), definition.getName());
	}

	@Override
	public Object prepareTransaction(EntityManager entityManager, boolean readOnly, String name)
			throws PersistenceException {

		Session session = getSession(entityManager);
		FlushMode flushMode = session.getFlushMode();
		FlushMode previousFlushMode = null;
		if (readOnly) {
			// We should suppress flushing for a read-only transaction.
			session.setFlushMode(FlushMode.MANUAL);
			previousFlushMode = flushMode;
		}
		else {
			// We need AUTO or COMMIT for a non-read-only transaction.
			if (flushMode.lessThan(FlushMode.COMMIT)) {
				session.setFlushMode(FlushMode.AUTO);
				previousFlushMode = flushMode;
			}
		}
		return new SessionTransactionData(session, previousFlushMode);
	}

	@Override
	public void cleanupTransaction(Object transactionData) {
		((SessionTransactionData) transactionData).resetFlushMode();
	}

	@Override
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException {

		Session session = getSession(entityManager);
		return new HibernateConnectionHandle(session);
	}

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof HibernateException) {
			return SessionFactoryUtils.convertHibernateAccessException((HibernateException) ex);
		}
		if (ex instanceof PersistenceException && ex.getCause() instanceof HibernateException) {
			return SessionFactoryUtils.convertHibernateAccessException((HibernateException) ex.getCause());
		}
		return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
	}

	protected Session getSession(EntityManager em) {
		if (em instanceof HibernateEntityManager) {
			return ((HibernateEntityManager) em).getSession();
		}
		else {
			Object delegate = em.getDelegate();
			if (delegate instanceof Session) {
				return (Session) delegate;
			}
			else {
				throw new IllegalStateException(
						"Cannot obtain native Hibernate Session from given JPA EntityManager: " + em.getClass());
			}
		}
	}


	private static class SessionTransactionData {

		private final Session session;

		private final FlushMode previousFlushMode;

		public SessionTransactionData(Session session, FlushMode previousFlushMode) {
			this.session = session;
			this.previousFlushMode = previousFlushMode;
		}

		public void resetFlushMode() {
			if (this.previousFlushMode != null) {
				this.session.setFlushMode(this.previousFlushMode);
			}
		}
	}


	private static class HibernateConnectionHandle implements ConnectionHandle {

		private final Session session;

		private static volatile Method connectionMethod;

		public HibernateConnectionHandle(Session session) {
			this.session = session;
		}

		public Connection getConnection() {
			try {
				if (connectionMethod == null) {
					// reflective lookup to bridge between Hibernate 3.x and 4.x
					connectionMethod = this.session.getClass().getMethod("connection");
				}
				return (Connection) ReflectionUtils.invokeMethod(connectionMethod, this.session);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Cannot find connection() method on Hibernate session", ex);
			}
		}

		public void releaseConnection(Connection con) {
			JdbcUtils.closeConnection(con);
		}
	}

}
