/*
 * Copyright 2002-2009 the original author or authors.
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

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * RMI exporter that exposes the specified service as RMI object with the specified name.
 * Such services can be accessed via plain RMI or via {@link RmiProxyFactoryBean}.
 * Also supports exposing any non-RMI service via RMI invokers, to be accessed via
 * {@link RmiClientInterceptor} / {@link RmiProxyFactoryBean}'s automatic detection
 * of such invokers.
 *
 * <p>With an RMI invoker, RMI communication works on the {@link RmiInvocationHandler}
 * level, needing only one stub for any service. Service interfaces do not have to
 * extend {@code java.rmi.Remote} or throw {@code java.rmi.RemoteException}
 * on all methods, but in and out parameters have to be serializable.
 *
 * <p>The major advantage of RMI, compared to Hessian and Burlap, is serialization.
 * Effectively, any serializable Java object can be transported without hassle.
 * Hessian and Burlap have their own (de-)serialization mechanisms, but are
 * HTTP-based and thus much easier to setup than RMI. Alternatively, consider
 * Spring's HTTP invoker to combine Java serialization with HTTP-based transport.
 *
 * <p>Note: RMI makes a best-effort attempt to obtain the fully qualified host name.
 * If one cannot be determined, it will fall back and use the IP address. Depending
 * on your network configuration, in some cases it will resolve the IP to the loopback
 * address. To ensure that RMI will use the host name bound to the correct network
 * interface, you should pass the {@code java.rmi.server.hostname} property to the
 * JVM that will export the registry and/or the service using the "-D" JVM argument.
 * For example: {@code -Djava.rmi.server.hostname=myserver.com}
 *
 * @author Juergen Hoeller
 * @since 13.05.2003
 * @see RmiClientInterceptor
 * @see RmiProxyFactoryBean
 * @see java.rmi.Remote
 * @see java.rmi.RemoteException
 * @see org.springframework.remoting.caucho.HessianServiceExporter
 * @see org.springframework.remoting.caucho.BurlapServiceExporter
 * @see org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter
 */
public class RmiServiceExporter extends RmiBasedExporter implements InitializingBean, DisposableBean {

	private String serviceName;

	private int servicePort = 0;  // anonymous port

	private RMIClientSocketFactory clientSocketFactory;

	private RMIServerSocketFactory serverSocketFactory;

	private Registry registry;

	private String registryHost;

	private int registryPort = Registry.REGISTRY_PORT;

	private RMIClientSocketFactory registryClientSocketFactory;

	private RMIServerSocketFactory registryServerSocketFactory;

	private boolean alwaysCreateRegistry = false;

	private boolean replaceExistingBinding = true;

	private Remote exportedObject;

	private boolean createdRegistry = false;


	/**
	 * Set the name of the exported RMI service,
	 * i.e. {@code rmi://host:port/NAME}
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Set the port that the exported RMI service will use.
	 * <p>Default is 0 (anonymous port).
	 */
	public void setServicePort(int servicePort) {
		this.servicePort = servicePort;
	}

	/**
	 * Set a custom RMI client socket factory to use for exporting the service.
	 * <p>If the given object also implements {@code java.rmi.server.RMIServerSocketFactory},
	 * it will automatically be registered as server socket factory too.
	 * @see #setServerSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see UnicastRemoteObject#exportObject(Remote, int, RMIClientSocketFactory, RMIServerSocketFactory)
	 */
	public void setClientSocketFactory(RMIClientSocketFactory clientSocketFactory) {
		this.clientSocketFactory = clientSocketFactory;
	}

