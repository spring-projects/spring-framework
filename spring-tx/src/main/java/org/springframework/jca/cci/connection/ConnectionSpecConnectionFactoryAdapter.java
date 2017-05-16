/*
 * Copyright 2002-2016 the original author or authors.
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

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionSpec;

import org.springframework.core.NamedThreadLocal;

/**
 * An adapter for a target CCI {@link javax.resource.cci.ConnectionFactory},
 * applying the given ConnectionSpec to every standard {@code getConnection()}
 * call, that is, implicitly invoking {@code getConnection(ConnectionSpec)}
 * on the target. All other methods simply delegate to the corresponding methods
 * of the target ConnectionFactory.
 *
 * <p>Can be used to proxy a target JNDI ConnectionFactory that does not have a
 * ConnectionSpec configured. Client code can work with the ConnectionFactory
 * without passing in a ConnectionSpec on every {@code getConnection()} call.
 *
 * <p>In the following example, client code can simply transparently work with
 * the preconfigured "myConnectionFactory", implicitly accessing
 * "myTargetConnectionFactory" with the specified user credentials.
 *
 * <pre class="code">
 * &lt;bean id="myTargetConnectionFactory" class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *   &lt;property name="jndiName" value="java:comp/env/cci/mycf"/&gt;
 * &lt;/bean>
 *
 * &lt;bean id="myConnectionFactory" class="org.springframework.jca.cci.connection.ConnectionSpecConnectionFactoryAdapter"&gt;
 *   &lt;property name="targetConnectionFactory" ref="myTargetConnectionFactory"/&gt;
 *   &lt;property name="connectionSpec"&gt;
 *     &lt;bean class="your.resource.adapter.ConnectionSpecImpl"&gt;
 *       &lt;property name="username" value="myusername"/&gt;
 *       &lt;property name="password" value="mypassword"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>If the "connectionSpec" is empty, this proxy will simply delegate to the
 * standard {@code getConnection()} method of the target ConnectionFactory.
 * This can be used to keep a UserCredentialsConnectionFactoryAdapter bean definition
 * just for the <i>option</i> of implicitly passing in a ConnectionSpec if the
 * particular target ConnectionFactory requires it.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #getConnection
 */
@SuppressWarnings("serial")
public class ConnectionSpecConnectionFactoryAdapter extends DelegatingConnectionFactory {

	private ConnectionSpec connectionSpec;

	private final ThreadLocal<ConnectionSpec> threadBoundSpec =
			new NamedThreadLocal<>("Current CCI ConnectionSpec");


	/**
	 * Set the ConnectionSpec that this adapter should use for retrieving Connections.
	 * Default is none.
	 */
	public void setConnectionSpec(ConnectionSpec connectionSpec) {
		this.connectionSpec = connectionSpec;
	}

	/**
	 * Set a ConnectionSpec for this proxy and the current thread.
	 * The given ConnectionSpec will be applied to all subsequent
	 * {@code getConnection()} calls on this ConnectionFactory proxy.
	 * <p>This will override any statically specified "connectionSpec" property.
	 * @param spec the ConnectionSpec to apply
	 * @see #removeConnectionSpecFromCurrentThread
	 */
	public void setConnectionSpecForCurrentThread(ConnectionSpec spec) {
		this.threadBoundSpec.set(spec);
	}

	/**
	 * Remove any ConnectionSpec for this proxy from the current thread.
	 * A statically specified ConnectionSpec applies again afterwards.
	 * @see #setConnectionSpecForCurrentThread
	 */
	public void removeConnectionSpecFromCurrentThread() {
		this.threadBoundSpec.remove();
	}


	/**
	 * Determine whether there is currently a thread-bound ConnectionSpec,
	 * using it if available, falling back to the statically specified
	 * "connectionSpec" property else.
	 * @see #doGetConnection
	 */
	@Override
	public final Connection getConnection() throws ResourceException {
		ConnectionSpec threadSpec = this.threadBoundSpec.get();
		if (threadSpec != null) {
			return doGetConnection(threadSpec);
		}
		else {
			return doGetConnection(this.connectionSpec);
		}
	}

	/**
	 * This implementation delegates to the {@code getConnection(ConnectionSpec)}
	 * method of the target ConnectionFactory, passing in the specified user credentials.
	 * If the specified username is empty, it will simply delegate to the standard
	 * {@code getConnection()} method of the target ConnectionFactory.
	 * @param spec the ConnectionSpec to apply
	 * @return the Connection
	 * @see javax.resource.cci.ConnectionFactory#getConnection(javax.resource.cci.ConnectionSpec)
	 * @see javax.resource.cci.ConnectionFactory#getConnection()
	 */
	protected Connection doGetConnection(ConnectionSpec spec) throws ResourceException {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalStateException("targetConnectionFactory is required");
		}
		if (spec != null) {
			return getTargetConnectionFactory().getConnection(spec);
		}
		else {
			return getTargetConnectionFactory().getConnection();
		}
	}

}
