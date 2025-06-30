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

package org.springframework.orm.jpa.hibernate;

import java.sql.Connection;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.service.UnknownServiceException;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager}
 * implementation for a single Hibernate {@link SessionFactory}.
 * Binds a Hibernate Session from the specified factory to the thread,
 * potentially allowing for one thread-bound Session per factory.
 * {@code SessionFactory.getCurrentSession()} is required for Hibernate
 * access code that needs to support this transaction handling mechanism,
 * with the SessionFactory being configured with {@link SpringSessionContext}.
 *
 * <p>Supports custom isolation levels, and timeouts that get applied as
 * Hibernate transaction timeouts.
 *
 * <p>This transaction manager is appropriate for applications that use a single
 * Hibernate SessionFactory for transactional data access, but it also supports
 * direct DataSource access within a transaction (i.e. plain JDBC code working
 * with the same DataSource). This allows for mixing services which access Hibernate
 * and services which use plain JDBC (without being aware of Hibernate)!
 * Application code needs to stick to the same simple Connection lookup pattern as
 * with {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}
 * (i.e. {@link DataSourceUtils#getConnection}
 * or going through a
 * {@link TransactionAwareDataSourceProxy}).
 *
 * <p>Note: To be able to register a DataSource's Connection for plain JDBC code,
 * this instance needs to be aware of the DataSource ({@link #setDataSource}).
 * The given DataSource should obviously match the one used by the given SessionFactory.
 *
 * <p>JTA (usually through {@link org.springframework.transaction.jta.JtaTransactionManager})
 * is necessary for accessing multiple transactional resources within the same
 * transaction. The DataSource that Hibernate uses needs to be JTA-enabled in
 * such a scenario (see container setup).
 *
 * <p>This transaction manager supports nested transactions via JDBC Savepoints.
 * The {@link #setNestedTransactionAllowed} "nestedTransactionAllowed"} flag defaults
 * to "false", though, as nested transactions will just apply to the JDBC Connection,
 * not to the Hibernate Session and its cached entity objects and related context.
 * You can manually set the flag to "true" if you want to use nested transactions
 * for JDBC access code which participates in Hibernate transactions (provided that
 * your JDBC driver supports Savepoints). <i>Note that Hibernate itself does not
 * support nested transactions! Hence, do not expect Hibernate access code to
 * semantically participate in a nested transaction.</i>
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see #setSessionFactory
 * @see SessionFactory#getCurrentSession()
 * @see org.springframework.jdbc.core.JdbcTemplate
 * @see org.springframework.jdbc.support.JdbcTransactionManager
 * @see org.springframework.orm.jpa.JpaTransactionManager
 * @see org.springframework.orm.jpa.vendor.HibernateJpaDialect
 */
@SuppressWarnings("serial")
public class HibernateTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {

	private final HibernateExceptionTranslator exceptionTranslator = new HibernateExceptionTranslator();

	private @Nullable SessionFactory sessionFactory;

	private @Nullable DataSource dataSource;

	private boolean autodetectDataSource = true;

	private boolean prepareConnection = true;

	private boolean hibernateManagedSession = false;

	private @Nullable Consumer<Session> sessionInitializer;

	private @Nullable Object entityInterceptor;

	/**
	 * Just needed for entityInterceptorBeanName.
	 * @see #setEntityInterceptorBeanName
	 */
	private @Nullable BeanFactory beanFactory;


	/**
	 * Create a new HibernateTransactionManager instance.
	 * A SessionFactory has to be set to be able to use it.
	 * @see #setSessionFactory
	 */
	public HibernateTransactionManager() {
	}

	/**
	 * Create a new HibernateTransactionManager instance.
	 * @param sessionFactory the SessionFactory to manage transactions for
	 */
	public HibernateTransactionManager(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		afterPropertiesSet();
	}


	/**
	 * Set the SessionFactory that this instance should manage transactions for.
	 */
	public void setSessionFactory(@Nullable SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Return the SessionFactory that this instance should manage transactions for.
	 */
	public @Nullable SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	/**
	 * Obtain the SessionFactory for actual use.
	 * @return the SessionFactory (never {@code null})
	 * @throws IllegalStateException in case of no SessionFactory set
	 */
	protected final SessionFactory obtainSessionFactory() {
		SessionFactory sessionFactory = getSessionFactory();
		Assert.state(sessionFactory != null, "No SessionFactory set");
		return sessionFactory;
	}

	/**
	 * Set the JDBC DataSource that this instance should manage transactions for.
	 * <p>The DataSource should match the one used by the Hibernate SessionFactory:
	 * for example, you could specify the same JNDI DataSource for both.
	 * <p>If the SessionFactory was configured with LocalDataSourceConnectionProvider,
	 * i.e. by Spring's LocalSessionFactoryBean with a specified "dataSource",
	 * the DataSource will be auto-detected. You can still explicitly specify the
	 * DataSource, but you don't need to in this case.
	 * <p>A transactional JDBC Connection for this DataSource will be provided to
	 * application code accessing this DataSource directly via DataSourceUtils
	 * or JdbcTemplate. The Connection will be taken from the Hibernate Session.
	 * <p>The DataSource specified here should be the target DataSource to manage
	 * transactions for, not a TransactionAwareDataSourceProxy. Only data access
	 * code may work with TransactionAwareDataSourceProxy, while the transaction
	 * manager needs to work on the underlying target DataSource. If there's
	 * nevertheless a TransactionAwareDataSourceProxy passed in, it will be
	 * unwrapped to extract its target DataSource.
	 * <p><b>NOTE: For scenarios with many transactions that just read data from
	 * Hibernate's cache (and do not actually access the database), consider using
	 * a {@link org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy}
	 * for the actual target DataSource. Alternatively, consider switching
	 * {@link #setPrepareConnection "prepareConnection"} to {@code false}.</b>
	 * In both cases, this transaction manager will not eagerly acquire a
	 * JDBC Connection for each Hibernate Session.
	 * @see #setAutodetectDataSource
	 * @see TransactionAwareDataSourceProxy
	 * @see org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
	 * @see org.springframework.jdbc.core.JdbcTemplate
	 */
	public void setDataSource(@Nullable DataSource dataSource) {
		if (dataSource instanceof TransactionAwareDataSourceProxy proxy) {
			// If we got a TransactionAwareDataSourceProxy, we need to perform transactions
			// for its underlying target DataSource, else data access code won't see
			// properly exposed transactions (i.e. transactions for the target DataSource).
			this.dataSource = proxy.getTargetDataSource();
		}
		else {
			this.dataSource = dataSource;
		}
	}

	/**
	 * Return the JDBC DataSource that this instance manages transactions for.
	 */
	public @Nullable DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * Set whether to autodetect a JDBC DataSource used by the Hibernate SessionFactory,
	 * if set via LocalSessionFactoryBean's {@code setDataSource}. Default is "true".
	 * <p>Can be turned off to deliberately ignore an available DataSource, in order
	 * to not expose Hibernate transactions as JDBC transactions for that DataSource.
	 * @see #setDataSource
	 */
	public void setAutodetectDataSource(boolean autodetectDataSource) {
		this.autodetectDataSource = autodetectDataSource;
	}

	/**
	 * Set whether to prepare the underlying JDBC Connection of a transactional
	 * Hibernate Session, that is, whether to apply a transaction-specific
	 * isolation level and/or the transaction's read-only flag to the underlying
	 * JDBC Connection.
	 * <p>Default is "true". If you turn this flag off, the transaction manager
	 * will not support per-transaction isolation levels anymore. It will not
	 * call {@code Connection.setReadOnly(true)} for read-only transactions
	 * anymore either. If this flag is turned off, no cleanup of a JDBC Connection
	 * is required after a transaction, since no Connection settings will get modified.
	 * @see Connection#setTransactionIsolation
	 * @see Connection#setReadOnly
	 */
	public void setPrepareConnection(boolean prepareConnection) {
		this.prepareConnection = prepareConnection;
	}

	/**
	 * Set whether to operate on a Hibernate-managed Session instead of a
	 * Spring-managed Session, that is, whether to obtain the Session through
	 * Hibernate's {@link SessionFactory#getCurrentSession()} instead of
	 * {@link SessionFactory#openSession()} (with a Spring
	 * {@link TransactionSynchronizationManager} check preceding it).
	 * <p>Default is "false", i.e. using a Spring-managed Session: taking the current
	 * thread-bound Session if available (for example, in an Open-Session-in-View scenario),
	 * creating a new Session for the current transaction otherwise.
	 * <p>Switch this flag to "true" in order to enforce use of a Hibernate-managed Session.
	 * Note that this requires {@link SessionFactory#getCurrentSession()}
	 * to always return a proper Session when called for a Spring-managed transaction;
	 * transaction begin will fail if the {@code getCurrentSession()} call fails.
	 * <p>This mode will typically be used in combination with a custom Hibernate
	 * {@link org.hibernate.context.spi.CurrentSessionContext} implementation that stores
	 * Sessions in a place other than Spring's TransactionSynchronizationManager.
	 * It may also be used in combination with Spring's Open-Session-in-View support
	 * (using Spring's default {@link SpringSessionContext}), in which case it subtly
	 * differs from the Spring-managed Session mode: The pre-bound Session will <i>not</i>
	 * receive a {@code clear()} call (on rollback) or a {@code disconnect()}
	 * call (on transaction completion) in such a scenario; this is rather left up
	 * to a custom CurrentSessionContext implementation (if desired).
	 */
	public void setHibernateManagedSession(boolean hibernateManagedSession) {
		this.hibernateManagedSession = hibernateManagedSession;
	}

	/**
	 * Specify a callback for customizing every Hibernate {@code Session} resource
	 * created for a new transaction managed by this {@code HibernateTransactionManager}.
	 * <p>This enables convenient customizations for application purposes, for example,
	 * setting Hibernate filters.
	 * @see Session#enableFilter
	 */
	public void setSessionInitializer(Consumer<Session> sessionInitializer) {
		this.sessionInitializer = sessionInitializer;
	}

	/**
	 * Set the bean name of a Hibernate entity interceptor that allows to inspect
	 * and change property values before writing to and reading from the database.
	 * Will get applied to any new Session created by this transaction manager.
	 * <p>Requires the bean factory to be known, to be able to resolve the bean
	 * name to an interceptor instance on session creation. Typically used for
	 * prototype interceptors, i.e. a new interceptor instance per session.
	 * <p>Can also be used for shared interceptor instances, but it is recommended
	 * to set the interceptor reference directly in such a scenario.
	 * @param entityInterceptorBeanName the name of the entity interceptor in
	 * the bean factory
	 * @see #setBeanFactory
	 * @see #setEntityInterceptor
	 */
	public void setEntityInterceptorBeanName(String entityInterceptorBeanName) {
		this.entityInterceptor = entityInterceptorBeanName;
	}

	/**
	 * Set a Hibernate entity interceptor that allows to inspect and change
	 * property values before writing to and reading from the database.
	 * Will get applied to any new Session created by this transaction manager.
	 * <p>Such an interceptor can either be set at the SessionFactory level,
	 * i.e. on LocalSessionFactoryBean, or at the Session level, i.e. on
	 * HibernateTransactionManager.
	 * @see LocalSessionFactoryBean#setEntityInterceptor
	 */
	public void setEntityInterceptor(@Nullable Interceptor entityInterceptor) {
		this.entityInterceptor = entityInterceptor;
	}

	/**
	 * Return the current Hibernate entity interceptor, or {@code null} if none.
	 * Resolves an entity interceptor bean name via the bean factory,
	 * if necessary.
	 * @throws IllegalStateException if bean name specified but no bean factory set
	 * @throws BeansException if bean name resolution via the bean factory failed
	 * @see #setEntityInterceptor
	 * @see #setEntityInterceptorBeanName
	 * @see #setBeanFactory
	 */
	public @Nullable Interceptor getEntityInterceptor() throws IllegalStateException, BeansException {
		if (this.entityInterceptor instanceof Interceptor interceptor) {
			return interceptor;
		}
		else if (this.entityInterceptor instanceof String beanName) {
			if (this.beanFactory == null) {
				throw new IllegalStateException("Cannot get entity interceptor via bean name if no bean factory set");
			}
			return this.beanFactory.getBean(beanName, Interceptor.class);
		}
		else {
			return null;
		}
	}

	/**
	 * The bean factory just needs to be known for resolving entity interceptor
	 * bean names. It does not need to be set for any other mode of operation.
	 * @see #setEntityInterceptorBeanName
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("Property 'sessionFactory' is required");
		}
		if (this.entityInterceptor instanceof String && this.beanFactory == null) {
			throw new IllegalArgumentException("Property 'beanFactory' is required for 'entityInterceptorBeanName'");
		}

		// Check for SessionFactory's DataSource.
		if (this.autodetectDataSource && getDataSource() == null) {
			DataSource sfds = determineDataSource();
			if (sfds != null) {
				// Use the SessionFactory's DataSource for exposing transactions to JDBC code.
				if (logger.isDebugEnabled()) {
					logger.debug("Using DataSource [" + sfds +
							"] of Hibernate SessionFactory for HibernateTransactionManager");
				}
				setDataSource(sfds);
			}
		}
	}

	/**
	 * Determine the DataSource of the given SessionFactory.
	 * @return the DataSource, or {@code null} if none found
	 * @see ConnectionProvider
	 */
	protected @Nullable DataSource determineDataSource() {
		SessionFactory sessionFactory = obtainSessionFactory();
		Map<String, Object> props = sessionFactory.getProperties();
		if (props != null) {
			Object dataSourceValue = props.get(Environment.JAKARTA_NON_JTA_DATASOURCE);
			if (dataSourceValue instanceof DataSource dataSourceToUse) {
				return dataSourceToUse;
			}
		}
		if (sessionFactory instanceof SessionFactoryImplementor sfi) {
			try {
				ConnectionProvider cp = sfi.getServiceRegistry().getService(ConnectionProvider.class);
				if (cp != null) {
					return cp.unwrap(DataSource.class);
				}
			}
			catch (UnknownServiceException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("No ConnectionProvider found - cannot determine DataSource for SessionFactory: " + ex);
				}
			}
		}
		return null;
	}


	@Override
	public Object getResourceFactory() {
		return obtainSessionFactory();
	}

	@Override
	protected Object doGetTransaction() {
		HibernateTransactionObject txObject = new HibernateTransactionObject();
		txObject.setSavepointAllowed(isNestedTransactionAllowed());

		SessionFactory sessionFactory = obtainSessionFactory();
		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
		if (sessionHolder != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found thread-bound Session [" + sessionHolder.getSession() + "] for Hibernate transaction");
			}
			txObject.setSessionHolder(sessionHolder);
		}
		else if (this.hibernateManagedSession) {
			try {
				Session session = sessionFactory.getCurrentSession();
				if (logger.isDebugEnabled()) {
					logger.debug("Found Hibernate-managed Session [" + session + "] for Spring-managed transaction");
				}
				txObject.setExistingSession(session);
			}
			catch (HibernateException ex) {
				throw new DataAccessResourceFailureException(
						"Could not obtain Hibernate-managed Session for Spring-managed transaction", ex);
			}
		}

		if (getDataSource() != null) {
			ConnectionHolder conHolder = (ConnectionHolder)
					TransactionSynchronizationManager.getResource(getDataSource());
			txObject.setConnectionHolder(conHolder);
		}

		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;
		return (txObject.hasSpringManagedTransaction() ||
				(this.hibernateManagedSession && txObject.hasHibernateManagedTransaction()));
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;

		if (txObject.hasConnectionHolder() && !txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
			throw new IllegalTransactionStateException(
					"Pre-bound JDBC Connection found! HibernateTransactionManager does not support " +
					"running within DataSourceTransactionManager if told to manage the DataSource itself. " +
					"It is recommended to use a single HibernateTransactionManager for all transactions " +
					"on a single DataSource, no matter whether Hibernate or JDBC access.");
		}

		SessionImplementor session = null;

		try {
			if (!txObject.hasSessionHolder() || txObject.getSessionHolder().isSynchronizedWithTransaction()) {
				Interceptor entityInterceptor = getEntityInterceptor();
				Session newSession = (entityInterceptor != null ?
						obtainSessionFactory().withOptions().interceptor(entityInterceptor).openSession() :
						obtainSessionFactory().openSession());
				if (this.sessionInitializer != null) {
					this.sessionInitializer.accept(newSession);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Opened new Session [" + newSession + "] for Hibernate transaction");
				}
				txObject.setSession(newSession);
			}

			session = txObject.getSessionHolder().getSession().unwrap(SessionImplementor.class);

			boolean isolationLevelNeeded = (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT);
			if (isolationLevelNeeded || definition.isReadOnly()) {
				if (this.prepareConnection && ConnectionReleaseMode.ON_CLOSE.equals(
						session.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode().getReleaseMode())) {
					// We're allowed to change the transaction settings of the JDBC Connection.
					if (logger.isDebugEnabled()) {
						logger.debug("Preparing JDBC Connection of Hibernate Session [" + session + "]");
					}
					Connection con = session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
					Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
					txObject.setPreviousIsolationLevel(previousIsolationLevel);
					txObject.setReadOnly(definition.isReadOnly());
					txObject.connectionPrepared();
				}
				else {
					// Not allowed to change the transaction settings of the JDBC Connection.
					if (isolationLevelNeeded) {
						// We should set a specific isolation level but are not allowed to...
						throw new InvalidIsolationLevelException(
								"HibernateTransactionManager is not allowed to support custom isolation levels: " +
								"make sure that its 'prepareConnection' flag is on (the default) and that the " +
								"Hibernate connection release mode is set to ON_CLOSE.");
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Not preparing JDBC Connection of Hibernate Session [" + session + "]");
					}
				}
			}

			if (definition.isReadOnly() && txObject.isNewSession()) {
				// Just set to MANUAL in case of a new Session for this transaction.
				session.setHibernateFlushMode(FlushMode.MANUAL);
				// We're also setting Hibernate's read-only entity mode by default.
				session.setDefaultReadOnly(true);
			}

			if (!definition.isReadOnly() && !txObject.isNewSession()) {
				// We need AUTO or COMMIT for a non-read-only transaction.
				FlushMode flushMode = session.getHibernateFlushMode();
				if (FlushMode.MANUAL.equals(flushMode)) {
					session.setHibernateFlushMode(FlushMode.AUTO);
					txObject.getSessionHolder().setPreviousFlushMode(flushMode);
				}
			}

			Transaction hibTx;

			// Register transaction timeout.
			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				hibTx = session.getTransaction();
				hibTx.setTimeout(timeout);
				hibTx.begin();
			}
			else {
				// Open a plain Hibernate transaction without specified timeout.
				hibTx = session.beginTransaction();
			}

			// Add the Hibernate transaction to the session holder.
			txObject.getSessionHolder().setTransaction(hibTx);

			// Register the Hibernate Session's JDBC Connection for the DataSource, if set.
			if (getDataSource() != null) {
				final SessionImplementor sessionToUse = session;
				ConnectionHolder conHolder = new ConnectionHolder(
						() -> sessionToUse.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection());
				if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
					conHolder.setTimeoutInSeconds(timeout);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Exposing Hibernate transaction as JDBC [" + conHolder.getConnectionHandle() + "]");
				}
				TransactionSynchronizationManager.bindResource(getDataSource(), conHolder);
				txObject.setConnectionHolder(conHolder);
			}

			// Bind the session holder to the thread.
			if (txObject.isNewSessionHolder()) {
				TransactionSynchronizationManager.bindResource(obtainSessionFactory(), txObject.getSessionHolder());
			}
			txObject.getSessionHolder().setSynchronizedWithTransaction(true);
		}

		catch (Throwable ex) {
			if (txObject.isNewSession()) {
				try {
					if (session != null && session.getTransaction().getStatus() == TransactionStatus.ACTIVE) {
						session.getTransaction().rollback();
					}
				}
				catch (Throwable ex2) {
					logger.debug("Could not rollback Session after failed transaction begin", ex);
				}
				finally {
					EntityManagerFactoryUtils.closeEntityManager(session);
					txObject.setSessionHolder(null);
				}
			}
			throw new CannotCreateTransactionException("Could not open Hibernate Session for transaction", ex);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;
		txObject.setSessionHolder(null);
		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.unbindResource(obtainSessionFactory());
		txObject.setConnectionHolder(null);
		ConnectionHolder connectionHolder = null;
		if (getDataSource() != null) {
			connectionHolder = (ConnectionHolder) TransactionSynchronizationManager.unbindResource(getDataSource());
		}
		return new SuspendedResourcesHolder(sessionHolder, connectionHolder);
	}

	@Override
	protected void doResume(@Nullable Object transaction, Object suspendedResources) {
		SessionFactory sessionFactory = obtainSessionFactory();

		SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder) suspendedResources;
		if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
			// From non-transactional code running in active transaction synchronization
			// -> can be safely removed, will be closed on transaction completion.
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}
		TransactionSynchronizationManager.bindResource(sessionFactory, resourcesHolder.getSessionHolder());
		ConnectionHolder connectionHolder = resourcesHolder.getConnectionHolder();
		if (connectionHolder != null && getDataSource() != null) {
			TransactionSynchronizationManager.bindResource(getDataSource(), connectionHolder);
		}
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) status.getTransaction();
		Transaction hibTx = txObject.getSessionHolder().getTransaction();
		Assert.state(hibTx != null, "No Hibernate transaction");
		if (status.isDebug()) {
			logger.debug("Committing Hibernate transaction on Session [" +
					txObject.getSessionHolder().getSession() + "]");
		}

		try {
			hibTx.commit();
		}
		catch (org.hibernate.TransactionException ex) {
			DataAccessException dae = this.exceptionTranslator.translateExceptionIfPossible(ex);
			if (dae != null) {
				throw dae;
			}
			throw new TransactionSystemException("Could not commit Hibernate transaction", ex);
		}
		catch (RuntimeException ex) {
			// Assumably failed to flush changes to database.
			throw DataAccessUtils.translateIfNecessary(ex, this.exceptionTranslator);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) status.getTransaction();
		Transaction hibTx = txObject.getSessionHolder().getTransaction();
		Assert.state(hibTx != null, "No Hibernate transaction");
		if (status.isDebug()) {
			logger.debug("Rolling back Hibernate transaction on Session [" +
					txObject.getSessionHolder().getSession() + "]");
		}

		try {
			hibTx.rollback();
		}
		catch (org.hibernate.TransactionException ex) {
			DataAccessException dae = this.exceptionTranslator.translateExceptionIfPossible(ex);
			if (dae != null) {
				throw dae;
			}
			throw new TransactionSystemException("Could not roll back Hibernate transaction", ex);
		}
		catch (RuntimeException ex) {
			// Shouldn't really happen, as a rollback doesn't cause a flush.
			throw DataAccessUtils.translateIfNecessary(ex, this.exceptionTranslator);
		}
		finally {
			if (!txObject.isNewSession() && !this.hibernateManagedSession) {
				// Clear all pending inserts/updates/deletes in the Session.
				// Necessary for pre-bound Sessions, to avoid inconsistent state.
				txObject.getSessionHolder().getSession().clear();
			}
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting Hibernate transaction on Session [" +
					txObject.getSessionHolder().getSession() + "] rollback-only");
		}
		txObject.setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;

		// Remove the session holder from the thread.
		if (txObject.isNewSessionHolder()) {
			TransactionSynchronizationManager.unbindResource(obtainSessionFactory());
		}

		// Remove the JDBC connection holder from the thread, if exposed.
		if (getDataSource() != null) {
			TransactionSynchronizationManager.unbindResource(getDataSource());
		}

		SessionImplementor session = txObject.getSessionHolder().getSession().unwrap(SessionImplementor.class);
		if (txObject.needsConnectionReset() &&
				session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected()) {
			// We're running with connection release mode ON_CLOSE: We're able to reset
			// the isolation level and/or read-only flag of the JDBC Connection here.
			// Else, we need to rely on the connection pool to perform proper cleanup.
			try {
				Connection con = session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
				DataSourceUtils.resetConnectionAfterTransaction(
						con, txObject.getPreviousIsolationLevel(), txObject.isReadOnly());
			}
			catch (HibernateException ex) {
				logger.debug("Could not access JDBC Connection of Hibernate Session", ex);
			}
			catch (Throwable ex) {
				logger.debug("Could not reset JDBC Connection after transaction", ex);
			}
		}

		if (txObject.isNewSession()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing Hibernate Session [" + session + "] after transaction");
			}
			EntityManagerFactoryUtils.closeEntityManager(session);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Not closing pre-bound Hibernate Session [" + session + "] after transaction");
			}
			if (txObject.getSessionHolder().getPreviousFlushMode() != null) {
				session.setHibernateFlushMode(txObject.getSessionHolder().getPreviousFlushMode());
			}
			if (!this.hibernateManagedSession) {
				disconnectOnCompletion(session);
			}
		}
		txObject.getSessionHolder().clear();
	}

	/**
	 * Disconnect a pre-existing Hibernate Session on transaction completion,
	 * returning its database connection but preserving its entity state.
	 * <p>The default implementation triggers a manual disconnect. Subclasses
	 * may override this with a no-op or with fine-tuned disconnection logic.
	 * @param session the Hibernate Session to disconnect
	 */
	protected void disconnectOnCompletion(Session session) {
		if (session instanceof SessionImplementor sessionImpl) {
			sessionImpl.getJdbcCoordinator().getLogicalConnection().manualDisconnect();
		}
	}


	/**
	 * Hibernate transaction object, representing a SessionHolder.
	 * <p>Used as transaction object by HibernateTransactionManager.
	 */
	private class HibernateTransactionObject extends JdbcTransactionObjectSupport {

		private @Nullable SessionHolder sessionHolder;

		private boolean newSessionHolder;

		private boolean newSession;

		private boolean needsConnectionReset;

		public void setSession(Session session) {
			this.sessionHolder = new SessionHolder(session);
			this.newSessionHolder = true;
			this.newSession = true;
		}

		public void setExistingSession(Session session) {
			this.sessionHolder = new SessionHolder(session);
			this.newSessionHolder = true;
			this.newSession = false;
		}

		public void setSessionHolder(@Nullable SessionHolder sessionHolder) {
			this.sessionHolder = sessionHolder;
			this.newSessionHolder = false;
			this.newSession = false;
		}

		public SessionHolder getSessionHolder() {
			Assert.state(this.sessionHolder != null, "No SessionHolder available");
			return this.sessionHolder;
		}

		public boolean hasSessionHolder() {
			return (this.sessionHolder != null);
		}

		public boolean isNewSessionHolder() {
			return this.newSessionHolder;
		}

		public boolean isNewSession() {
			return this.newSession;
		}

		public void connectionPrepared() {
			this.needsConnectionReset = true;
		}

		public boolean needsConnectionReset() {
			return this.needsConnectionReset;
		}

		public boolean hasSpringManagedTransaction() {
			return (this.sessionHolder != null && this.sessionHolder.getTransaction() != null);
		}

		public boolean hasHibernateManagedTransaction() {
			return (this.sessionHolder != null &&
					this.sessionHolder.getSession().getTransaction().getStatus() == TransactionStatus.ACTIVE);
		}

		public void setRollbackOnly() {
			getSessionHolder().setRollbackOnly();
			if (hasConnectionHolder()) {
				getConnectionHolder().setRollbackOnly();
			}
		}

		@Override
		public boolean isRollbackOnly() {
			return getSessionHolder().isRollbackOnly() ||
					(hasConnectionHolder() && getConnectionHolder().isRollbackOnly());
		}

		@Override
		public void flush() {
			try {
				getSessionHolder().getSession().flush();
			}
			catch (RuntimeException ex) {
				throw DataAccessUtils.translateIfNecessary(ex, HibernateTransactionManager.this.exceptionTranslator);
			}
		}
	}


	/**
	 * Holder for suspended resources.
	 * Used internally by {@code doSuspend} and {@code doResume}.
	 */
	private static final class SuspendedResourcesHolder {

		private final SessionHolder sessionHolder;

		private final @Nullable ConnectionHolder connectionHolder;

		private SuspendedResourcesHolder(SessionHolder sessionHolder, @Nullable ConnectionHolder conHolder) {
			this.sessionHolder = sessionHolder;
			this.connectionHolder = conHolder;
		}

		private SessionHolder getSessionHolder() {
			return this.sessionHolder;
		}

		private @Nullable ConnectionHolder getConnectionHolder() {
			return this.connectionHolder;
		}
	}

}
