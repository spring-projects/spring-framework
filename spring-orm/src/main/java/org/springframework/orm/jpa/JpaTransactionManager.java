/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.orm.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.DelegatingTransactionDefinition;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager} implementation
 * for a single JPA {@link javax.persistence.EntityManagerFactory}. Binds a JPA
 * EntityManager from the specified factory to the thread, potentially allowing for
 * one thread-bound EntityManager per factory. {@link SharedEntityManagerCreator} and
 * {@code @PersistenceContext} are aware of thread-bound entity managers and participate
 * in such transactions automatically. Using either is required for JPA access code
 * supporting this transaction management mechanism.
 *
 * <p>This transaction manager is appropriate for applications that use a single
 * JPA EntityManagerFactory for transactional data access. JTA (usually through
 * {@link org.springframework.transaction.jta.JtaTransactionManager}) is necessary
 * for accessing multiple transactional resources within the same transaction.
 * Note that you need to configure your JPA provider accordingly in order to make
 * it participate in JTA transactions.
 *
 * <p>This transaction manager also supports direct DataSource access within a
 * transaction (i.e. plain JDBC code working with the same DataSource).
 * This allows for mixing services which access JPA and services which use plain
 * JDBC (without being aware of JPA)! Application code needs to stick to the
 * same simple Connection lookup pattern as with
 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}
 * (i.e. {@link org.springframework.jdbc.datasource.DataSourceUtils#getConnection}
 * or going through a
 * {@link org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy}).
 * Note that this requires a vendor-specific {@link JpaDialect} to be configured.
 *
 * <p>Note: To be able to register a DataSource's Connection for plain JDBC code,
 * this instance needs to be aware of the DataSource ({@link #setDataSource}).
 * The given DataSource should obviously match the one used by the given
 * EntityManagerFactory. This transaction manager will autodetect the DataSource
 * used as the connection factory of the EntityManagerFactory, so you usually
 * don't need to explicitly specify the "dataSource" property.
 *
 * <p>This transaction manager supports nested transactions via JDBC 3.0 Savepoints.
 * The {@link #setNestedTransactionAllowed "nestedTransactionAllowed"} flag defaults
 * to {@code false} though, since nested transactions will just apply to the JDBC
 * Connection, not to the JPA EntityManager and its cached entity objects and related
 * context. You can manually set the flag to {@code true} if you want to use nested
 * transactions for JDBC access code which participates in JPA transactions (provided
 * that your JDBC driver supports Savepoints). <i>Note that JPA itself does not support
 * nested transactions! Hence, do not expect JPA access code to semantically
 * participate in a nested transaction.</i>
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setEntityManagerFactory
 * @see #setDataSource
 * @see LocalEntityManagerFactoryBean
 * @see org.springframework.orm.jpa.support.SharedEntityManagerBean
 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
 * @see org.springframework.jdbc.core.JdbcTemplate
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 */
@SuppressWarnings("serial")
public class JpaTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {

	private EntityManagerFactory entityManagerFactory;

	private String persistenceUnitName;

	private final Map<String, Object> jpaPropertyMap = new HashMap<>();

	private DataSource dataSource;

	private JpaDialect jpaDialect = new DefaultJpaDialect();


	/**
	 * Create a new JpaTransactionManager instance.
	 * <p>An EntityManagerFactory has to be set to be able to use it.
	 * @see #setEntityManagerFactory
	 */
	public JpaTransactionManager() {
		setNestedTransactionAllowed(true);
	}

	/**
	 * Create a new JpaTransactionManager instance.
	 * @param emf EntityManagerFactory to manage transactions for
	 */
	public JpaTransactionManager(EntityManagerFactory emf) {
		this();
		this.entityManagerFactory = emf;
		afterPropertiesSet();
	}


	/**
	 * Set the EntityManagerFactory that this instance should manage transactions for.
	 * <p>Alternatively, specify the persistence unit name of the target EntityManagerFactory.
	 * By default, a default EntityManagerFactory will be retrieved by finding a
	 * single unique bean of type EntityManagerFactory in the containing BeanFactory.
	 * @see #setPersistenceUnitName
	 */
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.entityManagerFactory = emf;
	}

	/**
	 * Return the EntityManagerFactory that this instance should manage transactions for.
	 */
	public EntityManagerFactory getEntityManagerFactory() {
		return this.entityManagerFactory;
	}

	/**
	 * Set the name of the persistence unit to manage transactions for.
	 * <p>This is an alternative to specifying the EntityManagerFactory by direct reference,
	 * resolving it by its persistence unit name instead. If no EntityManagerFactory and
	 * no persistence unit name have been specified, a default EntityManagerFactory will
	 * be retrieved by finding a single unique bean of type EntityManagerFactory.
	 * @see #setEntityManagerFactory
	 */
	public void setPersistenceUnitName(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	/**
	 * Return the name of the persistence unit to manage transactions for, if any.
	 */
	public String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	/**
	 * Specify JPA properties, to be passed into
	 * {@code EntityManagerFactory.createEntityManager(Map)} (if any).
	 * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
	 * or a "props" element in XML bean definitions.
	 * @see javax.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	public void setJpaProperties(Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
	}

	/**
	 * Specify JPA properties as a Map, to be passed into
	 * {@code EntityManagerFactory.createEntityManager(Map)} (if any).
	 * <p>Can be populated with a "map" or "props" element in XML bean definitions.
	 * @see javax.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	public void setJpaPropertyMap(Map<String, ?> jpaProperties) {
		if (jpaProperties != null) {
			this.jpaPropertyMap.putAll(jpaProperties);
		}
	}

	/**
	 * Allow Map access to the JPA properties to be passed to the persistence
	 * provider, with the option to add or override specific entries.
	 * <p>Useful for specifying entries directly, for example via "jpaPropertyMap[myKey]".
	 */
	public Map<String, Object> getJpaPropertyMap() {
		return this.jpaPropertyMap;
	}

	/**
	 * Set the JDBC DataSource that this instance should manage transactions for.
	 * The DataSource should match the one used by the JPA EntityManagerFactory:
	 * for example, you could specify the same JNDI DataSource for both.
	 * <p>If the EntityManagerFactory uses a known DataSource as its connection factory,
	 * the DataSource will be autodetected: You can still explicitly specify the
	 * DataSource, but you don't need to in this case.
	 * <p>A transactional JDBC Connection for this DataSource will be provided to
	 * application code accessing this DataSource directly via DataSourceUtils
	 * or JdbcTemplate. The Connection will be taken from the JPA EntityManager.
	 * <p>Note that you need to use a JPA dialect for a specific JPA implementation
	 * to allow for exposing JPA transactions as JDBC transactions.
	 * <p>The DataSource specified here should be the target DataSource to manage
	 * transactions for, not a TransactionAwareDataSourceProxy. Only data access
	 * code may work with TransactionAwareDataSourceProxy, while the transaction
	 * manager needs to work on the underlying target DataSource. If there's
	 * nevertheless a TransactionAwareDataSourceProxy passed in, it will be
	 * unwrapped to extract its target DataSource.
	 * @see EntityManagerFactoryInfo#getDataSource()
	 * @see #setJpaDialect
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
	 * Set the JPA dialect to use for this transaction manager.
	 * Used for vendor-specific transaction management and JDBC connection exposure.
	 * <p>If the EntityManagerFactory uses a known JpaDialect, it will be autodetected:
	 * You can still explicitly specify the DataSource, but you don't need to in this case.
	 * <p>The dialect object can be used to retrieve the underlying JDBC connection
	 * and thus allows for exposing JPA transactions as JDBC transactions.
	 * @see EntityManagerFactoryInfo#getJpaDialect()
	 * @see JpaDialect#beginTransaction
	 * @see JpaDialect#getJdbcConnection
	 */
	public void setJpaDialect(JpaDialect jpaDialect) {
		this.jpaDialect = (jpaDialect != null ? jpaDialect : new DefaultJpaDialect());
	}

	/**
	 * Return the JPA dialect to use for this transaction manager.
	 */
	public JpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	/**
	 * Retrieves an EntityManagerFactory by persistence unit name, if none set explicitly.
	 * Falls back to a default EntityManagerFactory bean if no persistence unit specified.
	 * @see #setPersistenceUnitName
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (getEntityManagerFactory() == null) {
			if (!(beanFactory instanceof ListableBeanFactory)) {
				throw new IllegalStateException("Cannot retrieve EntityManagerFactory by persistence unit name " +
						"in a non-listable BeanFactory: " + beanFactory);
			}
			ListableBeanFactory lbf = (ListableBeanFactory) beanFactory;
			setEntityManagerFactory(EntityManagerFactoryUtils.findEntityManagerFactory(lbf, getPersistenceUnitName()));
		}
	}

	/**
	 * Eagerly initialize the JPA dialect, creating a default one
	 * for the specified EntityManagerFactory if none set.
	 * Auto-detect the EntityManagerFactory's DataSource, if any.
	 */
	@Override
	public void afterPropertiesSet() {
		if (getEntityManagerFactory() == null) {
			throw new IllegalArgumentException("'entityManagerFactory' or 'persistenceUnitName' is required");
		}
		if (getEntityManagerFactory() instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) getEntityManagerFactory();
			DataSource dataSource = emfInfo.getDataSource();
			if (dataSource != null) {
				setDataSource(dataSource);
			}
			JpaDialect jpaDialect = emfInfo.getJpaDialect();
			if (jpaDialect != null) {
				setJpaDialect(jpaDialect);
			}
		}
	}


	@Override
	public Object getResourceFactory() {
		return getEntityManagerFactory();
	}

	@Override
	protected Object doGetTransaction() {
		JpaTransactionObject txObject = new JpaTransactionObject();
		txObject.setSavepointAllowed(isNestedTransactionAllowed());

		EntityManagerHolder emHolder = (EntityManagerHolder)
				TransactionSynchronizationManager.getResource(getEntityManagerFactory());
		if (emHolder != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found thread-bound EntityManager [" + emHolder.getEntityManager() +
						"] for JPA transaction");
			}
			txObject.setEntityManagerHolder(emHolder, false);
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
		return ((JpaTransactionObject) transaction).hasTransaction();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		JpaTransactionObject txObject = (JpaTransactionObject) transaction;

		if (txObject.hasConnectionHolder() && !txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
			throw new IllegalTransactionStateException(
					"Pre-bound JDBC Connection found! JpaTransactionManager does not support " +
					"running within DataSourceTransactionManager if told to manage the DataSource itself. " +
					"It is recommended to use a single JpaTransactionManager for all transactions " +
					"on a single DataSource, no matter whether JPA or JDBC access.");
		}

		try {
			if (txObject.getEntityManagerHolder() == null ||
					txObject.getEntityManagerHolder().isSynchronizedWithTransaction()) {
				EntityManager newEm = createEntityManagerForTransaction();
				if (logger.isDebugEnabled()) {
					logger.debug("Opened new EntityManager [" + newEm + "] for JPA transaction");
				}
				txObject.setEntityManagerHolder(new EntityManagerHolder(newEm), true);
			}

			EntityManager em = txObject.getEntityManagerHolder().getEntityManager();

			// Delegate to JpaDialect for actual transaction begin.
			final int timeoutToUse = determineTimeout(definition);
			Object transactionData = getJpaDialect().beginTransaction(em,
					new DelegatingTransactionDefinition(definition) {
						@Override
						public int getTimeout() {
							return timeoutToUse;
						}
					});
			txObject.setTransactionData(transactionData);

			// Register transaction timeout.
			if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getEntityManagerHolder().setTimeoutInSeconds(timeoutToUse);
			}

			// Register the JPA EntityManager's JDBC Connection for the DataSource, if set.
			if (getDataSource() != null) {
				ConnectionHandle conHandle = getJpaDialect().getJdbcConnection(em, definition.isReadOnly());
				if (conHandle != null) {
					ConnectionHolder conHolder = new ConnectionHolder(conHandle);
					if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
						conHolder.setTimeoutInSeconds(timeoutToUse);
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Exposing JPA transaction as JDBC transaction [" +
								conHolder.getConnectionHandle() + "]");
					}
					TransactionSynchronizationManager.bindResource(getDataSource(), conHolder);
					txObject.setConnectionHolder(conHolder);
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Not exposing JPA transaction [" + em + "] as JDBC transaction because " +
								"JpaDialect [" + getJpaDialect() + "] does not support JDBC Connection retrieval");
					}
				}
			}

			// Bind the entity manager holder to the thread.
			if (txObject.isNewEntityManagerHolder()) {
				TransactionSynchronizationManager.bindResource(
						getEntityManagerFactory(), txObject.getEntityManagerHolder());
			}
			txObject.getEntityManagerHolder().setSynchronizedWithTransaction(true);
		}

		catch (TransactionException ex) {
			closeEntityManagerAfterFailedBegin(txObject);
			throw ex;
		}
		catch (Throwable ex) {
			closeEntityManagerAfterFailedBegin(txObject);
			throw new CannotCreateTransactionException("Could not open JPA EntityManager for transaction", ex);
		}
	}

	/**
	 * Create a JPA EntityManager to be used for a transaction.
	 * <p>The default implementation checks whether the EntityManagerFactory
	 * is a Spring proxy and unwraps it first.
	 * @see javax.persistence.EntityManagerFactory#createEntityManager()
	 * @see EntityManagerFactoryInfo#getNativeEntityManagerFactory()
	 */
	protected EntityManager createEntityManagerForTransaction() {
		EntityManagerFactory emf = getEntityManagerFactory();
		if (emf instanceof EntityManagerFactoryInfo) {
			emf = ((EntityManagerFactoryInfo) emf).getNativeEntityManagerFactory();
		}
		Map<String, Object> properties = getJpaPropertyMap();
		return (!CollectionUtils.isEmpty(properties) ?
				emf.createEntityManager(properties) : emf.createEntityManager());
	}

	/**
	 * Close the current transaction's EntityManager.
	 * Called after a transaction begin attempt failed.
	 * @param txObject the current transaction
	 */
	protected void closeEntityManagerAfterFailedBegin(JpaTransactionObject txObject) {
		if (txObject.isNewEntityManagerHolder()) {
			EntityManager em = txObject.getEntityManagerHolder().getEntityManager();
			try {
				if (em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				}
			}
			catch (Throwable ex) {
				logger.debug("Could not rollback EntityManager after failed transaction begin", ex);
			}
			finally {
				EntityManagerFactoryUtils.closeEntityManager(em);
			}
			txObject.setEntityManagerHolder(null, false);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		JpaTransactionObject txObject = (JpaTransactionObject) transaction;
		txObject.setEntityManagerHolder(null, false);
		EntityManagerHolder entityManagerHolder = (EntityManagerHolder)
				TransactionSynchronizationManager.unbindResource(getEntityManagerFactory());
		txObject.setConnectionHolder(null);
		ConnectionHolder connectionHolder = null;
		if (getDataSource() != null && TransactionSynchronizationManager.hasResource(getDataSource())) {
			connectionHolder = (ConnectionHolder) TransactionSynchronizationManager.unbindResource(getDataSource());
		}
		return new SuspendedResourcesHolder(entityManagerHolder, connectionHolder);
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder) suspendedResources;
		TransactionSynchronizationManager.bindResource(
				getEntityManagerFactory(), resourcesHolder.getEntityManagerHolder());
		if (getDataSource() != null && resourcesHolder.getConnectionHolder() != null) {
			TransactionSynchronizationManager.bindResource(getDataSource(), resourcesHolder.getConnectionHolder());
		}
	}

	/**
	 * This implementation returns "true": a JPA commit will properly handle
	 * transactions that have been marked rollback-only at a global level.
	 */
	@Override
	protected boolean shouldCommitOnGlobalRollbackOnly() {
		return true;
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Committing JPA transaction on EntityManager [" +
					txObject.getEntityManagerHolder().getEntityManager() + "]");
		}
		try {
			EntityTransaction tx = txObject.getEntityManagerHolder().getEntityManager().getTransaction();
			tx.commit();
		}
		catch (RollbackException ex) {
			if (ex.getCause() instanceof RuntimeException) {
				DataAccessException dex = getJpaDialect().translateExceptionIfPossible((RuntimeException) ex.getCause());
				if (dex != null) {
					throw dex;
				}
			}
			throw new TransactionSystemException("Could not commit JPA transaction", ex);
		}
		catch (RuntimeException ex) {
			// Assumably failed to flush changes to database.
			throw DataAccessUtils.translateIfNecessary(ex, getJpaDialect());
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Rolling back JPA transaction on EntityManager [" +
					txObject.getEntityManagerHolder().getEntityManager() + "]");
		}
		try {
			EntityTransaction tx = txObject.getEntityManagerHolder().getEntityManager().getTransaction();
			if (tx.isActive()) {
				tx.rollback();
			}
		}
		catch (PersistenceException ex) {
			throw new TransactionSystemException("Could not roll back JPA transaction", ex);
		}
		finally {
			if (!txObject.isNewEntityManagerHolder()) {
				// Clear all pending inserts/updates/deletes in the EntityManager.
				// Necessary for pre-bound EntityManagers, to avoid inconsistent state.
				txObject.getEntityManagerHolder().getEntityManager().clear();
			}
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting JPA transaction on EntityManager [" +
					txObject.getEntityManagerHolder().getEntityManager() + "] rollback-only");
		}
		txObject.setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		JpaTransactionObject txObject = (JpaTransactionObject) transaction;

		// Remove the entity manager holder from the thread, if still there.
		// (Could have been removed by EntityManagerFactoryUtils in order
		// to replace it with an unsynchronized EntityManager).
		if (txObject.isNewEntityManagerHolder()) {
			TransactionSynchronizationManager.unbindResourceIfPossible(getEntityManagerFactory());
		}
		txObject.getEntityManagerHolder().clear();

		// Remove the JDBC connection holder from the thread, if exposed.
		if (txObject.hasConnectionHolder()) {
			TransactionSynchronizationManager.unbindResource(getDataSource());
			try {
				getJpaDialect().releaseJdbcConnection(txObject.getConnectionHolder().getConnectionHandle(),
						txObject.getEntityManagerHolder().getEntityManager());
			}
			catch (Exception ex) {
				// Just log it, to keep a transaction-related exception.
				logger.error("Could not close JDBC connection after transaction", ex);
			}
		}

		getJpaDialect().cleanupTransaction(txObject.getTransactionData());

		// Remove the entity manager holder from the thread.
		if (txObject.isNewEntityManagerHolder()) {
			EntityManager em = txObject.getEntityManagerHolder().getEntityManager();
			if (logger.isDebugEnabled()) {
				logger.debug("Closing JPA EntityManager [" + em + "] after transaction");
			}
			EntityManagerFactoryUtils.closeEntityManager(em);
		}
		else {
			logger.debug("Not closing pre-bound JPA EntityManager after transaction");
		}
	}


	/**
	 * JPA transaction object, representing a EntityManagerHolder.
	 * Used as transaction object by JpaTransactionManager.
	 */
	private class JpaTransactionObject extends JdbcTransactionObjectSupport {

		private EntityManagerHolder entityManagerHolder;

		private boolean newEntityManagerHolder;

		private Object transactionData;

		public void setEntityManagerHolder(
				EntityManagerHolder entityManagerHolder, boolean newEntityManagerHolder) {
			this.entityManagerHolder = entityManagerHolder;
			this.newEntityManagerHolder = newEntityManagerHolder;
		}

		public EntityManagerHolder getEntityManagerHolder() {
			return this.entityManagerHolder;
		}

		public boolean isNewEntityManagerHolder() {
			return this.newEntityManagerHolder;
		}

		public boolean hasTransaction() {
			return (this.entityManagerHolder != null && this.entityManagerHolder.isTransactionActive());
		}

		public void setTransactionData(Object transactionData) {
			this.transactionData = transactionData;
			this.entityManagerHolder.setTransactionActive(true);
			if (transactionData instanceof SavepointManager) {
				this.entityManagerHolder.setSavepointManager((SavepointManager) transactionData);
			}
		}

		public Object getTransactionData() {
			return this.transactionData;
		}

		public void setRollbackOnly() {
			EntityTransaction tx = this.entityManagerHolder.getEntityManager().getTransaction();
			if (tx.isActive()) {
				tx.setRollbackOnly();
			}
			if (hasConnectionHolder()) {
				getConnectionHolder().setRollbackOnly();
			}
		}

		@Override
		public boolean isRollbackOnly() {
			EntityTransaction tx = this.entityManagerHolder.getEntityManager().getTransaction();
			return tx.getRollbackOnly();
		}

		@Override
		public void flush() {
			try {
				this.entityManagerHolder.getEntityManager().flush();
			}
			catch (RuntimeException ex) {
				throw DataAccessUtils.translateIfNecessary(ex, getJpaDialect());
			}
		}

		@Override
		public Object createSavepoint() throws TransactionException {
			if (this.entityManagerHolder.isRollbackOnly()) {
				throw new CannotCreateTransactionException(
						"Cannot create savepoint for transaction which is already marked as rollback-only");
			}
			return getSavepointManager().createSavepoint();
		}

		@Override
		public void rollbackToSavepoint(Object savepoint) throws TransactionException {
			getSavepointManager().rollbackToSavepoint(savepoint);
			this.entityManagerHolder.resetRollbackOnly();
		}

		@Override
		public void releaseSavepoint(Object savepoint) throws TransactionException {
			getSavepointManager().releaseSavepoint(savepoint);
		}

		private SavepointManager getSavepointManager() {
			if (!isSavepointAllowed()) {
				throw new NestedTransactionNotSupportedException(
						"Transaction manager does not allow nested transactions");
			}
			SavepointManager savepointManager = getEntityManagerHolder().getSavepointManager();
			if (savepointManager == null) {
				throw new NestedTransactionNotSupportedException(
						"JpaDialect does not support savepoints - check your JPA provider's capabilities");
			}
			return savepointManager;
		}
	}


	/**
	 * Holder for suspended resources.
	 * Used internally by {@code doSuspend} and {@code doResume}.
	 */
	private static class SuspendedResourcesHolder {

		private final EntityManagerHolder entityManagerHolder;

		private final ConnectionHolder connectionHolder;

		private SuspendedResourcesHolder(EntityManagerHolder emHolder, ConnectionHolder conHolder) {
			this.entityManagerHolder = emHolder;
			this.connectionHolder = conHolder;
		}

		private EntityManagerHolder getEntityManagerHolder() {
			return this.entityManagerHolder;
		}

		private ConnectionHolder getConnectionHolder() {
			return this.connectionHolder;
		}
	}

}
