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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Wrapped;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * Proxy for a target R2DBC {@link ConnectionFactory}, adding awareness
 * of Spring-managed transactions.
 *
 * <p>Data access code that should remain unaware of Spring's data access
 * support can work with this proxy to seamlessly participate in
 * Spring-managed transactions.
 * Note that the transaction manager, for example {@link R2dbcTransactionManager},
 * still needs to work with the underlying {@link ConnectionFactory},
 * <i>not</i> with this proxy.
 *
 * <p><b>Make sure that {@link TransactionAwareConnectionFactoryProxy} is the outermost
 * {@link ConnectionFactory} of a chain of {@link ConnectionFactory} proxies/adapters.</b>
 * {@link TransactionAwareConnectionFactoryProxy} can delegate either directly to the
 * target connection pool or to some intermediary proxy/adapter.
 *
 * <p>Delegates to {@link ConnectionFactoryUtils} for automatically participating
 * in thread-bound transactions, for example managed by {@link R2dbcTransactionManager}.
 * {@link #create()} calls and {@code close} calls on returned {@link Connection}
 * will behave properly within a transaction, i.e. always operate on the
 * transactional Connection. If not within a transaction, normal {@link ConnectionFactory}
 * behavior applies.
 *
 * <p> This proxy allows data access code to work with the plain R2DBC API. However,
 * if possible, use Spring's {@link ConnectionFactoryUtils} or {@code DatabaseClient}
 * to get transaction participation even without a proxy for the target
 * {@link ConnectionFactory}, avoiding the need to define such a proxy in the first place.
 *
 * <p><b>NOTE:</b> This {@link ConnectionFactory} proxy needs to return wrapped
 * {@link Connection}s in order to handle close calls properly.
 * Use {@link Wrapped#unwrap()} to retrieve the native R2DBC Connection.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 5.3
 * @see ConnectionFactory#create
 * @see Connection#close
 * @see ConnectionFactoryUtils#doGetConnection
 * @see ConnectionFactoryUtils#doReleaseConnection
 */
public class TransactionAwareConnectionFactoryProxy extends DelegatingConnectionFactory {

	/**
	 * Create a new {@link TransactionAwareConnectionFactoryProxy}.
	 * @param targetConnectionFactory the target {@link ConnectionFactory}
	 */
	public TransactionAwareConnectionFactoryProxy(ConnectionFactory targetConnectionFactory) {
		super(targetConnectionFactory);
	}


	/**
	 * Delegates to {@link ConnectionFactoryUtils} for automatically participating
	 * in Spring-managed transactions.
	 * @return a transactional {@link Connection} if any, a new one else.
	 * @see ConnectionFactoryUtils#doGetConnection
	 */
	@Override
	public Mono<Connection> create() {
		return getTransactionAwareConnectionProxy(getTargetConnectionFactory());
	}

	/**
	 * Wraps the given {@link Connection} with a proxy that delegates every method call
	 * to it but delegates {@code close()} calls to {@link ConnectionFactoryUtils}.
	 * @param targetConnectionFactory the {@link ConnectionFactory} that the {@link Connection} came from
	 * @return the wrapped {@link Connection}.
	 * @see Connection#close()
	 * @see ConnectionFactoryUtils#doReleaseConnection
	 */
	protected Mono<Connection> getTransactionAwareConnectionProxy(ConnectionFactory targetConnectionFactory) {
		return ConnectionFactoryUtils.getConnection(targetConnectionFactory)
				.map(connection -> proxyConnection(connection, targetConnectionFactory));
	}

	private static Connection proxyConnection(Connection connection, ConnectionFactory targetConnectionFactory) {
		return (Connection) Proxy.newProxyInstance(TransactionAwareConnectionFactoryProxy.class.getClassLoader(),
				new Class<?>[] {Connection.class, Wrapped.class},
				new TransactionAwareInvocationHandler(connection, targetConnectionFactory));
	}


	/**
	 * Invocation handler that delegates close calls on R2DBC Connections to
	 * {@link ConnectionFactoryUtils} for being aware of context-bound transactions.
	 */
	private static class TransactionAwareInvocationHandler implements InvocationHandler {

		private final Connection connection;

		private final ConnectionFactory targetConnectionFactory;

		private boolean closed = false;

		TransactionAwareInvocationHandler(Connection connection, ConnectionFactory targetConnectionFactory) {
			this.connection = connection;
			this.targetConnectionFactory = targetConnectionFactory;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isObjectMethod(method)) {
				if (ReflectionUtils.isToStringMethod(method)) {
					return proxyToString(proxy);
				}
				if (ReflectionUtils.isEqualsMethod(method)) {
					return (proxy == args[0]);
				}
				if (ReflectionUtils.isHashCodeMethod(method)) {
					return System.identityHashCode(proxy);
				}
			}

			switch (method.getName()) {
				case "unwrap":
					return this.connection;
				case "close":
					// Handle close method: only close if not within a transaction.
					return ConnectionFactoryUtils.doReleaseConnection(this.connection, this.targetConnectionFactory)
							.doOnSubscribe(n -> this.closed = true);
				case "isClosed":
					return this.closed;
			}

			if (this.closed) {
				throw new IllegalStateException("Connection handle already closed");
			}

			// Invoke method on target Connection.
			try {
				return method.invoke(this.connection, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private String proxyToString(@Nullable Object proxy) {
			// Allow for differentiating between the proxy and the raw Connection.
			return "Transaction-aware proxy for target Connection [" + this.connection.toString() + "]";
		}

	}

}