	/**
	 * Set a custom RMI server socket factory to use for exporting the service.
	 * <p>Only needs to be specified when the client socket factory does not
	 * implement {@code java.rmi.server.RMIServerSocketFactory} already.
	 * @see #setClientSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see UnicastRemoteObject#exportObject(Remote, int, RMIClientSocketFactory, RMIServerSocketFactory)
	 */
	public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}

	/**
	 * Specify the RMI registry to register the exported service with.
	 * Typically used in combination with RmiRegistryFactoryBean.
	 * <p>Alternatively, you can specify all registry properties locally.
	 * This exporter will then try to locate the specified registry,
	 * automatically creating a new local one if appropriate.
	 * <p>Default is a local registry at the default port (1099),
	 * created on the fly if necessary.
	 * @see RmiRegistryFactoryBean
	 * @see #setRegistryHost
	 * @see #setRegistryPort
	 * @see #setRegistryClientSocketFactory
	 * @see #setRegistryServerSocketFactory
	 */
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	/**
	 * Set the host of the registry for the exported RMI service,
	 * i.e. {@code rmi://HOST:port/name}
	 * <p>Default is localhost.
	 */
	public void setRegistryHost(String registryHost) {
		this.registryHost = registryHost;
	}

	/**
	 * Set the port of the registry for the exported RMI service,
	 * i.e. {@code rmi://host:PORT/name}
	 * <p>Default is {@code Registry.REGISTRY_PORT} (1099).
	 * @see java.rmi.registry.Registry#REGISTRY_PORT
	 */
	public void setRegistryPort(int registryPort) {
		this.registryPort = registryPort;
	}

	/**
	 * Set a custom RMI client socket factory to use for the RMI registry.
	 * <p>If the given object also implements {@code java.rmi.server.RMIServerSocketFactory},
	 * it will automatically be registered as server socket factory too.
	 * @see #setRegistryServerSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see LocateRegistry#getRegistry(String, int, RMIClientSocketFactory)
	 */
	public void setRegistryClientSocketFactory(RMIClientSocketFactory registryClientSocketFactory) {
		this.registryClientSocketFactory = registryClientSocketFactory;
	}

	/**
	 * Set a custom RMI server socket factory to use for the RMI registry.
	 * <p>Only needs to be specified when the client socket factory does not
	 * implement {@code java.rmi.server.RMIServerSocketFactory} already.
	 * @see #setRegistryClientSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see LocateRegistry#createRegistry(int, RMIClientSocketFactory, RMIServerSocketFactory)
	 */
	public void setRegistryServerSocketFactory(RMIServerSocketFactory registryServerSocketFactory) {
		this.registryServerSocketFactory = registryServerSocketFactory;
	}

	/**
	 * Set whether to always create the registry in-process,
	 * not attempting to locate an existing registry at the specified port.
	 * <p>Default is "false". Switch this flag to "true" in order to avoid
	 * the overhead of locating an existing registry when you always
	 * intend to create a new registry in any case.
	 */
	public void setAlwaysCreateRegistry(boolean alwaysCreateRegistry) {
		this.alwaysCreateRegistry = alwaysCreateRegistry;
	}

	/**
	 * Set whether to replace an existing binding in the RMI registry,
	 * that is, whether to simply override an existing binding with the
	 * specified service in case of a naming conflict in the registry.
	 * <p>Default is "true", assuming that an existing binding for this
	 * exporter's service name is an accidental leftover from a previous
	 * execution. Switch this to "false" to make the exporter fail in such
	 * a scenario, indicating that there was already an RMI object bound.
	 */
	public void setReplaceExistingBinding(boolean replaceExistingBinding) {
		this.replaceExistingBinding = replaceExistingBinding;
	}


	public void afterPropertiesSet() throws RemoteException {
		prepare();
	}

	/**
	 * Initialize this service exporter, registering the service as RMI object.
	 * <p>Creates an RMI registry on the specified port if none exists.
	 * @throws RemoteException if service registration failed
	 */
	public void prepare() throws RemoteException {
		checkService();

		if (this.serviceName == null) {
			throw new IllegalArgumentException("Property 'serviceName' is required");
		}

		// Check socket factories for exported object.
		if (this.clientSocketFactory instanceof RMIServerSocketFactory) {
			this.serverSocketFactory = (RMIServerSocketFactory) this.clientSocketFactory;
		}
		if ((this.clientSocketFactory != null && this.serverSocketFactory == null) ||
				(this.clientSocketFactory == null && this.serverSocketFactory != null)) {
			throw new IllegalArgumentException(
					"Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
		}

		// Check socket factories for RMI registry.
		if (this.registryClientSocketFactory instanceof RMIServerSocketFactory) {
			this.registryServerSocketFactory = (RMIServerSocketFactory) this.registryClientSocketFactory;
		}
		if (this.registryClientSocketFactory == null && this.registryServerSocketFactory != null) {
			throw new IllegalArgumentException(
					"RMIServerSocketFactory without RMIClientSocketFactory for registry not supported");
		}

		this.createdRegistry = false;

		// Determine RMI registry to use.
		if (this.registry == null) {
			this.registry = getRegistry(this.registryHost, this.registryPort,
				this.registryClientSocketFactory, this.registryServerSocketFactory);
			this.createdRegistry = true;
		}

		// Initialize and cache exported object.
		this.exportedObject = getObjectToExport();

		if (logger.isInfoEnabled()) {
			logger.info("Binding service '" + this.serviceName + "' to RMI registry: " + this.registry);
		}

		// Export RMI object.
		if (this.clientSocketFactory != null) {
			UnicastRemoteObject.exportObject(
					this.exportedObject, this.servicePort, this.clientSocketFactory, this.serverSocketFactory);
		}
		else {
			UnicastRemoteObject.exportObject(this.exportedObject, this.servicePort);
		}

		// Bind RMI object to registry.
		try {
			if (this.replaceExistingBinding) {
				this.registry.rebind(this.serviceName, this.exportedObject);
			}
			else {
				this.registry.bind(this.serviceName, this.exportedObject);
			}
		}
		catch (AlreadyBoundException ex) {
			// Already an RMI object bound for the specified service name...
			unexportObjectSilently();
			throw new IllegalStateException(
					"Already an RMI object bound for name '"  + this.serviceName + "': " + ex.toString());
		}
		catch (RemoteException ex) {
			// Registry binding failed: let's unexport the RMI object as well.
			unexportObjectSilently();
			throw ex;
		}
	}


	/**
	 * Locate or create the RMI registry for this exporter.
	 * @param registryHost the registry host to use (if this is specified,
	 * no implicit creation of a RMI registry will happen)
	 * @param registryPort the registry port to use
	 * @param clientSocketFactory the RMI client socket factory for the registry (if any)
	 * @param serverSocketFactory the RMI server socket factory for the registry (if any)
	 * @return the RMI registry
	 * @throws RemoteException if the registry couldn't be located or created
	 */
	protected Registry getRegistry(String registryHost, int registryPort,
			RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (registryHost != null) {
			// Host explicitly specified: only lookup possible.
			if (logger.isInfoEnabled()) {
				logger.info("Looking for RMI registry at port '" + registryPort + "' of host [" + registryHost + "]");
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
	 * Locate or create the RMI registry for this exporter.
	 * @param registryPort the registry port to use
	 * @param clientSocketFactory the RMI client socket factory for the registry (if any)
	 * @param serverSocketFactory the RMI server socket factory for the registry (if any)
	 * @return the RMI registry
	 * @throws RemoteException if the registry couldn't be located or created
	 */
	protected Registry getRegistry(
			int registryPort, RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (clientSocketFactory != null) {
			if (this.alwaysCreateRegistry) {
				logger.info("Creating new RMI registry");
				return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
			}
			if (logger.isInfoEnabled()) {
				logger.info("Looking for RMI registry at port '" + registryPort + "', using custom socket factory");
			}
			synchronized (LocateRegistry.class) {
				try {
					// Retrieve existing registry.
					Registry reg = LocateRegistry.getRegistry(null, registryPort, clientSocketFactory);
					testRegistry(reg);
					return reg;
				}
				catch (RemoteException ex) {
					logger.debug("RMI registry access threw exception", ex);
					logger.info("Could not detect RMI registry - creating new one");
					// Assume no registry found -> create new one.
					return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
				}
			}
		}

		else {
			return getRegistry(registryPort);
		}
	}

	/**
	 * Locate or create the RMI registry for this exporter.
	 * @param registryPort the registry port to use
	 * @return the RMI registry
	 * @throws RemoteException if the registry couldn't be located or created
	 */
	protected Registry getRegistry(int registryPort) throws RemoteException {
		if (this.alwaysCreateRegistry) {
			logger.info("Creating new RMI registry");
			return LocateRegistry.createRegistry(registryPort);
		}
		if (logger.isInfoEnabled()) {
			logger.info("Looking for RMI registry at port '" + registryPort + "'");
		}
		synchronized (LocateRegistry.class) {
			try {
				// Retrieve existing registry.
				Registry reg = LocateRegistry.getRegistry(registryPort);
				testRegistry(reg);
				return reg;
			}
			catch (RemoteException ex) {
				logger.debug("RMI registry access threw exception", ex);
				logger.info("Could not detect RMI registry - creating new one");
				// Assume no registry found -> create new one.
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


	/**
	 * Unbind the RMI service from the registry on bean factory shutdown.
	 */
	public void destroy() throws RemoteException {
		if (logger.isInfoEnabled()) {
			logger.info("Unbinding RMI service '" + this.serviceName +
					"' from registry" + (this.createdRegistry ? (" at port '" + this.registryPort + "'") : ""));
		}
		try {
			this.registry.unbind(this.serviceName);
		}
		catch (NotBoundException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("RMI service '" + this.serviceName + "' is not bound to registry"
						+ (this.createdRegistry ? (" at port '" + this.registryPort + "' anymore") : ""), ex);
			}
		}
		finally {
			unexportObjectSilently();
		}
	}

	/**
	 * Unexport the registered RMI object, logging any exception that arises.
	 */
	private void unexportObjectSilently() {
		try {
			UnicastRemoteObject.unexportObject(this.exportedObject, true);
		}
		catch (NoSuchObjectException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("RMI object for service '" + this.serviceName + "' isn't exported anymore", ex);
			}
		}
	}
}