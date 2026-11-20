/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.access;

import java.io.IOException;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.jmx.support.JmxUtils;

/**
 * Internal helper class for managing a JMX connector.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
class ConnectorDelegate {

	private static final Log logger = LogFactory.getLog(ConnectorDelegate.class);

	private @Nullable JMXConnector connector;


	/**
	 * Connects to the remote {@code MBeanServer} using the configured {@code JMXServiceURL}:
	 * to the specified JMX service, or to a local MBeanServer if no service URL specified.
	 * @param serviceUrl the JMX service URL to connect to (may be {@code null})
	 * @param environment the JMX environment for the connector (may be {@code null})
	 * @param agentId the local JMX MBeanServer's agent id (may be {@code null})
	 */
	public MBeanServerConnection connect(@Nullable JMXServiceURL serviceUrl, @Nullable Map<String, ?> environment, @Nullable String agentId)
			throws MBeanServerNotFoundException {

		if (serviceUrl != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Connecting to remote MBeanServer at URL [" + serviceUrl + "]");
			}
			try {
				this.connector = JMXConnectorFactory.connect(serviceUrl, environment);
				return this.connector.getMBeanServerConnection();
			}
			catch (IOException ex) {
				throw new MBeanServerNotFoundException("Could not connect to remote MBeanServer [" + serviceUrl + "]", ex);
			}
		}
		else {
			logger.debug("Attempting to locate local MBeanServer");
			return JmxUtils.locateMBeanServer(agentId);
		}
	}

	/**
	 * Closes any {@code JMXConnector} that may be managed by this interceptor.
	 */
	public void close() {
		if (this.connector != null) {
			try {
				this.connector.close();
			}
			catch (IOException ex) {
				logger.debug("Could not close JMX connector", ex);
			}
		}
	}

}
