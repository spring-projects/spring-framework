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

import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import oracle.toplink.essentials.internal.sessions.AbstractSession;
import oracle.toplink.essentials.sessions.Session;
import oracle.toplink.essentials.sessions.UnitOfWork;

import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.SimpleConnectionHandle;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * {@link org.springframework.orm.jpa.JpaDialect} implementation for
 * Oracle TopLink Essentials. Developed and tested against TopLink Essentials v2.
 *
 * <p>By default, this class acquires a TopLink transaction to get the JDBC Connection
 * early. This allows mixing JDBC and JPA/TopLink operations in the same transaction.
 * In some cases, this eager acquisition of a transaction/connection may impact
 * scalability. In that case, set the "lazyDatabaseTransaction" flag to true if you
 * do not require mixing JDBC and JPA operations in the same transaction. Otherwise,
 * use a {@link org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy}
 * to ensure that the cost of connection acquisition is near zero until code actually
 * needs a JDBC Connection.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setLazyDatabaseTransaction
 * @see org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
 * @deprecated as of Spring 3.1, in favor of the EclipseLink project and
 * Spring's corresponding {@link EclipseLinkJpaDialect}
 */
@Deprecated
public class TopLinkJpaDialect extends DefaultJpaDialect {

	private boolean lazyDatabaseTransaction = false;


	/**
	 * Set whether to lazily start a database transaction within a TopLink
	 * transaction.
	 * <p>By default, database transactions are started early. This allows
	 * for reusing the same JDBC Connection throughout an entire transaction,
	 * including read operations, and also for exposing TopLink transactions
	 * to JDBC access code (working on the same DataSource).
	 * <p>It is only recommended to switch this flag to "true" when no JDBC access
	 * code is involved in any of the transactions, and when it is acceptable to
	 * perform read operations outside of the transactional JDBC Connection.
	 * @see oracle.toplink.essentials.sessions.UnitOfWork#beginEarlyTransaction()
	 */
	public void setLazyDatabaseTransaction(boolean lazyDatabaseTransaction) {
		this.lazyDatabaseTransaction = lazyDatabaseTransaction;
	}


	@Override
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		super.beginTransaction(entityManager, definition);
		if (!definition.isReadOnly() && !this.lazyDatabaseTransaction) {
			// This is the magic bit. As with the existing Spring TopLink integration,
			// begin an early transaction to force TopLink to get a JDBC Connection
			// so that Spring can manage transactions with JDBC as well as TopLink.
			UnitOfWork uow = (UnitOfWork) getSession(entityManager);
			uow.beginEarlyTransaction();
		}
		// Could return the UOW, if there were any advantage in having it later.
		return null;
	}

	@Override
	public ConnectionHandle getJdbcConnection(EntityManager em, boolean readOnly)
			throws PersistenceException, SQLException {

		AbstractSession session = (AbstractSession) getSession(em);
		// The connection was already acquired eagerly in beginTransaction,
		// unless lazyDatabaseTransaction was set to true.
		Connection con = session.getAccessor().getConnection();
		return (con != null ? new SimpleConnectionHandle(con) : null);
	}

	/**
	 * Get a traditional TopLink Session from the given EntityManager.
	 */
	protected Session getSession(EntityManager em) {
		oracle.toplink.essentials.ejb.cmp3.EntityManager emi = (oracle.toplink.essentials.ejb.cmp3.EntityManager) em;
		return emi.getActiveSession();
	}

}
