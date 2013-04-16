/*
 * Copyright 2002-2013 the original author or authors.
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

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;

import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * {@link org.springframework.orm.jpa.JpaDialect} implementation for Apache OpenJPA.
 * Developed and tested against OpenJPA 2.2.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class OpenJpaDialect extends DefaultJpaDialect {

	@Override
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		super.beginTransaction(entityManager, definition);
		OpenJPAEntityManager em = getOpenJPAEntityManager(entityManager);
		if (!definition.isReadOnly()) {
			// Like with EclipseLink, make sure to start the logic transaction early so that other
			// participants using the connection (such as JdbcTemplate) run in a transaction.
			em.beginStore();
		}
		return new OpenJpaTransactionData(em);
	}

	@Override
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException {

		return new OpenJpaConnectionHandle(getOpenJPAEntityManager(entityManager));
	}

	/**
	 * Return the OpenJPA-specific variant of {@code EntityManager}.
	 * @param em the generic {@code EntityManager} instance
	 * @return the OpenJPA-specific variant of {@code EntityManager}
	 */
	protected OpenJPAEntityManager getOpenJPAEntityManager(EntityManager em) {
		return OpenJPAPersistence.cast(em);
	}


	/**
	 * Transaction data Object exposed from {@code beginTransaction},
	 * implementing the {@link SavepointManager} interface.
	 */
	private static class OpenJpaTransactionData implements SavepointManager {

		private final OpenJPAEntityManager entityManager;

		private int savepointCounter = 0;

		public OpenJpaTransactionData(OpenJPAEntityManager entityManager) {
			this.entityManager = entityManager;
		}

		public Object createSavepoint() throws TransactionException {
			this.savepointCounter++;
			String savepointName = ConnectionHolder.SAVEPOINT_NAME_PREFIX + this.savepointCounter;
			this.entityManager.setSavepoint(savepointName);
			return savepointName;
		}

		public void rollbackToSavepoint(Object savepoint) throws TransactionException {
			this.entityManager.rollbackToSavepoint((String) savepoint);
		}

		public void releaseSavepoint(Object savepoint) throws TransactionException {
			this.entityManager.releaseSavepoint((String) savepoint);
		}
	}


	/**
	 * {@link ConnectionHandle} implementation that fetches a new OpenJPA-provided
	 * Connection for every {@code getConnection} call and closes the Connection on
	 * {@code releaseConnection}. This is necessary because OpenJPA requires the
	 * fetched Connection to be closed before continuing EntityManager work.
	 * @see org.apache.openjpa.persistence.OpenJPAEntityManager#getConnection()
	 */
	private static class OpenJpaConnectionHandle implements ConnectionHandle {

		private final OpenJPAEntityManager entityManager;

		public OpenJpaConnectionHandle(OpenJPAEntityManager entityManager) {
			this.entityManager = entityManager;
		}

		public Connection getConnection() {
			return (Connection) this.entityManager.getConnection();
		}

		public void releaseConnection(Connection con) {
			JdbcUtils.closeConnection(con);
		}
	}

}
