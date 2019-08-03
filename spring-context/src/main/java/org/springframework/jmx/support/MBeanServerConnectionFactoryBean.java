/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jmx.support;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.AbstractLazyCreationTargetSource;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * {@link FactoryBean} that creates a JMX 1.2 {@code MBeanServerConnection}
 * to a remote {@code MBeanServer} exposed via a {@code JMXServerConnector}.
 * Exposes the {@code MBeanServer} for bean references.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see MBeanServerFactoryBean
 * @see ConnectorServerFactoryBean
 * @see org.springframework.jmx.access.MBeanClientInterceptor#setServer
 * @see org.springframework.jmx.access.NotificationListenerRegistrar#setServer
 */
public class MBeanServerConnectionFactoryBean
		implements FactoryBean<MBeanServerConnection>, BeanClassLoaderAware, InitializingBean, DisposableBean {

	@Nullable
	private JMXServiceURL serviceUrl;

	private Map<String, Object> environment = new HashMap<>();

	private boolean connectOnStartup = true;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private JMXConnector connector;

	@Nullable
	private MBeanServerConnection connection;

	@Nullable
	private JMXConnectorLazyInitTargetSource connectorTargetSource;


	/**
	 * Set the service URL of the remote {@code MBeanServer}.
	 */
	public void setServiceUrl(String url) throws MalformedURLException {
		this.serviceUrl = new JMXServiceURL(url);
	}

	/**
	 * Set the environment properties used to construct the {@code JMXConnector}
	 * as {@code java.util.Properties} (String key/value pairs).
	 */
	public void setEnvironment(Properties environment) {
		CollectionUtils.mergePropertiesIntoMap(environment, this.environment);
	}

	/**
	 * Set the environment properties used to construct the {@code JMXConnector}
	 * as a {@code Map} of String keys and arbitrary Object values.
	 */
	public void setEnvironmentMap(@Nullable Map<String, ?> environment) {
		if (environment != null) {
			this.environment.putAll(environment);
		}
	}

	/**
	 * Set whether to connect to the server on startup. Default is "true".
	 * <p>Can be turned off to allow for late start of the JMX server.
	 * In this case, the JMX connector will be fetched on first access.
	 */
	public void setConnectOnStartup(boolean connectOnStartup) {
		this.connectOnStartup = connectOnStartup;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	/**
	 * Creates a {@code JMXConnector} for the given settings
	 * and exposes the associated {@code MBeanServerConnection}.
	 */
	@Override
	public void afterPropertiesSet() throws IOException {
		if (this.serviceUrl == null) {
			throw new IllegalArgumentException("Property 'serviceUrl' is required");
		}

		if (this.connectOnStartup) {
			connect();
		}
		else {
			createLazyConnection();
		}
	}

	/**
	 * Connects to the remote {@code MBeanServer} using the configured service URL and
	 * environment properties.
	 */
	private void connect() throws IOException {
		Assert.state(this.serviceUrl != null, "No JMXServiceURL set");
		this.connector = JMXConnectorFactory.connect(this.serviceUrl, this.environment);
		this.connection = this.connector.getMBeanServerConnection();
	}

	/**
	 * Creates lazy proxies for the {@code JMXConnector} and {@code MBeanServerConnection}.
	 */
	private void createLazyConnection() {
		this.connectorTargetSource = new JMXConnectorLazyInitTargetSource();
		TargetSource connectionTargetSource = new MBeanServerConnectionLazyInitTargetSource();

		this.connector = (JMXConnector)
				new ProxyFactory(JMXConnector.class, this.connectorTargetSource).getProxy(this.beanClassLoader);
		this.connection = (MBeanServerConnection)
				new ProxyFactory(MBeanServerConnection.class, connectionTargetSource).getProxy(this.beanClassLoader);
	}


	@Override
	@Nullable
	public MBeanServerConnection getObject() {
		return this.connection;
	}

	@Override
	public Class<? extends MBeanServerConnection> getObjectType() {
		return (this.connection != null ? this.connection.getClass() : MBeanServerConnection.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * Closes the underlying {@code JMXConnector}.
	 */
	@Override
	public void destroy() throws IOException {
		if (this.connector != null &&
				(this.connectorTargetSource == null || this.connectorTargetSource.isInitialized())) {
			this.connector.close();
		}
	}


	/**
	 * Lazily creates a {@code JMXConnector} using the configured service URL
	 * and environment properties.
	 * @see MBeanServerConnectionFactoryBean#setServiceUrl(String)
	 * @see MBeanServerConnectionFactoryBean#setEnvironment(java.util.Properties)
	 */
	private class JMXConnectorLazyInitTargetSource extends AbstractLazyCreationTargetSource {

		@Override
		protected Object createObject() throws Exception {
			Assert.state(serviceUrl != null, "No JMXServiceURL set");
			return JMXConnectorFactory.connect(serviceUrl, environment);
		}

		@Override
		public Class<?> getTargetClass() {
			return JMXConnector.class;
		}
	}


	/**
	 * Lazily creates an {@code MBeanServerConnection}.
	 */
	private class MBeanServerConnectionLazyInitTargetSource extends AbstractLazyCreationTargetSource {

		@Override
		protected Object createObject() throws Exception {
			Assert.state(connector != null, "JMXConnector not initialized");
			return connector.getMBeanServerConnection();
		}

		@Override
		public Class<?> getTargetClass() {
			return MBeanServerConnection.class;
		}
	}

}
