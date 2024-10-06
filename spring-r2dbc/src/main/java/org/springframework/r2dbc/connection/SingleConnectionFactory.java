/*
 * Copyright 2002-2024 the original author or authors.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.Wrapped;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link DelegatingConnectionFactory} that wraps a
 * single R2DBC {@link Connection} which is not closed after use.
 * Obviously, this is not multi-threading capable.
 *
 * <p>Note that at shutdown, someone should close the underlying
 * {@code Connection} via the {@code close()} method. Client code will
 * never call close on the {@code Connection} handle if it is
 * SmartConnectionFactory-aware (e.g. uses
 * {@link ConnectionFactoryUtils#releaseConnection(Connection, ConnectionFactory)}).
 *
 * <p>If client code will call {@link Connection#close()} in the
 * assumption of a pooled {@code Connection}, like when using persistence tools,
 * set "suppressClose" to {@code true}. This will return a close-suppressing
 * proxy instead of the physical Connection.
 *
 * <p>This is primarily intended for testing and pipelining usage of connections.
 * For example, it enables easy testing outside an application server for code
 * that expects to work on a {@link ConnectionFactory}.
 * Note that this implementation does not act as a connection pool-like utility.
 * Connection pooling requires a pooling {@link ConnectionFactory} such as one from
 * {@code r2dbc-pool}.
 *
 * @author Mark Paluch
 * @since 5.3
 * @see #create()
 * @see Connection#close()
 * @see ConnectionFactoryUtils#releaseConnection(Connection, ConnectionFactory)
 */
