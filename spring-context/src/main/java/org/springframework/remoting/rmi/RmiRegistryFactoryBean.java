/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.remoting.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * {@link FactoryBean} that locates a {@link java.rmi.registry.Registry} and
 * exposes it for bean references. Can also create a local RMI registry
 * on the fly if none exists already.
 *
 * <p>Can be used to set up and pass around the actual Registry object to
 * applications objects that need to work with RMI. One example for such an
 * object that needs to work with RMI is Spring's {@link RmiServiceExporter},
 * which either works with a passed-in Registry reference or falls back to
 * the registry as specified by its local properties and defaults.
 *
 * <p>Also useful to enforce creation of a local RMI registry at a given port,
 * for example for a JMX connector. If used in conjunction with
 * {@link org.springframework.jmx.support.ConnectorServerFactoryBean},
 * it is recommended to mark the connector definition (ConnectorServerFactoryBean)
 * as "depends-on" the registry definition (RmiRegistryFactoryBean),
 * to guarantee starting up the registry first.
 *
 * <p>Note: The implementation of this class mirrors the corresponding logic
 * in {@link RmiServiceExporter}, and also offers the same customization hooks.
 * RmiServiceExporter implements its own registry lookup as a convenience:
 * It is very common to simply rely on the registry defaults.
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 * @see RmiServiceExporter#setRegistry
 * @see org.springframework.jmx.support.ConnectorServerFactoryBean
 * @see java.rmi.registry.Registry
 * @see java.rmi.registry.LocateRegistry
 */
