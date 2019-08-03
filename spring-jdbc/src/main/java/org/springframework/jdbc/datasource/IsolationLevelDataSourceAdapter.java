/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * An adapter for a target {@link javax.sql.DataSource}, applying the current
 * Spring transaction's isolation level (and potentially specified user credentials)
 * to every {@code getConnection} call. Also applies the read-only flag,
 * if specified.
 *
 * <p>Can be used to proxy a target JNDI DataSource that does not have the
 * desired isolation level (and user credentials) configured. Client code
 * can work with this DataSource as usual, not worrying about such settings.
 *
 * <p>Inherits the capability to apply specific user credentials from its superclass
 * {@link UserCredentialsDataSourceAdapter}; see the latter's javadoc for details
 * on that functionality (e.g. {@link #setCredentialsForCurrentThread}).
 *
 * <p><b>WARNING:</b> This adapter simply calls
 * {@link java.sql.Connection#setTransactionIsolation} and/or
 * {@link java.sql.Connection#setReadOnly} for every Connection obtained from it.
 * It does, however, <i>not</i> reset those settings; it rather expects the target
 * DataSource to perform such resetting as part of its connection pool handling.
 * <b>Make sure that the target DataSource properly cleans up such transaction state.</b>
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #setIsolationLevel
 * @see #setIsolationLevelName
 * @see #setUsername
 * @see #setPassword
 */
public class IsolationLevelDataSourceAdapter extends UserCredentialsDataSourceAdapter {

	/** Constants instance for TransactionDefinition. */
	private static final Constants constants = new Constants(TransactionDefinition.class);

	@Nullable
	private Integer isolationLevel;


	/**
	 * Set the default isolation level by the name of the corresponding constant
	 * in {@link org.springframework.transaction.TransactionDefinition}, e.g.
	 * "ISOLATION_SERIALIZABLE".
	 * <p>If not specified, the target DataSource's default will be used.
	 * Note that a transaction-specific isolation value will always override
	 * any isolation setting specified at the DataSource level.
	 * @param constantName name of the constant
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see #setIsolationLevel
	 */
	public final void setIsolationLevelName(String constantName) throws IllegalArgumentException {
		if (!constantName.startsWith(DefaultTransactionDefinition.PREFIX_ISOLATION)) {
			throw new IllegalArgumentException("Only isolation constants allowed");
		}
		setIsolationLevel(constants.asNumber(constantName).intValue());
	}

	/**
	 * Specify the default isolation level to use for Connection retrieval,
	 * according to the JDBC {@link java.sql.Connection} constants
	 * (equivalent to the corresponding Spring
	 * {@link org.springframework.transaction.TransactionDefinition} constants).
	 * <p>If not specified, the target DataSource's default will be used.
	 * Note that a transaction-specific isolation value will always override
	 * any isolation setting specified at the DataSource level.
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()
	 */
	public void setIsolationLevel(int isolationLevel) {
		if (!constants.getValues(DefaultTransactionDefinition.PREFIX_ISOLATION).contains(isolationLevel)) {
			throw new IllegalArgumentException("Only values of isolation constants allowed");
		}
		this.isolationLevel = (isolationLevel != TransactionDefinition.ISOLATION_DEFAULT ? isolationLevel : null);
	}

	/**
	 * Return the statically specified isolation level,
	 * or {@code null} if none.
	 */
	@Nullable
	protected Integer getIsolationLevel() {
		return this.isolationLevel;
	}


	/**
	 * Applies the current isolation level value and read-only flag
	 * to the returned Connection.
	 * @see #getCurrentIsolationLevel()
	 * @see #getCurrentReadOnlyFlag()
	 */
	@Override
	protected Connection doGetConnection(@Nullable String username, @Nullable String password) throws SQLException {
		Connection con = super.doGetConnection(username, password);
		Boolean readOnlyToUse = getCurrentReadOnlyFlag();
		if (readOnlyToUse != null) {
			con.setReadOnly(readOnlyToUse);
		}
		Integer isolationLevelToUse = getCurrentIsolationLevel();
		if (isolationLevelToUse != null) {
			con.setTransactionIsolation(isolationLevelToUse);
		}
		return con;
	}

	/**
	 * Determine the current isolation level: either the transaction's
	 * isolation level or a statically defined isolation level.
	 * @return the current isolation level, or {@code null} if none
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()
	 * @see #setIsolationLevel
	 */
	@Nullable
	protected Integer getCurrentIsolationLevel() {
		Integer isolationLevelToUse = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
		if (isolationLevelToUse == null) {
			isolationLevelToUse = getIsolationLevel();
		}
		return isolationLevelToUse;
	}

	/**
	 * Determine the current read-only flag: by default,
	 * the transaction's read-only hint.
	 * @return whether there is a read-only hint for the current scope
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
	 */
	@Nullable
	protected Boolean getCurrentReadOnlyFlag() {
		boolean txReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
		return (txReadOnly ? Boolean.TRUE : null);
	}

}
