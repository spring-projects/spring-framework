/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jca.cci.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;

/**
 * Proxy for a target CCI {@link javax.resource.cci.ConnectionFactory}, adding
 * awareness of Spring-managed transactions. Similar to a transactional JNDI
 * ConnectionFactory as provided by a J2EE server.
 *
 * <p>Data access code that should remain unaware of Spring's data access support
 * can work with this proxy to seamlessly participate in Spring-managed transactions.
 * Note that the transaction manager, for example the {@link CciLocalTransactionManager},
 * still needs to work with underlying ConnectionFactory, <i>not</i> with this proxy.
 *
 * <p><b>Make sure that TransactionAwareConnectionFactoryProxy is the outermost
 * ConnectionFactory of a chain of ConnectionFactory proxies/adapters.</b>
 * TransactionAwareConnectionFactoryProxy can delegate either directly to the
 * target connection pool or to some intermediate proxy/adapter like
 * {@link ConnectionSpecConnectionFactoryAdapter}.
 *
 * <p>Delegates to {@link ConnectionFactoryUtils} for automatically participating in
 * thread-bound transactions, for example managed by {@link CciLocalTransactionManager}.
 * {@code getConnection} calls and {@code close} calls on returned Connections
 * will behave properly within a transaction, i.e. always operate on the transactional
 * Connection. If not within a transaction, normal ConnectionFactory behavior applies.
 *
 * <p>This proxy allows data access code to work with the plain JCA CCI API and still
 * participate in Spring-managed transactions, similar to CCI code in a J2EE/JTA
 * environment. However, if possible, use Spring's ConnectionFactoryUtils, CciTemplate or
 * CCI operation objects to get transaction participation even without a proxy for
 * the target ConnectionFactory, avoiding the need to define such a proxy in the first place.
 *
 * <p><b>NOTE:</b> This ConnectionFactory proxy needs to return wrapped Connections
 * in order to handle close calls properly. Therefore, the returned Connections cannot
 * be cast to a native CCI Connection type or to a connection pool implementation type.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see javax.resource.cci.ConnectionFactory#getConnection
 * @see javax.resource.cci.Connection#close
 * @see ConnectionFactoryUtils#doGetConnection
 * @see ConnectionFactoryUtils#doReleaseConnection
 */
@SuppressWarnings("serial")
public class TransactionAwareConnectionFactoryProxy extends DelegatingConnectionFactory {

	/**
	 * Create a new TransactionAwareConnectionFactoryProxy.
	 * @see #setTargetConnectionFactory
	 */
	public TransactionAwareConnectionFactoryProxy() {
	}

	/**
	 * Create a new TransactionAwareConnectionFactoryProxy.
	 * @param targetConnectionFactory the target ConnectionFactory
	 */
	public TransactionAwareConnectionFactoryProxy(ConnectionFactory targetConnectionFactory) {
		setTargetConnectionFactory(targetConnectionFactory);
		afterPropertiesSet();
	}


	/**
	 * Delegate to ConnectionFactoryUtils for automatically participating in Spring-managed
	 * transactions. Throws the original ResourceException, if any.
	 * @return a transactional Connection if any, a new one else
	 * @see org.springframework.jca.cci.connection.ConnectionFactoryUtils#doGetConnection
	 */
	@Override
	public Connection getConnection() throws ResourceException {
		Connection con = ConnectionFactoryUtils.doGetConnection(getTargetConnectionFactory());
		return getTransactionAwareConnectionProxy(con, getTargetConnectionFactory());
	}

	/**
	 * Wrap the given Connection with a proxy that delegates every method call to it
	 * but delegates {@code close} calls to ConnectionFactoryUtils.
	 * @param target the original Connection to wrap
	 * @param cf ConnectionFactory that the Connection came from
	 * @return the wrapped Connection
	 * @see javax.resource.cci.Connection#close()
	 * @see ConnectionFactoryUtils#doReleaseConnection
	 */
	protected Connection getTransactionAwareConnectionProxy(Connection target, ConnectionFactory cf) {
		return (Connection) Proxy.newProxyInstance(
				Connection.class.getClassLoader(),
				new Class<?>[] {Connection.class},
				new TransactionAwareInvocationHandler(target, cf));
	}


	/**
	 * Invocation handler that delegates close calls on CCI Connections
	 * to ConnectionFactoryUtils for being aware of thread-bound transactions.
	 */
	private static class TransactionAwareInvocationHandler implements InvocationHandler {

		private final Connection target;

		private final ConnectionFactory connectionFactory;

		public TransactionAwareInvocationHandler(Connection target, ConnectionFactory cf) {
			this.target = target;
			this.connectionFactory = cf;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on Connection interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of Connection proxy.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("getLocalTransaction")) {
				if (ConnectionFactoryUtils.isConnectionTransactional(this.target, this.connectionFactory)) {
					throw new javax.resource.spi.IllegalStateException(
							"Local transaction handling not allowed within a managed transaction");
				}
			}
			else if (method.getName().equals("close")) {
				// Handle close method: only close if not within a transaction.
				ConnectionFactoryUtils.doReleaseConnection(this.target, this.connectionFactory);
				return null;
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
