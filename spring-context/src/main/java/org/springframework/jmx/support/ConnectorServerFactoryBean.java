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

package org.springframework.jmx.support;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.MBeanServerForwarder;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.JmxException;
import org.springframework.util.CollectionUtils;

/**
 * {@link FactoryBean} that creates a JSR-160 {@link JMXConnectorServer},
 * optionally registers it with the {@link MBeanServer}, and then starts it.
 *
 * <p>The {@code JMXConnectorServer} can be started in a separate thread by setting the
 * {@code threaded} property to {@code true}. You can configure this thread to be a
 * daemon thread by setting the {@code daemon} property to {@code true}.
 *
 * <p>The {@code JMXConnectorServer} is correctly shut down when an instance of this
 * class is destroyed on shutdown of the containing {@code ApplicationContext}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see JMXConnectorServer
 * @see MBeanServer
 */
public class ConnectorServerFactoryBean extends MBeanRegistrationSupport
		implements FactoryBean<JMXConnectorServer>, InitializingBean, DisposableBean {

	/** The default service URL. */
	public static final String DEFAULT_SERVICE_URL = "service:jmx:jmxmp://localhost:9875";


	private String serviceUrl = DEFAULT_SERVICE_URL;

	private final Map<String, Object> environment = new HashMap<>();

	private @Nullable MBeanServerForwarder forwarder;

	private @Nullable ObjectName objectName;

	private boolean threaded = false;

	private boolean daemon = false;

	private @Nullable JMXConnectorServer connectorServer;


	/**
	 * Set the service URL for the {@code JMXConnectorServer}.
	 */
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * Set the environment properties used to construct the {@code JMXConnectorServer}
	 * as {@code java.util.Properties} (String key/value pairs).
	 */
	public void setEnvironment(@Nullable Properties environment) {
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
	 * Set an MBeanServerForwarder to be applied to the {@code JMXConnectorServer}.
	 */
	public void setForwarder(MBeanServerForwarder forwarder) {
		this.forwarder = forwarder;
	}

	/**
	 * Set the {@code ObjectName} used to register the {@code JMXConnectorServer}
	 * itself with the {@code MBeanServer}, as {@code ObjectName} instance
	 * or as {@code String}.
	 * @throws MalformedObjectNameException if the {@code ObjectName} is malformed
	 */
	public void setObjectName(Object objectName) throws MalformedObjectNameException {
		this.objectName = ObjectNameManager.getInstance(objectName);
	}

	/**
	 * Set whether the {@code JMXConnectorServer} should be started in a separate thread.
	 */
	public void setThreaded(boolean threaded) {
		this.threaded = threaded;
	}

	/**
	 * Set whether any threads started for the {@code JMXConnectorServer} should be
	 * started as daemon threads.
	 */
	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
	}


	/**
	 * Start the connector server. If the {@code threaded} flag is set to {@code true},
	 * the {@code JMXConnectorServer} will be started in a separate thread.
	 * If the {@code daemon} flag is set to {@code true}, that thread will be
	 * started as a daemon thread.
	 * @throws JMException if a problem occurred when registering the connector server
	 * with the {@code MBeanServer}
	 * @throws IOException if there is a problem starting the connector server
	 */
	@Override
	public void afterPropertiesSet() throws JMException, IOException {
		if (this.server == null) {
			this.server = JmxUtils.locateMBeanServer();
		}

		// Create the JMX service URL.
		JMXServiceURL url = new JMXServiceURL(this.serviceUrl);

		// Create the connector server now.
		this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, this.environment, this.server);

		// Set the given MBeanServerForwarder, if any.
		if (this.forwarder != null) {
			this.connectorServer.setMBeanServerForwarder(this.forwarder);
		}

		// Do we want to register the connector with the MBean server?
		if (this.objectName != null) {
			doRegister(this.connectorServer, this.objectName);
		}

		try {
			if (this.threaded) {
				// Start the connector server asynchronously (in a separate thread).
				final JMXConnectorServer serverToStart = this.connectorServer;
				Thread connectorThread = new Thread() {
					@Override
					public void run() {
						try {
							serverToStart.start();
						}
						catch (IOException ex) {
							throw new JmxException("Could not start JMX connector server after delay", ex);
						}
					}
				};

				connectorThread.setName("JMX Connector Thread [" + this.serviceUrl + "]");
				connectorThread.setDaemon(this.daemon);
				connectorThread.start();
			}
			else {
				// Start the connector server in the same thread.
				this.connectorServer.start();
			}

			if (logger.isInfoEnabled()) {
				logger.info("JMX connector server started: " + this.connectorServer);
			}
		}

		catch (IOException ex) {
			// Unregister the connector server if startup failed.
			unregisterBeans();
			throw ex;
		}
	}


	@Override
	public @Nullable JMXConnectorServer getObject() {
		return this.connectorServer;
	}

	@Override
	public Class<? extends JMXConnectorServer> getObjectType() {
		return (this.connectorServer != null ? this.connectorServer.getClass() : JMXConnectorServer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * Stop the {@code JMXConnectorServer} managed by an instance of this class.
	 * Automatically called on {@code ApplicationContext} shutdown.
	 * @throws IOException if there is an error stopping the connector server
	 */
	@Override
	public void destroy() throws IOException {
		try {
			if (this.connectorServer != null) {
				if (logger.isInfoEnabled()) {
					logger.info("Stopping JMX connector server: " + this.connectorServer);
				}
				this.connectorServer.stop();
			}
		}
		finally {
			unregisterBeans();
		}
	}

}
