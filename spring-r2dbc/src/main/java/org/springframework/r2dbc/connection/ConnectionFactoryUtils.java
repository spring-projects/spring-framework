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

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.R2dbcNonTransientException;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcPermissionDeniedException;
import io.r2dbc.spi.R2dbcRollbackException;
import io.r2dbc.spi.R2dbcTimeoutException;
import io.r2dbc.spi.R2dbcTransientException;
import io.r2dbc.spi.R2dbcTransientResourceException;
import io.r2dbc.spi.Wrapped;
import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.r2dbc.UncategorizedR2dbcException;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.reactive.TransactionSynchronization;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Helper class that provides static methods for obtaining R2DBC Connections from
 * a {@link ConnectionFactory}.
 *
 * <p>Used internally by Spring's {@code DatabaseClient}, Spring's R2DBC operation
 * objects. Can also be used directly in application code.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 5.3
 * @see R2dbcTransactionManager
 * @see org.springframework.transaction.reactive.TransactionSynchronizationManager
 */
public abstract class ConnectionFactoryUtils {

	/**
	 * Order value for ReactiveTransactionSynchronization objects that clean up R2DBC Connections.
	 */
	public static final int CONNECTION_SYNCHRONIZATION_ORDER = 1000;


	/**
	 * Obtain a {@link Connection} from the given {@link ConnectionFactory}.
	 * Translates exceptions into the Spring hierarchy of unchecked generic
	 * data access exceptions, simplifying calling code and making any
	 * exception that is thrown more meaningful.
	 * <p>Is aware of a corresponding Connection bound to the current
	 * {@link TransactionSynchronizationManager}. Will bind a Connection to the
	 * {@link TransactionSynchronizationManager} if transaction synchronization is active.
	 * @param connectionFactory the {@link ConnectionFactory} to obtain
	 * {@link Connection Connections} from
	 * @return a R2DBC Connection from the given {@link ConnectionFactory}
	 * @throws DataAccessResourceFailureException if the attempt to get a
	 * {@link Connection} failed
	 * @see #releaseConnection
	 */
	public static Mono<Connection> getConnection(ConnectionFactory connectionFactory) {
		return doGetConnection(connectionFactory)
				.onErrorMap(ex -> new DataAccessResourceFailureException("Failed to obtain R2DBC Connection", ex));
	}

