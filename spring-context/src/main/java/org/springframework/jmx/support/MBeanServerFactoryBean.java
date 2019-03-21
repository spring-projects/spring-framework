/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.MBeanServerNotFoundException;

/**
 * {@link FactoryBean} that obtains an {@link javax.management.MBeanServer} reference
 * through the standard JMX 1.2 {@link javax.management.MBeanServerFactory}
 * API (which is available on JDK 1.5 or as part of a JMX 1.2 provider).
 * Exposes the {@code MBeanServer} for bean references.
 *
 * <p>By default, {@code MBeanServerFactoryBean} will always create
 * a new {@code MBeanServer} even if one is already running. To have
 * the {@code MBeanServerFactoryBean} attempt to locate a running
 * {@code MBeanServer} first, set the value of the
 * "locateExistingServerIfPossible" property to "true".
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setLocateExistingServerIfPossible
 * @see #locateMBeanServer
 * @see javax.management.MBeanServer
 * @see javax.management.MBeanServerFactory#findMBeanServer
 * @see javax.management.MBeanServerFactory#createMBeanServer
 * @see javax.management.MBeanServerFactory#newMBeanServer
 * @see MBeanServerConnectionFactoryBean
 * @see ConnectorServerFactoryBean
 */
public class MBeanServerFactoryBean implements FactoryBean<MBeanServer>, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private boolean locateExistingServerIfPossible = false;

	private String agentId;

	private String defaultDomain;

	private boolean registerWithFactory = true;

	private MBeanServer server;

	private boolean newlyRegistered = false;


	/**
	 * Set whether or not the {@code MBeanServerFactoryBean} should attempt
	 * to locate a running {@code MBeanServer} before creating one.
	 * <p>Default is {@code false}.
	 */
	public void setLocateExistingServerIfPossible(boolean locateExistingServerIfPossible) {
		this.locateExistingServerIfPossible = locateExistingServerIfPossible;
	}

	/**
	 * Set the agent id of the {@code MBeanServer} to locate.
	 * <p>Default is none. If specified, this will result in an
	 * automatic attempt being made to locate the attendant MBeanServer,
	 * and (importantly) if said MBeanServer cannot be located no
	 * attempt will be made to create a new MBeanServer (and an
	 * MBeanServerNotFoundException will be thrown at resolution time).
	 * <p>Specifying the empty String indicates the platform MBeanServer.
	 * @see javax.management.MBeanServerFactory#findMBeanServer(String)
	 */
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	/**
	 * Set the default domain to be used by the {@code MBeanServer},
	 * to be passed to {@code MBeanServerFactory.createMBeanServer()}
	 * or {@code MBeanServerFactory.findMBeanServer()}.
	 * <p>Default is none.
	 * @see javax.management.MBeanServerFactory#createMBeanServer(String)
	 * @see javax.management.MBeanServerFactory#findMBeanServer(String)
	 */
	public void setDefaultDomain(String defaultDomain) {
		this.defaultDomain = defaultDomain;
	}

	/**
	 * Set whether to register the {@code MBeanServer} with the
	 * {@code MBeanServerFactory}, making it available through
	 * {@code MBeanServerFactory.findMBeanServer()}.
	 * @see javax.management.MBeanServerFactory#createMBeanServer
	 * @see javax.management.MBeanServerFactory#findMBeanServer
	 */
	public void setRegisterWithFactory(boolean registerWithFactory) {
		this.registerWithFactory = registerWithFactory;
	}


	/**
	 * Creates the {@code MBeanServer} instance.
	 */
	@Override
	public void afterPropertiesSet() throws MBeanServerNotFoundException {
		// Try to locate existing MBeanServer, if desired.
		if (this.locateExistingServerIfPossible || this.agentId != null) {
			try {
				this.server = locateMBeanServer(this.agentId);
			}
			catch (MBeanServerNotFoundException ex) {
				// If agentId was specified, we were only supposed to locate that
				// specific MBeanServer; so let's bail if we can't find it.
				if (this.agentId != null) {
					throw ex;
				}
				logger.info("No existing MBeanServer found - creating new one");
			}
		}

		// Create a new MBeanServer and register it, if desired.
		if (this.server == null) {
			this.server = createMBeanServer(this.defaultDomain, this.registerWithFactory);
			this.newlyRegistered = this.registerWithFactory;
		}
	}

	/**
	 * Attempt to locate an existing {@code MBeanServer}.
	 * Called if {@code locateExistingServerIfPossible} is set to {@code true}.
	 * <p>The default implementation attempts to find an {@code MBeanServer} using
	 * a standard lookup. Subclasses may override to add additional location logic.
	 * @param agentId the agent identifier of the MBeanServer to retrieve.
	 * If this parameter is {@code null}, all registered MBeanServers are
	 * considered.
	 * @return the {@code MBeanServer} if found
	 * @throws org.springframework.jmx.MBeanServerNotFoundException
	 * if no {@code MBeanServer} could be found
	 * @see #setLocateExistingServerIfPossible
	 * @see JmxUtils#locateMBeanServer(String)
	 * @see javax.management.MBeanServerFactory#findMBeanServer(String)
	 */
	protected MBeanServer locateMBeanServer(String agentId) throws MBeanServerNotFoundException {
		return JmxUtils.locateMBeanServer(agentId);
	}

	/**
	 * Create a new {@code MBeanServer} instance and register it with the
	 * {@code MBeanServerFactory}, if desired.
	 * @param defaultDomain the default domain, or {@code null} if none
	 * @param registerWithFactory whether to register the {@code MBeanServer}
	 * with the {@code MBeanServerFactory}
	 * @see javax.management.MBeanServerFactory#createMBeanServer
	 * @see javax.management.MBeanServerFactory#newMBeanServer
	 */
	protected MBeanServer createMBeanServer(String defaultDomain, boolean registerWithFactory) {
		if (registerWithFactory) {
			return MBeanServerFactory.createMBeanServer(defaultDomain);
		}
		else {
			return MBeanServerFactory.newMBeanServer(defaultDomain);
		}
	}


	@Override
	public MBeanServer getObject() {
		return this.server;
	}

	@Override
	public Class<? extends MBeanServer> getObjectType() {
		return (this.server != null ? this.server.getClass() : MBeanServer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * Unregisters the {@code MBeanServer} instance, if necessary.
	 */
	@Override
	public void destroy() {
		if (this.newlyRegistered) {
			MBeanServerFactory.releaseMBeanServer(this.server);
		}
	}

}
