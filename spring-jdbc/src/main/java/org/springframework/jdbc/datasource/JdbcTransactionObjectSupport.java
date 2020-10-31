/*
 * Copyright 2002-2019 the original author or authors.
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

import java.sql.SQLException;
import java.sql.Savepoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionUsageException;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.util.Assert;

/**
 * Convenient base class for JDBC-aware transaction objects. Can contain a
 * {@link ConnectionHolder} with a JDBC {@code Connection}, and implements the
 * {@link SavepointManager} interface based on that {@code ConnectionHolder}.
 *
 * <p>Allows for programmatic management of JDBC {@link java.sql.Savepoint Savepoints}.
 * Spring's {@link org.springframework.transaction.support.DefaultTransactionStatus}
 * automatically delegates to this, as it autodetects transaction objects which
 * implement the {@link SavepointManager} interface.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see DataSourceTransactionManager
 */
public abstract class JdbcTransactionObjectSupport implements SavepointManager, SmartTransactionObject {

	private static final Log logger = LogFactory.getLog(JdbcTransactionObjectSupport.class);


	@Nullable
	private ConnectionHolder connectionHolder;

	@Nullable
	private Integer previousIsolationLevel;

	private boolean readOnly = false;

	private boolean savepointAllowed = false;


	/**
	 * Set the ConnectionHolder for this transaction object.
	 */
	public void setConnectionHolder(@Nullable ConnectionHolder connectionHolder) {
		this.connectionHolder = connectionHolder;
	}

	/**
	 * Return the ConnectionHolder for this transaction object.
	 */
	public ConnectionHolder getConnectionHolder() {
		Assert.state(this.connectionHolder != null, "No ConnectionHolder available");
		return this.connectionHolder;
	}

	/**
	 * Check whether this transaction object has a ConnectionHolder.
	 */
	public boolean hasConnectionHolder() {
		return (this.connectionHolder != null);
	}

	/**
	 * Set the previous isolation level to retain, if any.
	 */
	public void setPreviousIsolationLevel(@Nullable Integer previousIsolationLevel) {
		this.previousIsolationLevel = previousIsolationLevel;
	}

	/**
	 * Return the retained previous isolation level, if any.
	 */
	@Nullable
	public Integer getPreviousIsolationLevel() {
		return this.previousIsolationLevel;
	}

	/**
	 * Set the read-only status of this transaction.
	 * The default is {@code false}.
	 * @since 5.2.1
	 */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * Return the read-only status of this transaction.
	 * @since 5.2.1
	 */
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * Set whether savepoints are allowed within this transaction.
	 * The default is {@code false}.
	 */
	public void setSavepointAllowed(boolean savepointAllowed) {
		this.savepointAllowed = savepointAllowed;
	}

	/**
	 * Return whether savepoints are allowed within this transaction.
	 */
	public boolean isSavepointAllowed() {
		return this.savepointAllowed;
	}

	@Override
	public void flush() {
		// no-op
	}


	//---------------------------------------------------------------------
	// Implementation of SavepointManager
	//---------------------------------------------------------------------

	/**
	 * This implementation creates a JDBC 3.0 Savepoint and returns it.
	 * @see java.sql.Connection#setSavepoint
	 */
	@Override
	public Object createSavepoint() throws TransactionException {
		ConnectionHolder conHolder = getConnectionHolderForSavepoint();
		try {
			if (!conHolder.supportsSavepoints()) {
				throw new NestedTransactionNotSupportedException(
						"Cannot create a nested transaction because savepoints are not supported by your JDBC driver");
			}
			if (conHolder.isRollbackOnly()) {
				throw new CannotCreateTransactionException(
						"Cannot create savepoint for transaction which is already marked as rollback-only");
			}
			return conHolder.createSavepoint();
		}
		catch (SQLException ex) {
			throw new CannotCreateTransactionException("Could not create JDBC savepoint", ex);
		}
	}

	/**
	 * This implementation rolls back to the given JDBC 3.0 Savepoint.
	 * @see java.sql.Connection#rollback(java.sql.Savepoint)
	 */
	@Override
	public void rollbackToSavepoint(Object savepoint) throws TransactionException {
		ConnectionHolder conHolder = getConnectionHolderForSavepoint();
		try {
			conHolder.getConnection().rollback((Savepoint) savepoint);
			conHolder.resetRollbackOnly();
		}
		catch (Throwable ex) {
			throw new TransactionSystemException("Could not roll back to JDBC savepoint", ex);
		}
	}

	/**
	 * This implementation releases the given JDBC 3.0 Savepoint.
	 * @see java.sql.Connection#releaseSavepoint
	 */
	@Override
	public void releaseSavepoint(Object savepoint) throws TransactionException {
		ConnectionHolder conHolder = getConnectionHolderForSavepoint();
		try {
			conHolder.getConnection().releaseSavepoint((Savepoint) savepoint);
		}
		catch (Throwable ex) {
			logger.debug("Could not explicitly release JDBC savepoint", ex);
		}
	}

	protected ConnectionHolder getConnectionHolderForSavepoint() throws TransactionException {
		if (!isSavepointAllowed()) {
			throw new NestedTransactionNotSupportedException(
					"Transaction manager does not allow nested transactions");
		}
		if (!hasConnectionHolder()) {
			throw new TransactionUsageException(
					"Cannot create nested transaction when not exposing a JDBC transaction");
		}
		return getConnectionHolder();
	}

}
