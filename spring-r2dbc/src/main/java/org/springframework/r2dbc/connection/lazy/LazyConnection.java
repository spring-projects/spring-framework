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

package org.springframework.r2dbc.connection.lazy;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionMetadata;
import io.r2dbc.spi.IsolationLevel;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.TransactionDefinition;
import io.r2dbc.spi.ValidationDepth;
import io.r2dbc.spi.Wrapped;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lazy implementation of {@link Connection}.
 *
 * <p>This implementation delays the actual connection creation until the first
 * statement is executed or a method requiring the physical connection is called.
 * Transaction state (begin, commit, rollback) and isolation levels are buffered
 * locally and replayed onto the physical connection once it is initialized.
 *
 * @author Somil Jain
 * @since 6.2
 * @see LazyConnectionFactory
 */
class LazyConnection implements Connection, Wrapped<Connection> {

	private final ConnectionFactory targetFactory;
	private final Mono<Connection> connectionMono;
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	private volatile @Nullable Connection target;

	private enum TxState { NONE, ACTIVE }
	private volatile TxState txState = TxState.NONE;

	private volatile @Nullable IsolationLevel isolationLevel = null;
	private volatile @Nullable TransactionDefinition txDefinition = null;

	/**
	 * Create a new {@code LazyConnection} for the given target factory.
	 * @param targetFactory the factory to use for creating the physical connection
	 */
	public LazyConnection(ConnectionFactory targetFactory) {
		this.targetFactory = targetFactory;
		this.connectionMono = Mono.defer(() -> Mono.from(targetFactory.create()))
				.doOnNext(c -> {
					this.target = c;
					this.initialized.set(true);
				})
				.flatMap(this::replayState)
				.cache();
	}

	private Mono<Connection> replayState(Connection c) {
		Mono<Void> setup = Mono.empty();

		IsolationLevel localIso = this.isolationLevel;
		if (localIso != null) {
			setup = setup.then(Mono.from(c.setTransactionIsolationLevel(localIso)));
		}

		if (this.txState == TxState.ACTIVE) {
			TransactionDefinition localTxDef = this.txDefinition;
			if (localTxDef != null) {
				setup = setup.then(Mono.from(c.beginTransaction(localTxDef)));
			} else {
				setup = setup.then(Mono.from(c.beginTransaction()));
			}
		}

		return setup.thenReturn(c);
	}

	@Override
	public Mono<Void> beginTransaction() {
		return Mono.fromRunnable(() -> {
			this.txState = TxState.ACTIVE;
			this.txDefinition = null;
		}).then(Mono.defer(() -> {
			if (this.initialized.get()) {
				return Mono.from(Objects.requireNonNull(this.target).beginTransaction());
			}
			return Mono.empty();
		}));
	}

	@Override
	public Mono<Void> beginTransaction(TransactionDefinition definition) {
		return Mono.fromRunnable(() -> {
			this.txState = TxState.ACTIVE;
			this.txDefinition = definition;
		}).then(Mono.defer(() -> {
			if (this.initialized.get()) {
				return Mono.from(Objects.requireNonNull(this.target).beginTransaction(definition));
			}
			return Mono.empty();
		}));
	}

	/**
	 * Buffer the isolation level until a physical connection is available.
	 */
	@Override
	public Mono<Void> commitTransaction() {
		return Mono.fromRunnable(() -> {
			this.txState = TxState.NONE;
			this.txDefinition = null;
		}).then(Mono.defer(() -> {
			if (this.initialized.get()) {
				return Mono.from(Objects.requireNonNull(this.target).commitTransaction());
			}
			return Mono.empty();
		}));
	}

	@Override
	public Mono<Void> rollbackTransaction() {
		return Mono.fromRunnable(() -> {
			this.txState = TxState.NONE;
			this.txDefinition = null;
		}).then(Mono.defer(() -> {
			if (this.initialized.get()) {
				return Mono.from(Objects.requireNonNull(this.target).rollbackTransaction());
			}
			return Mono.empty();
		}));
	}

