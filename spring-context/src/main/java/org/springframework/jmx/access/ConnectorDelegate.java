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

package org.springframework.jmx.access;

import java.io.IOException;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.jmx.support.JmxUtils;

/**
 * Internal helper class for managing a JMX connector.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
class ConnectorDelegate {

	private final static Log logger = LogFactory.getLog(ConnectorDelegate.class);

	private JMXConnector connector;


	/**
	 * Connects to the remote {@code MBeanServer} using the configured {@code JMXServiceURL}:
	 * to the specified JMX service, or to a local MBeanServer if no service URL specified.
	 * @param serviceUrl the JMX service URL to connect to (may be {@code null})
	 * @param environment the JMX environment for the connector (may be {@code null})
	 * @param agentId the local JMX MBeanServer's agent id (may be {@code null})
	 */
	public MBeanServerConnection connect(JMXServiceURL serviceUrl, Map<String, ?> environment, String agentId)
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