	/**
	 * Actually obtain a R2DBC Connection from the given {@link ConnectionFactory}.
	 * Same as {@link #getConnection}, but preserving the original exceptions.
	 * <p>Is aware of a corresponding Connection bound to the current
	 * {@link TransactionSynchronizationManager}. Will bind a Connection to the
	 * {@link TransactionSynchronizationManager} if transaction synchronization is active
	 * @param connectionFactory the {@link ConnectionFactory} to obtain Connections from
	 * @return a R2DBC {@link Connection} from the given {@link ConnectionFactory}.
	 */
	public static Mono<Connection> doGetConnection(ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {

			ConnectionHolder conHolder = (ConnectionHolder) synchronizationManager.getResource(connectionFactory);
			if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
				conHolder.requested();
				if (!conHolder.hasConnection()) {
					return fetchConnection(connectionFactory).doOnNext(conHolder::setConnection);
				}
				return Mono.just(conHolder.getConnection());
			}
			// Else we either got no holder or an empty thread-bound holder here.

			Mono<Connection> con = fetchConnection(connectionFactory);
			if (synchronizationManager.isSynchronizationActive()) {
				return con.flatMap(connection -> Mono.just(connection).doOnNext(conn -> {
					// Use same Connection for further R2DBC actions within the transaction.
					// Thread-bound object will get removed by synchronization at transaction completion.
					ConnectionHolder holderToUse = conHolder;
					if (holderToUse == null) {
						holderToUse = new ConnectionHolder(conn);
					}
					else {
						holderToUse.setConnection(conn);
					}
					holderToUse.requested();
					synchronizationManager.registerSynchronization(
							new ConnectionSynchronization(holderToUse, connectionFactory));
					holderToUse.setSynchronizedWithTransaction(true);
					if (holderToUse != conHolder) {
						synchronizationManager.bindResource(connectionFactory, holderToUse);
					}
				})      // Unexpected exception from external delegation call -> close Connection and rethrow.
				.onErrorResume(ex -> releaseConnection(connection, connectionFactory).then(Mono.error(ex))));
			}
			return con;
		}).onErrorResume(NoTransactionException.class, ex -> Mono.from(connectionFactory.create()));
	}

	/**
	 * Actually fetch a {@link Connection} from the given {@link ConnectionFactory}.
	 * @param connectionFactory the {@link ConnectionFactory} to obtain
	 * {@link Connection}s from
	 * @return a R2DBC {@link Connection} from the given {@link ConnectionFactory}
	 * (never {@code null}).
	 * @throws IllegalStateException if the {@link ConnectionFactory} returned a {@code null} value.
	 * @see ConnectionFactory#create()
	 */
	private static Mono<Connection> fetchConnection(ConnectionFactory connectionFactory) {
		return Mono.from(connectionFactory.create());
	}

	/**
	 * Close the given {@link Connection}, obtained from the given {@link ConnectionFactory}, if
	 * it is not managed externally (that is, not bound to the subscription).
	 * @param con the {@link Connection} to close if necessary
	 * @param connectionFactory the {@link ConnectionFactory} that the Connection was obtained from
	 * @see #getConnection
	 */
	public static Mono<Void> releaseConnection(Connection con, ConnectionFactory connectionFactory) {
		return doReleaseConnection(con, connectionFactory)
				.onErrorMap(ex -> new DataAccessResourceFailureException("Failed to close R2DBC Connection", ex));
	}

	/**
	 * Actually close the given {@link Connection}, obtained from the given
	 * {@link ConnectionFactory}. Same as {@link #releaseConnection},
	 * but preserving the original exception.
	 * @param connection the {@link Connection} to close if necessary
	 * @param connectionFactory the {@link ConnectionFactory} that the Connection was obtained from
	 * @see #doGetConnection
	 */
	public static Mono<Void> doReleaseConnection(Connection connection, ConnectionFactory connectionFactory) {
		return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {
			ConnectionHolder conHolder = (ConnectionHolder) synchronizationManager.getResource(connectionFactory);
			if (conHolder != null && connectionEquals(conHolder, connection)) {
				// It's the transactional Connection: Don't close it.
				conHolder.released();
			}
			return Mono.from(connection.close());
		}).onErrorResume(NoTransactionException.class, ex -> Mono.from(connection.close()));
	}

	/**
	 * Obtain the {@link ConnectionFactory} from the current {@link TransactionSynchronizationManager}.
	 * @param connectionFactory the {@link ConnectionFactory} that the Connection was obtained from
	 * @see TransactionSynchronizationManager
	 */
	public static Mono<ConnectionFactory> currentConnectionFactory(ConnectionFactory connectionFactory) {
		return TransactionSynchronizationManager.forCurrentTransaction()
				.filter(TransactionSynchronizationManager::isSynchronizationActive)
				.filter(synchronizationManager -> {
					ConnectionHolder conHolder = (ConnectionHolder) synchronizationManager.getResource(connectionFactory);
					return conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction());
				}).map(synchronizationManager -> connectionFactory);
	}

	/**
	 * Translate the given {@link R2dbcException} into a generic {@link DataAccessException}.
	 * <p>The returned DataAccessException is supposed to contain the original
	 * {@link R2dbcException} as root cause. However, client code may not generally
	 * rely on this due to DataAccessExceptions possibly being caused by other resource
	 * APIs as well. That said, a {@code getRootCause() instanceof R2dbcException}
	 * check (and subsequent cast) is considered reliable when expecting R2DBC-based
	 * access to have happened.
	 * @param task readable text describing the task being attempted
	 * @param sql the SQL query or update that caused the problem (if known)
	 * @param ex the offending {@link R2dbcException}
	 * @return the corresponding DataAccessException instance
	 */
	public static DataAccessException convertR2dbcException(String task, @Nullable String sql, R2dbcException ex) {
		if (ex instanceof R2dbcTransientException) {
			if (ex instanceof R2dbcTransientResourceException) {
				return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof R2dbcRollbackException) {
				if ("40001".equals(ex.getSqlState())) {
					return new CannotAcquireLockException(buildMessage(task, sql, ex), ex);
				}
				return new PessimisticLockingFailureException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof R2dbcTimeoutException) {
				return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
			}
		}
		else if (ex instanceof R2dbcNonTransientException) {
			if (ex instanceof R2dbcNonTransientResourceException) {
				return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof R2dbcDataIntegrityViolationException) {
				if (indicatesDuplicateKey(ex.getSqlState(), ex.getErrorCode())) {
					return new DuplicateKeyException(buildMessage(task, sql, ex), ex);
				}
				return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof R2dbcPermissionDeniedException) {
				return new PermissionDeniedDataAccessException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof R2dbcBadGrammarException) {
				return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
			}
		}
		return new UncategorizedR2dbcException(buildMessage(task, sql, ex), sql, ex);
	}

	/**
	 * Check whether the given SQL state (and the associated error code in case
	 * of a generic SQL state value) indicate a duplicate key exception. See
	 * {@code org.springframework.jdbc.support.SQLStateSQLExceptionTranslator#indicatesDuplicateKey}.
	 * @param sqlState the SQL state value
	 * @param errorCode the error code value
	 */
	static boolean indicatesDuplicateKey(@Nullable String sqlState, int errorCode) {
		return ("23505".equals(sqlState) ||
				("23000".equals(sqlState) &&
						(errorCode == 1 || errorCode == 1062 || errorCode == 2601 || errorCode == 2627)));
	}

	/**
	 * Build a message {@code String} for the given {@link R2dbcException}.
	 * <p>To be called by translator subclasses when creating an instance of a generic
	 * {@link org.springframework.dao.DataAccessException} class.
	 * @param task readable text describing the task being attempted
	 * @param sql the SQL statement that caused the problem
	 * @param ex the offending {@code R2dbcException}
	 * @return the message {@code String} to use
	 */
	private static String buildMessage(String task, @Nullable String sql, R2dbcException ex) {
		return task + "; " + (sql != null ? ("SQL [" + sql + "]; ") : "") + ex.getMessage();
	}

	/**
	 * Determine whether the given two {@link Connection}s are equal, asking the target
	 * {@link Connection} in case of a proxy. Used to detect equality even if the user
	 * passed in a raw target Connection while the held one is a proxy.
	 * @param conHolder the {@link ConnectionHolder} for the held {@link Connection} (potentially a proxy)
	 * @param passedInCon the {@link Connection} passed-in by the user (potentially
	 * a target {@link Connection} without proxy).
	 * @return whether the given Connections are equal
	 * @see #getTargetConnection
	 */
	private static boolean connectionEquals(ConnectionHolder conHolder, Connection passedInCon) {
		if (!conHolder.hasConnection()) {
			return false;
		}
		Connection heldCon = conHolder.getConnection();
		// Explicitly check for identity too: for Connection handles that do not implement
		// "equals" properly).
		return (heldCon == passedInCon || heldCon.equals(passedInCon) ||
				getTargetConnection(heldCon).equals(passedInCon));
	}

	/**
	 * Return the innermost target {@link Connection} of the given {@link Connection}.
	 * If the given {@link Connection} is wrapped, it will be unwrapped until a
	 * plain {@link Connection} is found. Otherwise, the passed-in Connection
	 * will be returned as-is.
	 * @param con the {@link Connection} wrapper to unwrap
	 * @return the innermost target Connection, or the passed-in one if not wrapped
	 * @see Wrapped#unwrap()
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Connection getTargetConnection(Connection con) {
		Object conToUse = con;
		while (conToUse instanceof Wrapped wrapped) {
			conToUse = wrapped.unwrap();
		}
		return (Connection) conToUse;
	}

	/**
	 * Determine the connection synchronization order to use for the given {@link ConnectionFactory}.
	 * Decreased for every level of nesting that a {@link ConnectionFactory} has,
	 * checked through the level of {@link DelegatingConnectionFactory} nesting.
	 * @param connectionFactory the {@link ConnectionFactory} to check
	 * @return the connection synchronization order to use
	 * @see #CONNECTION_SYNCHRONIZATION_ORDER
	 */
	private static int getConnectionSynchronizationOrder(ConnectionFactory connectionFactory) {
		int order = CONNECTION_SYNCHRONIZATION_ORDER;
		ConnectionFactory current = connectionFactory;
		while (current instanceof DelegatingConnectionFactory delegatingConnectionFactory) {
			order--;
			current = delegatingConnectionFactory.getTargetConnectionFactory();
		}
		return order;
	}


	/**
	 * Callback for resource cleanup at the end of a non-native R2DBC transaction.
	 */
	private static class ConnectionSynchronization implements TransactionSynchronization, Ordered {

		private final ConnectionHolder connectionHolder;

		private final ConnectionFactory connectionFactory;

		private final int order;

		private boolean holderActive = true;

		ConnectionSynchronization(ConnectionHolder connectionHolder, ConnectionFactory connectionFactory) {
			this.connectionHolder = connectionHolder;
			this.connectionFactory = connectionFactory;
			this.order = getConnectionSynchronizationOrder(connectionFactory);
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public Mono<Void> suspend() {
			if (this.holderActive) {
				return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {
					synchronizationManager.unbindResource(this.connectionFactory);
					if (this.connectionHolder.hasConnection() && !this.connectionHolder.isOpen()) {
						// Release Connection on suspend if the application doesn't keep
						// a handle to it anymore. We will fetch a fresh Connection if the
						// application accesses the ConnectionHolder again after resume,
						// assuming that it will participate in the same transaction.
						return releaseConnection(this.connectionHolder.getConnection(), this.connectionFactory)
								.doOnTerminate(() -> this.connectionHolder.setConnection(null));
					}
					return Mono.empty();
				});
			}
			return Mono.empty();
		}

		@Override
		public Mono<Void> resume() {
			if (this.holderActive) {
				return TransactionSynchronizationManager.forCurrentTransaction()
						.doOnNext(synchronizationManager ->
								synchronizationManager.bindResource(this.connectionFactory, this.connectionHolder))
						.then();
			}
			return Mono.empty();
		}

		@Override
		public Mono<Void> beforeCompletion() {
			// Release Connection early if the holder is not open anymore (that is,
			// not used by another resource that has its own cleanup via transaction
			// synchronization), to avoid issues with strict transaction implementations
			// that expect the close call before transaction completion.
			if (!this.connectionHolder.isOpen()) {
				return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {
					synchronizationManager.unbindResource(this.connectionFactory);
					this.holderActive = false;
					if (this.connectionHolder.hasConnection()) {
						return releaseConnection(this.connectionHolder.getConnection(), this.connectionFactory);
					}
					return Mono.empty();
				});
			}

			return Mono.empty();
		}

		@Override
		public Mono<Void> afterCompletion(int status) {
			// If we haven't closed the Connection in beforeCompletion,
			// close it now.
			if (this.holderActive) {
				// The bound ConnectionHolder might not be available anymore,
				// since afterCompletion might get called from a different thread.
				return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {
					synchronizationManager.unbindResourceIfPossible(this.connectionFactory);
					this.holderActive = false;
					if (this.connectionHolder.hasConnection()) {
						return releaseConnection(this.connectionHolder.getConnection(), this.connectionFactory)
								// Reset the ConnectionHolder: It might remain bound to the context.
								.doOnTerminate(() -> this.connectionHolder.setConnection(null));
					}
					return Mono.empty();
				});
			}
			this.connectionHolder.reset();
			return Mono.empty();
		}
	}

}