public class RmiRegistryFactoryBean implements FactoryBean<Registry>, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private String host;

	private int port = Registry.REGISTRY_PORT;

	private RMIClientSocketFactory clientSocketFactory;

	private RMIServerSocketFactory serverSocketFactory;

	private Registry registry;

	private boolean alwaysCreate = false;

	private boolean created = false;


	/**
	 * Set the host of the registry for the exported RMI service,
	 * i.e. {@code rmi://HOST:port/name}
	 * <p>Default is localhost.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Return the host of the registry for the exported RMI service.
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Set the port of the registry for the exported RMI service,
	 * i.e. {@code rmi://host:PORT/name}
	 * <p>Default is {@code Registry.REGISTRY_PORT} (1099).
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Return the port of the registry for the exported RMI service.
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Set a custom RMI client socket factory to use for the RMI registry.
	 * <p>If the given object also implements {@code java.rmi.server.RMIServerSocketFactory},
	 * it will automatically be registered as server socket factory too.
	 * @see #setServerSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see java.rmi.registry.LocateRegistry#getRegistry(String, int, java.rmi.server.RMIClientSocketFactory)
	 */
	public void setClientSocketFactory(RMIClientSocketFactory clientSocketFactory) {
		this.clientSocketFactory = clientSocketFactory;
	}

	/**
	 * Set a custom RMI server socket factory to use for the RMI registry.
	 * <p>Only needs to be specified when the client socket factory does not
	 * implement {@code java.rmi.server.RMIServerSocketFactory} already.
	 * @see #setClientSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see java.rmi.registry.LocateRegistry#createRegistry(int, RMIClientSocketFactory, java.rmi.server.RMIServerSocketFactory)
	 */
	public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}

	/**
	 * Set whether to always create the registry in-process,
	 * not attempting to locate an existing registry at the specified port.
	 * <p>Default is "false". Switch this flag to "true" in order to avoid
	 * the overhead of locating an existing registry when you always
	 * intend to create a new registry in any case.
	 */
	public void setAlwaysCreate(boolean alwaysCreate) {
		this.alwaysCreate = alwaysCreate;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		// Check socket factories for registry.
		if (this.clientSocketFactory instanceof RMIServerSocketFactory) {
			this.serverSocketFactory = (RMIServerSocketFactory) this.clientSocketFactory;
		}
		if ((this.clientSocketFactory != null && this.serverSocketFactory == null) ||
				(this.clientSocketFactory == null && this.serverSocketFactory != null)) {
			throw new IllegalArgumentException(
					"Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
		}

		// Fetch RMI registry to expose.
		this.registry = getRegistry(this.host, this.port, this.clientSocketFactory, this.serverSocketFactory);
	}


	/**
	 * Locate or create the RMI registry.
	 * @param registryHost the registry host to use (if this is specified,
	 * no implicit creation of a RMI registry will happen)
	 * @param registryPort the registry port to use
	 * @param clientSocketFactory the RMI client socket factory for the registry (if any)
	 * @param serverSocketFactory the RMI server socket factory for the registry (if any)
	 * @return the RMI registry
	 * @throws java.rmi.RemoteException if the registry couldn't be located or created
	 */
	protected Registry getRegistry(String registryHost, int registryPort,
			@Nullable RMIClientSocketFactory clientSocketFactory, @Nullable RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (registryHost != null) {
			// Host explicitly specified: only lookup possible.
			if (logger.isDebugEnabled()) {
				logger.debug("Looking for RMI registry at port '" + registryPort + "' of host [" + registryHost + "]");
			}
			Registry reg = LocateRegistry.getRegistry(registryHost, registryPort, clientSocketFactory);
			testRegistry(reg);
			return reg;
		}

		else {
			return getRegistry(registryPort, clientSocketFactory, serverSocketFactory);
		}
	}

	/**
	 * Locate or create the RMI registry.
	 * @param registryPort the registry port to use
	 * @param clientSocketFactory the RMI client socket factory for the registry (if any)
	 * @param serverSocketFactory the RMI server socket factory for the registry (if any)
	 * @return the RMI registry
	 * @throws RemoteException if the registry couldn't be located or created
	 */
	protected Registry getRegistry(int registryPort,
			@Nullable RMIClientSocketFactory clientSocketFactory, @Nullable RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (clientSocketFactory != null) {
			if (this.alwaysCreate) {
				logger.debug("Creating new RMI registry");
				this.created = true;
				return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Looking for RMI registry at port '" + registryPort + "', using custom socket factory");
			}
			synchronized (LocateRegistry.class) {
				try {
					// Retrieve existing registry.
					Registry reg = LocateRegistry.getRegistry(null, registryPort, clientSocketFactory);
					testRegistry(reg);
					return reg;
				}
				catch (RemoteException ex) {
					logger.trace("RMI registry access threw exception", ex);
					logger.debug("Could not detect RMI registry - creating new one");
					// Assume no registry found -> create new one.
					this.created = true;
					return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
				}
			}
		}

		else {
			return getRegistry(registryPort);
		}
	}

	/**
	 * Locate or create the RMI registry.
	 * @param registryPort the registry port to use
	 * @return the RMI registry
	 * @throws RemoteException if the registry couldn't be located or created
	 */
	protected Registry getRegistry(int registryPort) throws RemoteException {
		if (this.alwaysCreate) {
			logger.debug("Creating new RMI registry");
			this.created = true;
			return LocateRegistry.createRegistry(registryPort);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for RMI registry at port '" + registryPort + "'");
		}
		synchronized (LocateRegistry.class) {
			try {
				// Retrieve existing registry.
				Registry reg = LocateRegistry.getRegistry(registryPort);
				testRegistry(reg);
				return reg;
			}
			catch (RemoteException ex) {
				logger.trace("RMI registry access threw exception", ex);
				logger.debug("Could not detect RMI registry - creating new one");
				// Assume no registry found -> create new one.
				this.created = true;
				return LocateRegistry.createRegistry(registryPort);
			}
		}
	}

	/**
	 * Test the given RMI registry, calling some operation on it to
	 * check whether it is still active.
	 * <p>Default implementation calls {@code Registry.list()}.
	 * @param registry the RMI registry to test
	 * @throws RemoteException if thrown by registry methods
	 * @see java.rmi.registry.Registry#list()
	 */
	protected void testRegistry(Registry registry) throws RemoteException {
		registry.list();
	}


	@Override
	public Registry getObject() throws Exception {
		return this.registry;
	}

	@Override
	public Class<? extends Registry> getObjectType() {
		return (this.registry != null ? this.registry.getClass() : Registry.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * Unexport the RMI registry on bean factory shutdown,
	 * provided that this bean actually created a registry.
	 */
	@Override
	public void destroy() throws RemoteException {
		if (this.created) {
			logger.debug("Unexporting RMI registry");
			UnicastRemoteObject.unexportObject(this.registry, true);
		}
	}

}
