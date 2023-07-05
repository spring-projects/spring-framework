/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.r2dbc.connection;

import java.time.Duration;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.IsolationLevel;
import io.r2dbc.spi.Option;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.Result;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.AbstractReactiveTransactionManager;
import org.springframework.transaction.reactive.GenericReactiveTransaction;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.transaction.ReactiveTransactionManager} implementation
 * for a single R2DBC {@link ConnectionFactory}. This class is capable of working
 * in any environment with any R2DBC driver, as long as the setup uses a
 * {@code ConnectionFactory} as its {@link Connection} factory mechanism.
 * Binds a R2DBC {@code Connection} from the specified {@code ConnectionFactory}
 * to the current subscriber context, potentially allowing for one context-bound
 * {@code Connection} per {@code ConnectionFactory}.
 *
 * <p><b>Note: The {@code ConnectionFactory} that this transaction manager operates
 * on needs to return independent {@code Connection}s.</b> The {@code Connection}s
 * typically come from a connection pool but the {@code ConnectionFactory} must not
 * return specifically scoped or constrained {@code Connection}s. This transaction
 * manager will associate {@code Connection} with context-bound transactions,
 * according to the specified propagation behavior. It assumes that a separate,
 * independent {@code Connection} can be obtained even during an ongoing transaction.
 *
 * <p>Application code is required to retrieve the R2DBC Connection via
 * {@link ConnectionFactoryUtils#getConnection(ConnectionFactory)}
 * instead of a standard R2DBC-style {@link ConnectionFactory#create()} call.
 * Spring classes such as {@code DatabaseClient} use this strategy implicitly.
 * If not used in combination with this transaction manager, the
 * {@link ConnectionFactoryUtils} lookup strategy behaves exactly like the native
 * {@code ConnectionFactory} lookup; it can thus be used in a portable fashion.
 *
 * <p>Alternatively, you can allow application code to work with the lookup pattern
 * {@link ConnectionFactory#create()}, for example for code not aware of Spring.
 * In that case, define a {@link TransactionAwareConnectionFactoryProxy} for your
 * target {@code ConnectionFactory}, and pass that proxy {@code ConnectionFactory}
 * to your DAOs which will automatically participate in Spring-managed transactions
 * when accessing it.
 *
 * <p>Spring's {@code TransactionDefinition} attributes are carried forward to
 * R2DBC drivers using extensible R2DBC {@link io.r2dbc.spi.TransactionDefinition}.
 * Subclasses may override {@link #createTransactionDefinition(TransactionDefinition)}
 * to customize transaction definitions for vendor-specific attributes. As of 6.0.10,
 * this transaction manager supports nested transactions via R2DBC savepoints as well.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.3
 * @see ConnectionFactoryUtils#getConnection(ConnectionFactory)
 * @see ConnectionFactoryUtils#releaseConnection
 * @see TransactionAwareConnectionFactoryProxy
 */
@SuppressWarnings("serial")
public class R2dbcTransactionManager extends AbstractReactiveTransactionManager implements InitializingBean {

	@Nullable
	private ConnectionFactory connectionFactory;

	private boolean enforceReadOnly = false;


	/**
	 * Create a new {@code R2dbcTransactionManager} instance.
	 * A {@code ConnectionFactory} has to be set to be able to use it.
	 * @see #setConnectionFactory
	 */
	public R2dbcTransactionManager() {}

	/**
	 * Create a new {@code R2dbcTransactionManager} instance.
	 * @param connectionFactory the R2DBC ConnectionFactory to manage transactions for
	 */
	public R2dbcTransactionManager(ConnectionFactory connectionFactory) {
		this();
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}


	/**
	 * Set the R2DBC {@link ConnectionFactory} that this instance should manage transactions
	 * for. This will typically be a locally defined {@code ConnectionFactory}, for example
	 * an R2DBC connection pool.
	 * <p><b>The {@code ConnectionFactory} passed in here needs to return independent
	 * {@link Connection}s.</b> The {@code Connection}s typically come from a connection
	 * pool but the {@code ConnectionFactory} must not return specifically scoped or
	 * constrained {@code Connection}s.
	 */
	public void setConnectionFactory(@Nullable ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Return the R2DBC {@link ConnectionFactory} that this instance manages transactions for.
	 */
	@Nullable
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * Obtain the {@link ConnectionFactory} for actual use.
	 * @return the {@code ConnectionFactory} (never {@code null})
	 * @throws IllegalStateException in case of no ConnectionFactory set
	 */
	protected ConnectionFactory obtainConnectionFactory() {
		ConnectionFactory connectionFactory = getConnectionFactory();
		Assert.state(connectionFactory != null, "No ConnectionFactory set");
		return connectionFactory;
	}

	/**
	 * Specify whether to enforce the read-only nature of a transaction (as indicated by
	 * {@link TransactionDefinition#isReadOnly()}) through an explicit statement on the
	 * transactional connection: "SET TRANSACTION READ ONLY" as understood by Oracle,
	 * MySQL and Postgres.
	 * <p>The exact treatment, including any SQL statement executed on the connection,
	 * can be customized through {@link #prepareTransactionalConnection}.
	 * @see #prepareTransactionalConnection
	 */
	public void setEnforceReadOnly(boolean enforceReadOnly) {
		this.enforceReadOnly = enforceReadOnly;
	}

	/**
	 * Return whether to enforce the read-only nature of a transaction through an
	 * explicit statement on the transactional connection.
	 * @see #setEnforceReadOnly
	 */
	public boolean isEnforceReadOnly() {
		return this.enforceReadOnly;
	}

	@Override
	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}

	@Override
	protected Object doGetTransaction(TransactionSynchronizationManager synchronizationManager) {
		ConnectionFactoryTransactionObject txObject = new ConnectionFactoryTransactionObject();
		ConnectionHolder conHolder = (ConnectionHolder) synchronizationManager.getResource(obtainConnectionFactory());
		txObject.setConnectionHolder(conHolder, false);
		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		return ((ConnectionFactoryTransactionObject) transaction).isTransactionActive();
	}

	@Override
	protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager, Object transaction,
			TransactionDefinition definition) {

		ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) transaction;

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED &&
				txObject.isTransactionActive())  {
			return txObject.createSavepoint();
		}

		return Mono.defer(() -> {
			Mono<Connection> connectionMono;

			if (!txObject.hasConnectionHolder() || txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
				Mono<Connection> newCon = Mono.from(obtainConnectionFactory().create());
				connectionMono = newCon.doOnNext(connection -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Acquired Connection [" + connection + "] for R2DBC transaction");
					}
					txObject.setConnectionHolder(new ConnectionHolder(connection), true);
				});
			}
			else {
				txObject.getConnectionHolder().setSynchronizedWithTransaction(true);
				connectionMono = Mono.just(txObject.getConnectionHolder().getConnection());
			}

			return connectionMono.flatMap(con -> doBegin(definition, con)
					.then(prepareTransactionalConnection(con, definition))
					.doOnSuccess(v -> {
						txObject.getConnectionHolder().setTransactionActive(true);
						Duration timeout = determineTimeout(definition);
						if (!timeout.isNegative() && !timeout.isZero()) {
							txObject.getConnectionHolder().setTimeoutInMillis(timeout.toMillis());
						}
						// Bind the connection holder to the thread.
						if (txObject.isNewConnectionHolder()) {
							synchronizationManager.bindResource(obtainConnectionFactory(), txObject.getConnectionHolder());
						}
					}).thenReturn(con).onErrorResume(ex -> {
						if (txObject.isNewConnectionHolder()) {
							return ConnectionFactoryUtils.releaseConnection(con, obtainConnectionFactory())
									.doOnTerminate(() -> txObject.setConnectionHolder(null, false))
									.then(Mono.error(ex));
						}
						return Mono.error(ex);
					})).onErrorResume(ex -> Mono.error(new CannotCreateTransactionException(
							"Could not open R2DBC Connection for transaction", ex)));
		}).then();
	}

	private Mono<Void> doBegin(TransactionDefinition definition, Connection con) {
		io.r2dbc.spi.TransactionDefinition transactionDefinition = createTransactionDefinition(definition);
		if (logger.isDebugEnabled()) {
			logger.debug("Starting R2DBC transaction on Connection [" + con + "] using [" + transactionDefinition + "]");
		}
		return Mono.from(con.beginTransaction(transactionDefinition));
	}

	/**
	 * Determine the transaction definition from our {@code TransactionDefinition}.
	 * Can be overridden to wrap the R2DBC {@code TransactionDefinition} to adjust or
	 * enhance transaction attributes.
	 * @param definition the transaction definition
	 * @return the actual transaction definition to use
	 * @since 6.0
	 * @see io.r2dbc.spi.TransactionDefinition
	 */
	protected io.r2dbc.spi.TransactionDefinition createTransactionDefinition(TransactionDefinition definition) {
		// Apply specific isolation level, if any.
		IsolationLevel isolationLevelToUse = resolveIsolationLevel(definition.getIsolationLevel());
		return new ExtendedTransactionDefinition(definition.getName(), definition.isReadOnly(),
				definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ? isolationLevelToUse : null,
				determineTimeout(definition));
	}

	/**
	 * Determine the actual timeout to use for the given definition.
	 * Will fall back to this manager's default timeout if the
	 * transaction definition doesn't specify a non-default value.
	 * @param definition the transaction definition
	 * @return the actual timeout to use
	 * @see org.springframework.transaction.TransactionDefinition#getTimeout()
	 */
	protected Duration determineTimeout(TransactionDefinition definition) {
		if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
			return Duration.ofSeconds(definition.getTimeout());
		}
		return Duration.ZERO;
	}

	@Override
	protected Mono<Object> doSuspend(TransactionSynchronizationManager synchronizationManager, Object transaction) {
		return Mono.defer(() -> {
			ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) transaction;
			txObject.setConnectionHolder(null);
			return Mono.justOrEmpty(synchronizationManager.unbindResource(obtainConnectionFactory()));
		});
	}

	@Override
	protected Mono<Void> doResume(TransactionSynchronizationManager synchronizationManager,
			@Nullable Object transaction, Object suspendedResources) {

		return Mono.defer(() -> {
			synchronizationManager.bindResource(obtainConnectionFactory(), suspendedResources);
			return Mono.empty();
		});
	}

	@Override
	protected Mono<Void> doCommit(TransactionSynchronizationManager TransactionSynchronizationManager,
			GenericReactiveTransaction status) {

		ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Committing R2DBC transaction on Connection [" +
					txObject.getConnectionHolder().getConnection() + "]");
		}
		return txObject.commit().onErrorMap(R2dbcException.class, ex -> translateException("R2DBC commit", ex));
	}

	@Override
	protected Mono<Void> doRollback(TransactionSynchronizationManager TransactionSynchronizationManager,
			GenericReactiveTransaction status) {

		ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Rolling back R2DBC transaction on Connection [" +
					txObject.getConnectionHolder().getConnection() + "]");
		}
		return txObject.rollback().onErrorMap(R2dbcException.class, ex -> translateException("R2DBC rollback", ex));
	}

	@Override
	protected Mono<Void> doSetRollbackOnly(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		return Mono.fromRunnable(() -> {
			ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) status.getTransaction();
			if (status.isDebug()) {
				logger.debug("Setting R2DBC transaction [" + txObject.getConnectionHolder().getConnection() +
						"] rollback-only");
			}
			txObject.setRollbackOnly();
		});
	}

	@Override
	protected Mono<Void> doCleanupAfterCompletion(TransactionSynchronizationManager synchronizationManager,
			Object transaction) {

		return Mono.defer(() -> {
			ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) transaction;

			// Remove the connection holder from the context, if exposed.
			if (txObject.isNewConnectionHolder()) {
				synchronizationManager.unbindResource(obtainConnectionFactory());
			}

			// Reset connection.
			Connection con = txObject.getConnectionHolder().getConnection();

			Mono<Void> afterCleanup = Mono.empty();

			Mono<Void> releaseConnectionStep = Mono.defer(() -> {
				try {
					if (txObject.isNewConnectionHolder()) {
						if (logger.isDebugEnabled()) {
							logger.debug("Releasing R2DBC Connection [" + con + "] after transaction");
						}
						Mono<Void> releaseMono = ConnectionFactoryUtils.releaseConnection(con, obtainConnectionFactory());
						if (logger.isDebugEnabled()) {
							releaseMono = releaseMono.doOnError(
									ex -> logger.debug(String.format("Error ignored during cleanup: %s", ex)));
						}
						return releaseMono.onErrorComplete();
					}
				}
				finally {
					txObject.getConnectionHolder().clear();
				}
				return Mono.empty();
			});
			return afterCleanup.then(releaseConnectionStep);
		});
	}

	/**
	 * Prepare the transactional {@link Connection} right after transaction begin.
	 * <p>The default implementation executes a "SET TRANSACTION READ ONLY" statement if the
	 * {@link #setEnforceReadOnly "enforceReadOnly"} flag is set to {@code true} and the
	 * transaction definition indicates a read-only transaction.
	 * <p>The "SET TRANSACTION READ ONLY" is understood by Oracle, MySQL and Postgres
	 * and may work with other databases as well. If you'd like to adapt this treatment,
	 * override this method accordingly.
	 * @param con the transactional R2DBC Connection
	 * @param definition the current transaction definition
	 * @since 5.3.22
	 * @see #setEnforceReadOnly
	 */
	protected Mono<Void> prepareTransactionalConnection(Connection con, TransactionDefinition definition) {
		Mono<Void> prepare = Mono.empty();
		if (isEnforceReadOnly() && definition.isReadOnly()) {
			prepare = Mono.from(con.createStatement("SET TRANSACTION READ ONLY").execute())
					.flatMapMany(Result::getRowsUpdated)
					.then();
		}
		return prepare;
	}

	/**
	 * Resolve the {@linkplain TransactionDefinition#getIsolationLevel() isolation level constant} to a R2DBC
	 * {@link IsolationLevel}. If you'd like to extend isolation level translation for vendor-specific
	 * {@code IsolationLevel}s, override this method accordingly.
	 * @param isolationLevel the isolation level to translate.
	 * @return the resolved isolation level. Can be {@code null} if not resolvable or the isolation level
	 * should remain {@link TransactionDefinition#ISOLATION_DEFAULT default}.
	 * @see TransactionDefinition#getIsolationLevel()
	 */
	@Nullable
	protected IsolationLevel resolveIsolationLevel(int isolationLevel) {
		return switch (isolationLevel) {
			case TransactionDefinition.ISOLATION_READ_COMMITTED -> IsolationLevel.READ_COMMITTED;
			case TransactionDefinition.ISOLATION_READ_UNCOMMITTED -> IsolationLevel.READ_UNCOMMITTED;
			case TransactionDefinition.ISOLATION_REPEATABLE_READ -> IsolationLevel.REPEATABLE_READ;
			case TransactionDefinition.ISOLATION_SERIALIZABLE -> IsolationLevel.SERIALIZABLE;
			default -> null;
		};
	}

	/**
	 * Translate the given R2DBC commit/rollback exception to a common Spring exception to propagate
	 * from the {@link #commit}/{@link #rollback} call.
	 * @param task the task description (commit or rollback).
	 * @param ex the SQLException thrown from commit/rollback.
	 * @return the translated exception to emit
	 */
	protected RuntimeException translateException(String task, R2dbcException ex) {
		return ConnectionFactoryUtils.convertR2dbcException(task, null, ex);
	}


	/**
	 * Extended R2DBC transaction definition object providing transaction attributes
	 * to R2DBC drivers when starting a transaction.
	 */
	private record ExtendedTransactionDefinition(@Nullable String transactionName,
			boolean readOnly, @Nullable IsolationLevel isolationLevel, Duration lockWaitTimeout)
			implements io.r2dbc.spi.TransactionDefinition {

		private ExtendedTransactionDefinition(@Nullable String transactionName, boolean readOnly,
				@Nullable IsolationLevel isolationLevel, Duration lockWaitTimeout) {

			this.transactionName = transactionName;
			this.readOnly = readOnly;
			this.isolationLevel = isolationLevel;
			this.lockWaitTimeout = lockWaitTimeout;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getAttribute(Option<T> option) {
			return (T) doGetValue(option);
		}

		@Nullable
		private Object doGetValue(Option<?> option) {
			if (io.r2dbc.spi.TransactionDefinition.ISOLATION_LEVEL.equals(option)) {
				return this.isolationLevel;
			}
			if (io.r2dbc.spi.TransactionDefinition.NAME.equals(option)) {
				return this.transactionName;
			}
			if (io.r2dbc.spi.TransactionDefinition.READ_ONLY.equals(option)) {
				return this.readOnly;
			}
			if (io.r2dbc.spi.TransactionDefinition.LOCK_WAIT_TIMEOUT.equals(option)
				&& !this.lockWaitTimeout.isZero()) {
				return this.lockWaitTimeout;
			}
			return null;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(getClass().getSimpleName());
			sb.append(" [transactionName='").append(this.transactionName).append('\'');
			sb.append(", readOnly=").append(this.readOnly);
			sb.append(", isolationLevel=").append(this.isolationLevel);
			sb.append(", lockWaitTimeout=").append(this.lockWaitTimeout);
			sb.append(']');
			return sb.toString();
		}
	}


	/**
	 * ConnectionFactory transaction object, representing a ConnectionHolder.
	 * Used as transaction object by R2dbcTransactionManager.
	 */
	private static class ConnectionFactoryTransactionObject {

		@Nullable
		private ConnectionHolder connectionHolder;

		private boolean newConnectionHolder;

		@Nullable
		private String savepointName;

		void setConnectionHolder(@Nullable ConnectionHolder connectionHolder, boolean newConnectionHolder) {
			setConnectionHolder(connectionHolder);
			this.newConnectionHolder = newConnectionHolder;
		}

		boolean isNewConnectionHolder() {
			return this.newConnectionHolder;
		}

		public void setConnectionHolder(@Nullable ConnectionHolder connectionHolder) {
			this.connectionHolder = connectionHolder;
		}

		public ConnectionHolder getConnectionHolder() {
			Assert.state(this.connectionHolder != null, "No ConnectionHolder available");
			return this.connectionHolder;
		}

		public boolean hasConnectionHolder() {
			return (this.connectionHolder != null);
		}

		public boolean isTransactionActive() {
			return (this.connectionHolder != null && this.connectionHolder.isTransactionActive());
		}

		public Mono<Void> createSavepoint() {
			ConnectionHolder holder = getConnectionHolder();
			this.savepointName = holder.nextSavepoint();
			return Mono.from(holder.getConnection().createSavepoint(this.savepointName));
		}

		public Mono<Void> commit() {
			Connection connection = getConnectionHolder().getConnection();
			return (this.savepointName != null ?
					Mono.from(connection.releaseSavepoint(this.savepointName)) :
					Mono.from(connection.commitTransaction()));
		}

		public Mono<Void> rollback() {
			Connection connection = getConnectionHolder().getConnection();
			return (this.savepointName != null ?
					Mono.from(connection.rollbackTransactionToSavepoint(this.savepointName)) :
					Mono.from(connection.rollbackTransaction()));
		}

		public void setRollbackOnly() {
			getConnectionHolder().setRollbackOnly();
		}
	}

}
