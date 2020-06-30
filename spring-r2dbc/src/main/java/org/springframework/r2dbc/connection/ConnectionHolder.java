/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;


/**
 * Resource holder wrapping a R2DBC {@link Connection}.
 * {@link R2dbcTransactionManager} binds instances of this class to the subscription,
 * for a specific {@link ConnectionFactory}.
 *
 * <p>Inherits rollback-only support for nested R2DBC transactions and reference
 * count functionality from the base class.
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 5.3
 * @see R2dbcTransactionManager
 * @see ConnectionFactoryUtils
 */
public class ConnectionHolder extends ResourceHolderSupport {

	@Nullable
	private Connection currentConnection;

	private boolean transactionActive;


	/**
	 * Create a new ConnectionHolder for the given R2DBC {@link Connection},
	 * assuming that there is no ongoing transaction.
	 * @param connection the R2DBC {@link Connection} to hold
	 * @see #ConnectionHolder(Connection, boolean)
	 */
	public ConnectionHolder(Connection connection) {
		this(connection, false);
	}

	/**
	 * Create a new ConnectionHolder for the given R2DBC {@link Connection}.
	 * @param connection the R2DBC {@link Connection} to hold
	 * @param transactionActive whether the given {@link Connection} is involved
	 * in an ongoing transaction
	 */
	public ConnectionHolder(Connection connection, boolean transactionActive) {
		this.currentConnection = connection;
		this.transactionActive = transactionActive;
	}


	/**
	 * Return whether this holder currently has a {@link Connection}.
	 */
	protected boolean hasConnection() {
		return (this.currentConnection != null);
	}

	/**
	 * Set whether this holder represents an active, R2DBC-managed transaction.
	 * @see R2dbcTransactionManager
	 */
	protected void setTransactionActive(boolean transactionActive) {
		this.transactionActive = transactionActive;
	}

	/**
	 * Return whether this holder represents an active, R2DBC-managed transaction.
	 */
	protected boolean isTransactionActive() {
		return this.transactionActive;
	}

	/**
	 * Override the existing Connection with the given {@link Connection}.
	 * <p>Used for releasing the {@link Connection} on suspend
	 * (with a {@code null} argument) and setting a fresh {@link Connection} on resume.
	 */
	protected void setConnection(@Nullable Connection connection) {
		this.currentConnection = connection;
	}

	/**
	 * Return the current {@link Connection} held by this {@link ConnectionHolder}.
	 * <p>This will be the same {@link Connection} until {@code released} gets called
	 * on the {@link ConnectionHolder}, which will reset the held {@link Connection},
	 * fetching a new {@link Connection} on demand.
	 * @see #released()
	 */
	public Connection getConnection() {
		Assert.notNull(this.currentConnection, "Active Connection is required");
		return this.currentConnection;
	}

	/**
	 * Releases the current {@link Connection}.
	 */
	@Override
	public void released() {
		super.released();
		if (!isOpen() && this.currentConnection != null) {
			this.currentConnection = null;
		}
	}

	@Override
	public void clear() {
		super.clear();
		this.transactionActive = false;
	}

}