public class SingleConnectionFactory extends DelegatingConnectionFactory
		implements DisposableBean {

	/** Create a close-suppressing proxy?. */
	private boolean suppressClose;

	/** Override auto-commit state?. */
	@Nullable
	private Boolean autoCommit;

	/** Wrapped Connection. */
	private final AtomicReference<Connection> target = new AtomicReference<>();

	/** Proxy Connection. */
	@Nullable
	private Connection connection;

	private final Mono<? extends Connection> connectionEmitter;


	/**
	 * Constructor for bean-style configuration.
	 */
	public SingleConnectionFactory(ConnectionFactory targetConnectionFactory) {
		super(targetConnectionFactory);
		this.connectionEmitter = super.create().cache();
	}

	/**
	 * Create a new {@code SingleConnectionFactory} using an R2DBC connection URL.
	 * @param url the R2DBC URL to use for accessing {@link ConnectionFactory} discovery
	 * @param suppressClose if the returned {@link Connection} should be a
	 * close-suppressing proxy or the physical {@code Connection}
	 * @see ConnectionFactories#get(String)
	 */
	public SingleConnectionFactory(String url, boolean suppressClose) {
		super(ConnectionFactories.get(url));
		this.suppressClose = suppressClose;
		this.connectionEmitter = super.create().cache();
	}

	/**
	 * Create a new {@code SingleConnectionFactory} with a given {@link Connection}
	 * and {@link ConnectionFactoryMetadata}.
	 * @param target underlying target {@code Connection}
	 * @param metadata {@code ConnectionFactory} metadata to be associated with
	 * this {@code ConnectionFactory}
	 * @param suppressClose {@code true} if the {@code Connection} should be wrapped
	 * with a {@code Connection} that suppresses {@code close()} calls (to allow
	 * for normal {@code close()} usage in applications that expect a pooled
	 * {@code Connection})
	 */
	public SingleConnectionFactory(Connection target, ConnectionFactoryMetadata metadata, boolean suppressClose) {
		super(new ConnectionFactory() {
			@Override
			public Publisher<? extends Connection> create() {
				return Mono.just(target);
			}
			@Override
			public ConnectionFactoryMetadata getMetadata() {
				return metadata;
			}
		});
		Assert.notNull(target, "Connection must not be null");
		Assert.notNull(metadata, "ConnectionFactoryMetadata must not be null");
		this.target.set(target);
		this.connectionEmitter = Mono.just(target);
		this.suppressClose = suppressClose;
		this.connection = (suppressClose ? getCloseSuppressingConnectionProxy(target) : target);
	}


	/**
	 * Set whether the returned {@link Connection} should be a close-suppressing proxy
	 * or the physical {@code Connection}.
	 */
	public void setSuppressClose(boolean suppressClose) {
		this.suppressClose = suppressClose;
	}

	/**
	 * Return whether the returned {@link Connection} will be a close-suppressing proxy
	 * or the physical {@code Connection}.
	 */
	protected boolean isSuppressClose() {
		return this.suppressClose;
	}

	/**
	 * Set whether the returned {@link Connection}'s "autoCommit" setting should
	 * be overridden.
	 */
	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}

	/**
	 * Return whether the returned {@link Connection}'s "autoCommit" setting should
	 * be overridden.
	 * @return the "autoCommit" value, or {@code null} if none to be applied
	 */
	@Nullable
	protected Boolean getAutoCommitValue() {
		return this.autoCommit;
	}


	@Override
	public Mono<? extends Connection> create() {
		Connection connection = this.target.get();
		return this.connectionEmitter.map(connectionToUse -> {
			if (connection == null) {
				this.target.compareAndSet(null, connectionToUse);
				this.connection =
						(isSuppressClose() ? getCloseSuppressingConnectionProxy(connectionToUse) : connectionToUse);
			}
			return this.connection;
		}).flatMap(this::prepareConnection);
	}

	/**
	 * Close the underlying {@link Connection}.
	 * The provider of this {@link ConnectionFactory} needs to care for proper shutdown.
	 * <p>As this bean implements {@link DisposableBean}, a bean factory will automatically
	 * invoke this on destruction of its cached singletons.
	 */
	@Override
	public void destroy() {
		resetConnection().block();
	}

	/**
	 * Reset the underlying shared Connection, to be reinitialized on next access.
	 */
	public Mono<Void> resetConnection() {
		Connection connection = this.target.get();
		if (connection == null) {
			return Mono.empty();
		}
		return Mono.defer(() -> {
			if (this.target.compareAndSet(connection, null)) {
				this.connection = null;
				return Mono.from(connection.close());
			}
			return Mono.empty();
		});
	}

	/**
	 * Prepare the {@link Connection} before using it.
	 * Applies {@linkplain #getAutoCommitValue() auto-commit} settings if configured.
	 * @param connection the requested {@code Connection}
	 * @return the prepared {@code Connection}
	 */
	protected Mono<Connection> prepareConnection(Connection connection) {
		Boolean autoCommit = getAutoCommitValue();
		if (autoCommit != null) {
			return Mono.from(connection.setAutoCommit(autoCommit)).thenReturn(connection);
		}
		return Mono.just(connection);
	}

	/**
	 * Wrap the given {@link Connection} with a proxy that delegates every method call to it
	 * but suppresses close calls.
	 * @param target the original {@code Connection} to wrap
	 * @return the wrapped Connection
	 */
	protected Connection getCloseSuppressingConnectionProxy(Connection target) {
		return (Connection) Proxy.newProxyInstance(SingleConnectionFactory.class.getClassLoader(),
				new Class<?>[] {Connection.class, Wrapped.class}, new CloseSuppressingInvocationHandler(target));
	}


	/**
	 * Invocation handler that suppresses close calls on R2DBC Connections.
	 *
	 * @see Connection#close()
	 */
	private static class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Connection target;

		CloseSuppressingInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return proxy == args[0];
				case "hashCode":
					// Use hashCode of PersistenceManager proxy.
					return System.identityHashCode(proxy);
				case "unwrap":
					return this.target;
				case "close":
					// Handle close method: suppress, not valid.
					return Mono.empty();
			}

			// Invoke method on target Connection.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
