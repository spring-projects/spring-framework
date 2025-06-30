/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.jdbc.support;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * {@link JdbcAccessor}-aligned subclass of the plain {@link DataSourceTransactionManager},
 * adding common JDBC exception translation for the commit and rollback step.
 * Typically used in combination with {@link org.springframework.jdbc.core.JdbcTemplate}
 * which applies the same {@link SQLExceptionTranslator} infrastructure by default.
 *
 * <p>Exception translation is specifically relevant for commit steps in serializable
 * transactions (for example, on Postgres) where concurrency failures may occur late on commit.
 * This allows for throwing {@link org.springframework.dao.ConcurrencyFailureException} to
 * callers instead of {@link org.springframework.transaction.TransactionSystemException}.
 *
 * <p>Analogous to {@code HibernateTransactionManager} and {@code JpaTransactionManager},
 * this transaction manager may throw {@link DataAccessException} from {@link #commit}
 * and possibly also from {@link #rollback}. Calling code should be prepared for handling
 * such exceptions next to {@link org.springframework.transaction.TransactionException},
 * which is generally sensible since {@code TransactionSynchronization} implementations
 * may also throw such exceptions in their {@code flush} and {@code beforeCommit} phases.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.3
 * @see DataSourceTransactionManager
 * @see #setDataSource
 * @see #setExceptionTranslator
 */
@SuppressWarnings("serial")
public class JdbcTransactionManager extends DataSourceTransactionManager {

	private volatile @Nullable SQLExceptionTranslator exceptionTranslator;

	private boolean lazyInit = true;


	/**
	 * Create a new {@code JdbcTransactionManager} instance.
	 * A {@code DataSource} has to be set to be able to use it.
	 * @see #setDataSource
	 */
	public JdbcTransactionManager() {
		super();
	}

	/**
	 * Create a new {@code JdbcTransactionManager} instance.
	 * @param dataSource the JDBC DataSource to manage transactions for
	 */
	public JdbcTransactionManager(DataSource dataSource) {
		this();
		setDataSource(dataSource);
		afterPropertiesSet();
	}


	/**
	 * Specify the database product name for the {@code DataSource} that this
	 * transaction manager operates on.
	 * This allows for initializing a {@link SQLErrorCodeSQLExceptionTranslator} without
	 * obtaining a {@code Connection} from the {@code DataSource} to get the meta-data.
	 * @param dbName the database product name that identifies the error codes entry
	 * @see #setExceptionTranslator
	 * @see SQLErrorCodeSQLExceptionTranslator#setDatabaseProductName
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 * @see JdbcAccessor#setDatabaseProductName
	 */
	public void setDatabaseProductName(String dbName) {
		if (SQLErrorCodeSQLExceptionTranslator.hasUserProvidedErrorCodesFile()) {
			this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dbName);
		}
		else {
			this.exceptionTranslator = new SQLExceptionSubclassTranslator();
		}
	}

	/**
	 * Set the exception translator for this transaction manager.
	 * <p>A {@link SQLErrorCodeSQLExceptionTranslator} used by default if a user-provided
	 * `sql-error-codes.xml` file has been found in the root of the classpath. Otherwise,
	 * {@link SQLExceptionSubclassTranslator} serves as the default translator as of 6.0.
	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
	 * @see org.springframework.jdbc.support.SQLExceptionSubclassTranslator
	 * @see JdbcAccessor#setExceptionTranslator
	 */
	public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
		this.exceptionTranslator = exceptionTranslator;
	}

	/**
	 * Return the exception translator to use for this instance,
	 * creating a default if necessary.
	 * @see #setExceptionTranslator
	 */
	public SQLExceptionTranslator getExceptionTranslator() {
		SQLExceptionTranslator exceptionTranslator = this.exceptionTranslator;
		if (exceptionTranslator != null) {
			return exceptionTranslator;
		}
		synchronized (this) {
			exceptionTranslator = this.exceptionTranslator;
			if (exceptionTranslator == null) {
				if (SQLErrorCodeSQLExceptionTranslator.hasUserProvidedErrorCodesFile()) {
					exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(obtainDataSource());
				}
				else {
					exceptionTranslator = new SQLExceptionSubclassTranslator();
				}
				this.exceptionTranslator = exceptionTranslator;
			}
			return exceptionTranslator;
		}
	}

	/**
	 * Set whether to lazily initialize the SQLExceptionTranslator for this transaction manager,
	 * on first encounter of an SQLException. Default is "true"; can be switched to
	 * "false" for initialization on startup.
	 * <p>Early initialization just applies if {@code afterPropertiesSet()} is called.
	 * @see #getExceptionTranslator()
	 * @see #afterPropertiesSet()
	 */
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * Return whether to lazily initialize the SQLExceptionTranslator for this transaction manager.
	 * @see #getExceptionTranslator()
	 */
	public boolean isLazyInit() {
		return this.lazyInit;
	}

	/**
	 * Eagerly initialize the exception translator, if demanded,
	 * creating a default one for the specified DataSource if none set.
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (!isLazyInit()) {
			getExceptionTranslator();
		}
	}


	/**
	 * This implementation attempts to use the {@link SQLExceptionTranslator},
	 * falling back to a {@link org.springframework.transaction.TransactionSystemException}.
	 * @see #getExceptionTranslator()
	 * @see DataSourceTransactionManager#translateException
	 */
	@Override
	protected RuntimeException translateException(String task, SQLException ex) {
		DataAccessException dae = getExceptionTranslator().translate(task, null, ex);
		if (dae != null) {
			return dae;
		}
		return super.translateException(task, ex);
	}

}
