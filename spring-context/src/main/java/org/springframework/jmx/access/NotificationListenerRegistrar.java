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
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.JmxException;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.jmx.support.NotificationListenerHolder;
import org.springframework.util.CollectionUtils;

/**
 * Registrar object that associates a specific {@link javax.management.NotificationListener}
 * with one or more MBeans in an {@link javax.management.MBeanServer}
 * (typically via a {@link javax.management.MBeanServerConnection}).
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see #setServer
 * @see #setMappedObjectNames
 * @see #setNotificationListener
 */
public class NotificationListenerRegistrar extends NotificationListenerHolder
		implements InitializingBean, DisposableBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private MBeanServerConnection server;

	private JMXServiceURL serviceUrl;

	private Map<String, ?> environment;

	private String agentId;

	private final ConnectorDelegate connector = new ConnectorDelegate();

	private ObjectName[] actualObjectNames;


	/**
	 * Set the {@code MBeanServerConnection} used to connect to the
	 * MBean which all invocations are routed to.
	 */
	public void setServer(MBeanServerConnection server) {
		this.server = server;
	}

	/**
	 * Specify the environment for the JMX connector.
	 * @see javax.management.remote.JMXConnectorFactory#connect(javax.management.remote.JMXServiceURL, java.util.Map)
	 */
	public void setEnvironment(Map<String, ?> environment) {
		this.environment = environment;
	}

	/**
	 * Allow Map access to the environment to be set for the connector,
	 * with the option to add or override specific entries.
	 * <p>Useful for specifying entries directly, for example via
	 * "environment[myKey]". This is particularly useful for
	 * adding or overriding entries in child bean definitions.
	 */
	public Map<String, ?> getEnvironment() {
		return this.environment;
	}

	/**
	 * Set the service URL of the remote {@code MBeanServer}.
	 */
	public void setServiceUrl(String url) throws MalformedURLException {
		this.serviceUrl = new JMXServiceURL(url);
	}

	/**
	 * Set the agent id of the {@code MBeanServer} to locate.
	 * <p>Default is none. If specified, this will result in an
	 * attempt being made to locate the attendant MBeanServer, unless
	 * the {@link #setServiceUrl "serviceUrl"} property has been set.
	 * @see javax.management.MBeanServerFactory#findMBeanServer(String)
	 * <p>Specifying the empty String indicates the platform MBeanServer.
	 */
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}


	public void afterPropertiesSet() {
		if (getNotificationListener() == null) {
			throw new IllegalArgumentException("Property 'notificationListener' is required");
		}
		if (CollectionUtils.isEmpty(this.mappedObjectNames)) {
			throw new IllegalArgumentException("Property 'mappedObjectName' is required");
		}
		prepare();
	}

	/**
	 * Registers the specified {@code NotificationListener}.
	 * <p>Ensures that an {@code MBeanServerConnection} is configured and attempts
	 * to detect a local connection if one is not supplied.
	 */
	public void prepare() {
		if (this.server == null) {
			this.server = this.connector.connect(this.serviceUrl, this.environment, this.agentId);
		}
		try {
			this.actualObjectNames = getResolvedObjectNames();
			if (logger.isDebugEnabled()) {
				logger.debug("Registering NotificationListener for MBeans " + Arrays.asList(this.actualObjectNames));
			}
			for (ObjectName actualObjectName : this.actualObjectNames) {
				this.server.addNotificationListener(
						actualObjectName, getNotificationListener(), getNotificationFilter(), getHandback());
			}
		}
		catch (IOException ex) {
			throw new MBeanServerNotFoundException(
					"Could not connect to remote MBeanServer at URL [" + this.serviceUrl + "]", ex);
		}
		catch (Exception ex) {
			throw new JmxException("Unable to register NotificationListener", ex);
		}
	}

	/**
	 * Unregisters the specified {@code NotificationListener}.
	 */
	public void destroy() {
		try {
			if (this.actualObjectNames != null) {
				for (ObjectName actualObjectName : this.actualObjectNames) {
					try {
						this.server.removeNotificationListener(
								actualObjectName, getNotificationListener(), getNotificationFilter(), getHandback());
					}
					catch (Exception ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Unable to unregister NotificationListener", ex);
						}
					}
				}
			}
		}
		finally {
			this.connector.close();
		}
	}

}
