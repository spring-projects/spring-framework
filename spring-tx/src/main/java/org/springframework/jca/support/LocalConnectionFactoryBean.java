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

package org.springframework.jca.support;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ManagedConnectionFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that creates
 * a local JCA connection factory in "non-managed" mode (as defined by the
 * Java Connector Architecture specification). This is a direct alternative
 * to a {@link org.springframework.jndi.JndiObjectFactoryBean} definition that
 * obtains a connection factory handle from a Jakarta EE server's naming environment.
 *
 * <p>The type of the connection factory is dependent on the actual connector:
 * the connector can either expose its native API (such as a JDBC
 * {@link javax.sql.DataSource} or a JMS {@link jakarta.jms.ConnectionFactory})
 * or follow the standard Common Client Interface (CCI), as defined by the JCA spec.
 * The exposed interface in the CCI case is {@link jakarta.resource.cci.ConnectionFactory}.
 *
 * <p>In order to use this FactoryBean, you must specify the connector's
 * {@link #setManagedConnectionFactory "managedConnectionFactory"} (usually
 * configured as separate JavaBean), which will be used to create the actual
 * connection factory reference as exposed to the application. Optionally,
 * you can also specify a {@link #setConnectionManager "connectionManager"},
 * in order to use a custom ConnectionManager instead of the connector's default.
 *
 * <p><b>NOTE:</b> In non-managed mode, a connector is not deployed on an
 * application server, or more specifically not interacting with an application
 * server. Consequently, it cannot use a Jakarta EE server's system contracts:
 * connection management, transaction management, and security management.
 * A custom ConnectionManager implementation has to be used for applying those
 * services in conjunction with a standalone transaction coordinator etc.
 *
 * <p>The connector will use a local ConnectionManager (included in the connector)
 * by default, which cannot participate in global transactions due to the lack
 * of XA enlistment. You need to specify an XA-capable ConnectionManager in
 * order to make the connector interact with an XA transaction coordinator.
 * Alternatively, simply use the native local transaction facilities of the
 * exposed API (for example, CCI local transactions), or use a corresponding
 * implementation of Spring's PlatformTransactionManager SPI to drive local
 * transactions.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setManagedConnectionFactory
 * @see #setConnectionManager
 * @see jakarta.resource.cci.ConnectionFactory
 * @see jakarta.resource.cci.Connection#getLocalTransaction
 */
public class LocalConnectionFactoryBean implements FactoryBean<Object>, InitializingBean {

	private @Nullable ManagedConnectionFactory managedConnectionFactory;

	private @Nullable ConnectionManager connectionManager;

	private @Nullable Object connectionFactory;


	/**
	 * Set the JCA ManagerConnectionFactory that should be used to create
	 * the desired connection factory.
	 * <p>The ManagerConnectionFactory will usually be set up as separate bean
	 * (potentially as inner bean), populated with JavaBean properties:
	 * a ManagerConnectionFactory is encouraged to follow the JavaBean pattern
	 * by the JCA specification, analogous to a JDBC DataSource and a JPA
	 * EntityManagerFactory.
	 * <p>Note that the ManagerConnectionFactory implementation might expect
	 * a reference to its JCA 1.7 ResourceAdapter, expressed through the
	 * {@link jakarta.resource.spi.ResourceAdapterAssociation} interface.
	 * Simply inject the corresponding ResourceAdapter instance into its
	 * "resourceAdapter" bean property in this case, before passing the
	 * ManagerConnectionFactory into this LocalConnectionFactoryBean.
	 * @see jakarta.resource.spi.ManagedConnectionFactory#createConnectionFactory()
	 */
	public void setManagedConnectionFactory(ManagedConnectionFactory managedConnectionFactory) {
		this.managedConnectionFactory = managedConnectionFactory;
	}

	/**
	 * Set the JCA ConnectionManager that should be used to create the
	 * desired connection factory.
	 * <p>A ConnectionManager implementation for local usage is often
	 * included with a JCA connector. Such an included ConnectionManager
	 * might be set as default, with no need to explicitly specify one.
	 * @see jakarta.resource.spi.ManagedConnectionFactory#createConnectionFactory(jakarta.resource.spi.ConnectionManager)
	 */
	public void setConnectionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Override
	public void afterPropertiesSet() throws ResourceException {
		if (this.managedConnectionFactory == null) {
			throw new IllegalArgumentException("Property 'managedConnectionFactory' is required");
		}
		if (this.connectionManager != null) {
			this.connectionFactory = this.managedConnectionFactory.createConnectionFactory(this.connectionManager);
		}
		else {
			this.connectionFactory = this.managedConnectionFactory.createConnectionFactory();
		}
	}


	@Override
	public @Nullable Object getObject() {
		return this.connectionFactory;
	}

	@Override
	public @Nullable Class<?> getObjectType() {
		return (this.connectionFactory != null ? this.connectionFactory.getClass() : null);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