	@Override
	public Mono<Void> setTransactionIsolationLevel(IsolationLevel isolationLevel) {
		return Mono.fromRunnable(() -> this.isolationLevel = isolationLevel)
				.then(Mono.defer(() -> {
					if (this.initialized.get()) {
						return Mono.from(Objects.requireNonNull(this.target).setTransactionIsolationLevel(isolationLevel));
					}
					return Mono.empty();
				}));
	}

	@Override
	public IsolationLevel getTransactionIsolationLevel() {
		IsolationLevel localIso = this.isolationLevel;
		if (localIso != null) {
			return localIso;
		}
		if (this.initialized.get()) {
			return Objects.requireNonNull(this.target).getTransactionIsolationLevel();
		}
		return IsolationLevel.READ_COMMITTED;
	}

	@Override
	public Mono<Void> setAutoCommit(boolean autoCommit) {
		if (this.initialized.get()) {
			return Mono.from(Objects.requireNonNull(this.target).setAutoCommit(autoCommit));
		}
		return Mono.empty();
	}

	@Override
	public boolean isAutoCommit() {
		if (this.initialized.get()) {
			return Objects.requireNonNull(this.target).isAutoCommit();
		}
		throw new IllegalStateException("Auto-commit state not available before connection initialization");
	}

	@Override
	public Mono<Void> setLockWaitTimeout(Duration timeout) {
		if (this.initialized.get()) {
			return Mono.from(Objects.requireNonNull(this.target).setLockWaitTimeout(timeout));
		}
		return Mono.empty();
	}

	@Override
	public Mono<Void> setStatementTimeout(Duration timeout) {
		if (this.initialized.get()) {
			return Mono.from(Objects.requireNonNull(this.target).setStatementTimeout(timeout));
		}
		return Mono.empty();
	}

	/**
	 * Savepoint operations always require a physical connection and
	 * therefore trigger initialization.
	 */
	@Override
	public Mono<Void> createSavepoint(String name) {
		if (this.txState != TxState.ACTIVE) {
			return Mono.error(new IllegalStateException("Cannot create savepoint: No active transaction"));
		}
		return this.connectionMono.flatMap(c -> Mono.from(c.createSavepoint(name)));
	}

	@Override
	public Mono<Void> releaseSavepoint(String name) {
		return this.connectionMono.flatMap(c -> Mono.from(c.releaseSavepoint(name)));
	}

	@Override
	public Mono<Void> rollbackTransactionToSavepoint(String name) {
		return this.connectionMono.flatMap(c -> Mono.from(c.rollbackTransactionToSavepoint(name)));
	}

	@Override
	public Statement createStatement(String sql) {
		return new LazyStatement(sql, this.connectionMono);
	}

	@Override
	public Batch createBatch() {
		return new LazyBatch(this.connectionMono);
	}

	@Override
	public Mono<Void> close() {
		return Mono.defer(() -> {
			if (this.initialized.get()) {
				return Mono.from(Objects.requireNonNull(this.target).close())
						.onErrorResume(e -> Mono.empty());
			}
			return Mono.empty();
		});
	}

	@Override
	public ConnectionMetadata getMetadata() {
		if (this.initialized.get()) {
			return Objects.requireNonNull(this.target).getMetadata();
		}
		return new LazyConnectionMetadata(this.targetFactory.getMetadata().getName());
	}

	@Override
	public Mono<Boolean> validate(ValidationDepth depth) {
		if (!this.initialized.get()) {
			return Mono.just(true);
		}
		return Mono.from(Objects.requireNonNull(this.target).validate(depth));
	}

	@Override
	public Connection unwrap() {
		Connection t = this.target;
		if (t == null) {
			throw new IllegalStateException("Underlying Connection not initialized yet");
		}
		return t;
	}

	/**
	 * Minimal {@link ConnectionMetadata} used before physical connection creation.
	 */
	private record LazyConnectionMetadata(String productName) implements ConnectionMetadata {
		@Override
		public String getDatabaseProductName() {
			return this.productName;
		}

		@Override
		public String getDatabaseVersion() {
			return "Unknown (Lazy)";
		}
	}
}