/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.orm.toplink;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import oracle.toplink.exceptions.DatabaseException;
import oracle.toplink.exceptions.TopLinkException;
import oracle.toplink.internal.databaseaccess.Accessor;
import oracle.toplink.internal.databaseaccess.DatabaseAccessor;
import oracle.toplink.sessions.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager} implementation
 * for a single TopLink {@link SessionFactory}. Binds a TopLink Session from the
 * specified factory to the thread, potentially allowing for one thread-bound Session
 * per factory. {@link SessionFactoryUtils} and {@link TopLinkTemplate} are aware
 * of thread-bound Sessions and participate in such transactions automatically.
 * Using either of those or going through <code>Session.getActiveUnitOfWork()</code> is
 * required for TopLink access code supporting this transaction handling mechanism.
 *
 * <p>This transaction manager is appropriate for applications that use a single
 * TopLink SessionFactory for transactional data access. JTA (usually through
 * {@link org.springframework.transaction.jta.JtaTransactionManager}) is necessary
 * for accessing multiple transactional resources within the same transaction.
 * Note that you need to configure TopLink with an appropriate external transaction
 * controller in order to make it participate in JTA transactions.
 *
 * <p>This transaction manager also supports direct DataSource access within a transaction
 * (i.e. plain JDBC code working with the same DataSource), but only for transactions
 * that are <i>not</i> marked as read-only. This allows for mixing services which
 * access TopLink and services which use plain JDBC (without being aware of TopLink)!
 * Application code needs to stick to the same simple Connection lookup pattern as
 * with {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}
 * (i.e. {@link org.springframework.jdbc.datasource.DataSourceUtils#getConnection}
 * or going through a
 * {@link org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy}).
 *
 * <p>Note: To be able to register a DataSource's Connection for plain JDBC code,
 * this instance needs to be aware of the DataSource ({@link #setDataSource}).
 * The given DataSource should obviously match the one used by the given TopLink
 * SessionFactory.
 *
 * <p>On JDBC 3.0, this transaction manager supports nested transactions via JDBC 3.0
 * Savepoints. The {@link #setNestedTransactionAllowed} "nestedTransactionAllowed"}
 * flag defaults to "false", though, as nested transactions will just apply to the
 * JDBC Connection, not to the TopLink PersistenceManager and its cached objects.
 * You can manually set the flag to "true" if you want to use nested transactions
 * for JDBC access code which participates in TopLink transactions (provided that
 * your JDBC driver supports Savepoints). <i>Note that TopLink itself does not
 * support nested transactions! Hence, do not expect TopLink access code to
 * semantically participate in a nested transaction.</i>
 *
 * <p>Thanks to Slavik Markovich for implementing the initial TopLink support prototype!
 *
 * @author Juergen Hoeller
 * @author <a href="mailto:james.x.clark@oracle.com">James Clark</a>
 * @since 1.2
 * @see #setSessionFactory
 * @see #setDataSource
 * @see LocalSessionFactoryBean
 * @see SessionFactoryUtils#getSession
 * @see SessionFactoryUtils#releaseSession
 * @see TopLinkTemplate
 * @see oracle.toplink.sessions.Session#getActiveUnitOfWork()
 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
 * @see org.springframework.jdbc.datasource.DataSourceUtils#applyTransactionTimeout
 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
 * @see org.springframework.jdbc.core.JdbcTemplate
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
  * @see org.springframework.transaction.jta.JtaTransactionManager
 */
public class TopLinkTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, InitializingBean {

	private SessionFactory sessionFactory;

	private DataSource dataSource;

	private boolean lazyDatabaseTransaction = false;

	private SQLExceptionTranslator jdbcExceptionTranslator;


	/**
	 * Create a new TopLinkTransactionManager instance.
	 * A SessionFactory has to be specified to be able to use it.
	 * @see #setSessionFactory
	 */
	public TopLinkTransactionManager() {
	}

	/**
	 * Create a new TopLinkTransactionManager instance.
	 * @param sessionFactory the TopLink SessionFactory to manage transactions for
	 */
	public TopLinkTransactionManager(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		afterPropertiesSet();
	}

	/**
	 * Set the the TopLink SessionFactory to manage transactions for.
	 * This will usually be a ServerSessionFactory.
	 * <p>The passed-in SessionFactory will be asked for a plain Session
	 * in case of a read-only transaction (where no active UnitOfWork is
	 * supposed to be available), and for a managed Session else (with an
	 * active UnitOfWork that will be committed by this transaction manager).
	 * @see ServerSessionFactory
	 * @see SessionFactory#createSession()
	 * @see SessionFactory#createManagedClientSession()
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Return the SessionFactory that this instance should manage transactions for.
	 */
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	/**
	 * Set the JDBC DataSource that this instance should manage transactions for.
   * The DataSource should match the one used by the TopLink SessionFactory:
	 * for example, you could specify the same JNDI DataSource for both.
	 * <p>A transactional JDBC Connection for this DataSource will be provided to
	 * application code accessing this DataSource directly via DataSourceUtils
	 * or JdbcTemplate. The Connection will be taken from the TopLink Session.
	 * <b>This will only happen for transactions that are <i>not</i> marked
	 * as read-only.</b> TopLink does not support database transactions for pure
	 * read-only operations on a Session (that is, without a UnitOfWork).
	 * <p>Note that you need to use a TopLink Session with a DatabaseAccessor
	 * to allow for exposing TopLink transactions as JDBC transactions. This is
	 * the case of all standard TopLink configurations.
	 * <p>The DataSource specified here should be the target DataSource to manage
	 * transactions for, not a TransactionAwareDataSourceProxy. Only data access
	 * code may work with TransactionAwareDataSourceProxy, while the transaction
	 * manager needs to work on the underlying target DataSource. If there's
	 * nevertheless a TransactionAwareDataSourceProxy passed in, it will be
	 * unwrapped to extract its target DataSource.
	 * @see org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
	 * @see org.springframework.jdbc.datasource.DataSourceUtils
	 * @see org.springframework.jdbc.core.JdbcTemplate
	 */
	public void setDataSource(DataSource dataSource) {
		if (dataSource instanceof TransactionAwareDataSourceProxy) {
			// If we got a TransactionAwareDataSourceProxy, we need to perform transactions
			// for its underlying target DataSource, else data access code won't see
			// properly exposed transactions (i.e. transactions for the target DataSource).
			this.dataSource = ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource();
		}
		else {
			this.dataSource = dataSource;
		}
	}

	/**
	 * Return the JDBC DataSource that this instance manages transactions for.
	 */
	public DataSource getDataSource() {
		return this.dataSource;
	}

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
	 * @see #setDataSource(javax.sql.DataSource)
	 * @see oracle.toplink.sessions.UnitOfWork#beginEarlyTransaction()
	 */
	public void setLazyDatabaseTransaction(boolean lazyDatabaseTransaction) {
		this.lazyDatabaseTransaction = lazyDatabaseTransaction;
	}

	/**
	 * Return whether to lazily start a database transaction within a TopLink
	 * transaction.
	 */
	public boolean isLazyDatabaseTransaction() {
		return this.lazyDatabaseTransaction;
	}

	/**
	 * Set the JDBC exception translator for this transaction manager.
	 * <p>Applied to any SQLException root cause of a TopLink DatabaseException
	 * that is thrown on commit. The default is to rely on TopLink's native
	 * exception translation.
	 * @param jdbcExceptionTranslator the exception translator
	 * @see oracle.toplink.exceptions.DatabaseException
	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
	 * @see org.springframework.jdbc.support.SQLStateSQLExceptionTranslator
	 * @see #setDataSource(javax.sql.DataSource)
	 */
	public void setJdbcExceptionTranslator(SQLExceptionTranslator jdbcExceptionTranslator) {
		this.jdbcExceptionTranslator = jdbcExceptionTranslator;
	}

	/**
	 * Return the JDBC exception translator for this transaction manager, if any.
	 */
	public SQLExceptionTranslator getJdbcExceptionTranslator() {
		return this.jdbcExceptionTranslator;
	}

	public void afterPropertiesSet() {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("Property 'sessionFactory' is required");
		}
	}


	public Object getResourceFactory() {
		return getSessionFactory();
	}

	protected Object doGetTransaction() {
		TopLinkTransactionObject txObject = new TopLinkTransactionObject();
		SessionHolder sessionHolder = (SessionHolder)
				TransactionSynchronizationManager.getResource(this.sessionFactory);
		txObject.setSessionHolder(sessionHolder);
		return txObject;
	}

	protected boolean isExistingTransaction(Object transaction) {
		TopLinkTransactionObject txObject = (TopLinkTransactionObject) transaction;
		return (txObject.getSessionHolder() != null);
	}

	protected void doBegin(Object transaction, TransactionDefinition definition) {
		Session session = null;

		try {
			if (!definition.isReadOnly()) {
				logger.debug("Creating managed TopLink Session with active UnitOfWork for read-write transaction");
				session = getSessionFactory().createManagedClientSession();
			}
			else {
				logger.debug("Creating plain TopLink Session without active UnitOfWork for read-only transaction");
				session = getSessionFactory().createSession();
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Opened new session [" + session + "] for TopLink transaction");
			}

			TopLinkTransactionObject txObject = (TopLinkTransactionObject) transaction;
			txObject.setSessionHolder(new SessionHolder(session));
			txObject.getSessionHolder().setSynchronizedWithTransaction(true);

			// Register transaction timeout.
			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getSessionHolder().setTimeoutInSeconds(timeout);
			}

			// Enforce early database transaction for TopLink read-write transaction,
			// unless we are explicitly told to use lazy transactions.
			if (!definition.isReadOnly() && !isLazyDatabaseTransaction()) {
				session.getActiveUnitOfWork().beginEarlyTransaction();
			}

			// Register the TopLink Session's JDBC Connection for the DataSource, if set.
			if (getDataSource() != null) {
				Session mostSpecificSession = (!definition.isReadOnly() ? session.getActiveUnitOfWork() : session);
				Connection con = getJdbcConnection(mostSpecificSession);
				if (con != null) {
					ConnectionHolder conHolder = new ConnectionHolder(con);
					if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
						conHolder.setTimeoutInSeconds(timeout);
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Exposing TopLink transaction as JDBC transaction [" + con + "]");
					}
					TransactionSynchronizationManager.bindResource(getDataSource(), conHolder);
					txObject.setConnectionHolder(conHolder);
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Not exposing TopLink transaction [" + session +
								"] as JDBC transaction because no JDBC Connection could be retrieved from it");
					}
				}
			}

			// Bind the session holder to the thread.
			TransactionSynchronizationManager.bindResource(getSessionFactory(), txObject.getSessionHolder());
		}

		catch (Exception ex) {
			SessionFactoryUtils.releaseSession(session, getSessionFactory());
			throw new CannotCreateTransactionException("Could not open TopLink Session for transaction", ex);
		}
	}

	/**
	 * Extract the underlying JDBC Connection from the given TopLink Session.
	 * <p>Default implementation casts to <code>oracle.toplink.publicinterface.Session</code>
	 * and fetches the Connection from the DatabaseAccessor exposed there.
	 * @param session the current TopLink Session
	 * @return the underlying JDBC Connection, or <code>null</code> if none found
	 * @see oracle.toplink.publicinterface.Session#getAccessor()
	 * @see oracle.toplink.internal.databaseaccess.DatabaseAccessor#getConnection()
	 */
	protected Connection getJdbcConnection(Session session) {
		if (!(session instanceof oracle.toplink.publicinterface.Session)) {
			if (logger.isDebugEnabled()) {
				logger.debug("TopLink Session [" + session +
						"] does not derive from [oracle.toplink.publicinterface.Session]");
			}
			return null;
		}
		Accessor accessor = ((oracle.toplink.publicinterface.Session) session).getAccessor();
		if (!(accessor instanceof DatabaseAccessor)) {
			if (logger.isDebugEnabled()) {
				logger.debug("TopLink Accessor [" + accessor +
						"] does not derive from [oracle.toplink.internal.databaseaccess.DatabaseAccessor]");
			}
			return null;
		}
		return ((DatabaseAccessor) accessor).getConnection();
	}

	protected Object doSuspend(Object transaction) {
		TopLinkTransactionObject txObject = (TopLinkTransactionObject) transaction;
		txObject.setSessionHolder(null);
		return TransactionSynchronizationManager.unbindResource(getSessionFactory());
	}

	protected void doResume(Object transaction, Object suspendedResources) {
		SessionHolder sessionHolder = (SessionHolder) suspendedResources;
		if (TransactionSynchronizationManager.hasResource(getSessionFactory())) {
			// From non-transactional code running in active transaction synchronization
			// -> can be safely removed, will be closed on transaction completion.
			TransactionSynchronizationManager.unbindResource(getSessionFactory());
		}
		TransactionSynchronizationManager.bindResource(getSessionFactory(), sessionHolder);
	}

	protected void doCommit(DefaultTransactionStatus status) {
		TopLinkTransactionObject txObject = (TopLinkTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Committing TopLink transaction on session [" +
					txObject.getSessionHolder().getSession() + "]");
		}
		try {
			if (!status.isReadOnly()) {
				txObject.getSessionHolder().getSession().getActiveUnitOfWork().commit();
			}
			txObject.getSessionHolder().clear();
		}
		catch (TopLinkException ex) {
			throw convertTopLinkAccessException(ex);
		}
	}

	protected void doRollback(DefaultTransactionStatus status) {
		TopLinkTransactionObject txObject = (TopLinkTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Not committing TopLink transaction on session [" +
					txObject.getSessionHolder().getSession() + "]");
		}
		txObject.getSessionHolder().clear();
	}

	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		TopLinkTransactionObject txObject = (TopLinkTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting TopLink transaction on session [" +
					txObject.getSessionHolder().getSession() + "] rollback-only");
		}
		txObject.getSessionHolder().setRollbackOnly();
	}

	protected void doCleanupAfterCompletion(Object transaction) {
		TopLinkTransactionObject txObject = (TopLinkTransactionObject) transaction;

		// Remove the session holder from the thread.
		TransactionSynchronizationManager.unbindResource(getSessionFactory());

		// Remove the JDBC connection holder from the thread, if exposed.
		if (txObject.hasConnectionHolder()) {
			TransactionSynchronizationManager.unbindResource(getDataSource());
		}

		Session session = txObject.getSessionHolder().getSession();
		if (logger.isDebugEnabled()) {
			logger.debug("Releasing TopLink Session [" + session + "] after transaction");
		}
		try {
			session.release();
		}
		catch (Throwable ex) {
			// just log it, to keep a transaction-related exception
			logger.debug("Could not release TopLink Session after transaction", ex);
		}
	}

	/**
	 * Convert the given TopLinkException to an appropriate exception from the
	 * <code>org.springframework.dao</code> hierarchy.
	 * <p>Will automatically apply a specified SQLExceptionTranslator to a
	 * TopLink DatabaseException, else rely on TopLink's default translation.
	 * @param ex TopLinkException that occured
	 * @return a corresponding DataAccessException
	 * @see SessionFactoryUtils#convertTopLinkAccessException
	 * @see #setJdbcExceptionTranslator
	 */
	protected DataAccessException convertTopLinkAccessException(TopLinkException ex) {
		if (getJdbcExceptionTranslator() != null && ex instanceof DatabaseException) {
			Throwable internalEx = ex.getInternalException();
			// Should always be a SQLException inside a DatabaseException.
			if (internalEx instanceof SQLException) {
				return getJdbcExceptionTranslator().translate(
						"TopLink commit: " + ex.getMessage(), null, (SQLException) internalEx);
			}
		}
		return SessionFactoryUtils.convertTopLinkAccessException(ex);
	}


	/**
	 * TopLink transaction object, representing a SessionHolder.
	 * Used as transaction object by TopLinkTransactionManager.
	 */
	private static class TopLinkTransactionObject extends JdbcTransactionObjectSupport {

		private SessionHolder sessionHolder;

		public void setSessionHolder(SessionHolder sessionHolder) {
			this.sessionHolder = sessionHolder;
		}

		public SessionHolder getSessionHolder() {
			return this.sessionHolder;
		}

		public boolean isRollbackOnly() {
			return getSessionHolder().isRollbackOnly();
		}
	}

}
