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

package org.springframework.remoting.jaxrpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.JAXRPCException;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;
import javax.xml.rpc.soap.SOAPFaultException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.remoting.rmi.RmiClientInterceptorUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor} for accessing a specific port
 * of a JAX-RPC service. Uses either {@link LocalJaxRpcServiceFactory}'s facilities
 * underneath or takes an explicit reference to an existing JAX-RPC Service instance
 * (e.g. obtained via a {@link org.springframework.jndi.JndiObjectFactoryBean}).
 *
 * <p>Allows to set JAX-RPC's standard stub properties directly, via the
 * "username", "password", "endpointAddress" and "maintainSession" properties.
 * For typical usage, it is not necessary to specify those.
 *
 * <p>In standard JAX-RPC style, this invoker is used with an RMI service interface.
 * Alternatively, this invoker can also proxy a JAX-RPC service with a matching
 * non-RMI business interface, that is, an interface that declares the service methods
 * without RemoteExceptions. In the latter case, RemoteExceptions thrown by JAX-RPC
 * will automatically get converted to Spring's unchecked RemoteAccessException.
 *
 * <p>Setting "serviceInterface" is usually sufficient: The invoker will automatically
 * use JAX-RPC "dynamic invocations" via the Call API in this case, no matter whether
 * the specified interface is an RMI or non-RMI interface. Alternatively, a corresponding
 * JAX-RPC port interface can be specified as "portInterface", which will turn this
 * invoker into "static invocation" mode (operating on a standard JAX-RPC port stub).
 *
 * @author Juergen Hoeller
 * @since 15.12.2003
 * @see #setPortName
 * @see #setServiceInterface
 * @see #setPortInterface
 * @see javax.xml.rpc.Service#createCall
 * @see javax.xml.rpc.Service#getPort
 * @see org.springframework.remoting.RemoteAccessException
 * @see org.springframework.jndi.JndiObjectFactoryBean
 * @deprecated in favor of JAX-WS support in <code>org.springframework.remoting.jaxws</code>
 */
@Deprecated
public class JaxRpcPortClientInterceptor extends LocalJaxRpcServiceFactory
		implements MethodInterceptor, InitializingBean {

	private Service jaxRpcService;

	private Service serviceToUse;

	private String portName;

	private String username;

	private String password;

	private String endpointAddress;

	private boolean maintainSession;

	/** Map of custom properties, keyed by property name (String) */
	private final Map<String, Object> customPropertyMap = new HashMap<String, Object>();

	private Class serviceInterface;

	private Class portInterface;

	private boolean lookupServiceOnStartup = true;

	private boolean refreshServiceAfterConnectFailure = false;

	private QName portQName;

	private Remote portStub;

	private final Object preparationMonitor = new Object();


	/**
	 * Set a reference to an existing JAX-RPC Service instance,
	 * for example obtained via {@link org.springframework.jndi.JndiObjectFactoryBean}.
	 * If not set, {@link LocalJaxRpcServiceFactory}'s properties have to be specified.
	 * @see #setServiceFactoryClass
	 * @see #setWsdlDocumentUrl
	 * @see #setNamespaceUri
	 * @see #setServiceName
	 * @see org.springframework.jndi.JndiObjectFactoryBean
	 */
	public void setJaxRpcService(Service jaxRpcService) {
		this.jaxRpcService = jaxRpcService;
	}

	/**
	 * Return a reference to an existing JAX-RPC Service instance, if any.
	 */
	public Service getJaxRpcService() {
		return this.jaxRpcService;
	}

	/**
	 * Set the name of the port.
	 * Corresponds to the "wsdl:port" name.
	 */
	public void setPortName(String portName) {
		this.portName = portName;
	}

	/**
	 * Return the name of the port.
	 */
	public String getPortName() {
		return this.portName;
	}

	/**
	 * Set the username to specify on the stub or call.
	 * @see javax.xml.rpc.Stub#USERNAME_PROPERTY
	 * @see javax.xml.rpc.Call#USERNAME_PROPERTY
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Return the username to specify on the stub or call.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Set the password to specify on the stub or call.
	 * @see javax.xml.rpc.Stub#PASSWORD_PROPERTY
	 * @see javax.xml.rpc.Call#PASSWORD_PROPERTY
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Return the password to specify on the stub or call.
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Set the endpoint address to specify on the stub or call.
	 * @see javax.xml.rpc.Stub#ENDPOINT_ADDRESS_PROPERTY
	 * @see javax.xml.rpc.Call#setTargetEndpointAddress
	 */
	public void setEndpointAddress(String endpointAddress) {
		this.endpointAddress = endpointAddress;
	}

	/**
	 * Return the endpoint address to specify on the stub or call.
	 */
	public String getEndpointAddress() {
		return this.endpointAddress;
	}

	/**
	 * Set the maintain session flag to specify on the stub or call.
	 * @see javax.xml.rpc.Stub#SESSION_MAINTAIN_PROPERTY
	 * @see javax.xml.rpc.Call#SESSION_MAINTAIN_PROPERTY
	 */
	public void setMaintainSession(boolean maintainSession) {
		this.maintainSession = maintainSession;
	}

	/**
	 * Return the maintain session flag to specify on the stub or call.
	 */
	public boolean isMaintainSession() {
		return this.maintainSession;
	}

	/**
	 * Set custom properties to be set on the stub or call.
	 * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
	 * or a "props" element in XML bean definitions.
	 * @see javax.xml.rpc.Stub#_setProperty
	 * @see javax.xml.rpc.Call#setProperty
	 */
	public void setCustomProperties(Properties customProperties) {
		CollectionUtils.mergePropertiesIntoMap(customProperties, this.customPropertyMap);
	}

	/**
	 * Set custom properties to be set on the stub or call.
	 * <p>Can be populated with a "map" or "props" element in XML bean definitions.
	 * @see javax.xml.rpc.Stub#_setProperty
	 * @see javax.xml.rpc.Call#setProperty
	 */
	public void setCustomPropertyMap(Map<String, Object> customProperties) {
		if (customProperties != null) {
			for (Map.Entry<String, Object> entry : customProperties.entrySet()) {
				addCustomProperty(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Allow Map access to the custom properties to be set on the stub
	 * or call, with the option to add or override specific entries.
	 * <p>Useful for specifying entries directly, for example via
	 * "customPropertyMap[myKey]". This is particularly useful for
	 * adding or overriding entries in child bean definitions.
	 */
	public Map<String, Object> getCustomPropertyMap() {
		return this.customPropertyMap;
	}

	/**
	 * Add a custom property to this JAX-RPC Stub/Call.
	 * @param name the name of the attribute to expose
	 * @param value the attribute value to expose
	 * @see javax.xml.rpc.Stub#_setProperty
	 * @see javax.xml.rpc.Call#setProperty
	 */
	public void addCustomProperty(String name, Object value) {
		this.customPropertyMap.put(name, value);
	}

	/**
	 * Set the interface of the service that this factory should create a proxy for.
	 * This will typically be a non-RMI business interface, although you can also
	 * use an RMI port interface as recommended by JAX-RPC here.
	 * <p>Calls on the specified service interface will either be translated to the
	 * underlying RMI port interface (in case of a "portInterface" being specified)
	 * or to dynamic calls (using the JAX-RPC Dynamic Invocation Interface).
	 * <p>The dynamic call mechanism has the advantage that you don't need to
	 * maintain an RMI port interface in addition to an existing non-RMI business
	 * interface. In terms of configuration, specifying the business interface
	 * as "serviceInterface" will be enough; this interceptor will automatically
	 * use dynamic calls in such a scenario.
	 * @see javax.xml.rpc.Service#createCall
	 * @see #setPortInterface
	 */
	public void setServiceInterface(Class serviceInterface) {
		if (serviceInterface != null && !serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	/**
	 * Return the interface of the service that this factory should create a proxy for.
	 */
	public Class getServiceInterface() {
		return this.serviceInterface;
	}

	/**
	 * Set the JAX-RPC port interface to use. Only needs to be set if a JAX-RPC
	 * port stub should be used instead of the dynamic call mechanism.
	 * See the javadoc of the "serviceInterface" property for more details.
	 * <p>The interface must be suitable for a JAX-RPC port, that is, it must be
	 * an RMI service interface (that extends <code>java.rmi.Remote</code>).
	 * <p><b>NOTE:</b> Check whether your JAX-RPC provider returns thread-safe
	 * port stubs. If not, use the dynamic call mechanism instead, which will
	 * always be thread-safe. In particular, do not use JAX-RPC port stubs
	 * with Apache Axis, whose port stubs are known to be non-thread-safe.
	 * @see javax.xml.rpc.Service#getPort
	 * @see java.rmi.Remote
	 * @see #setServiceInterface
	 */
	public void setPortInterface(Class portInterface) {
		if (portInterface != null &&
				(!portInterface.isInterface() || !Remote.class.isAssignableFrom(portInterface))) {
			throw new IllegalArgumentException(
					"'portInterface' must be an interface derived from [java.rmi.Remote]");
		}
		this.portInterface = portInterface;
	}

	/**
	 * Return the JAX-RPC port interface to use.
	 */
	public Class getPortInterface() {
		return this.portInterface;
	}

	/**
	 * Set whether to look up the JAX-RPC service on startup.
	 * <p>Default is "true". Turn this flag off to allow for late start
	 * of the target server. In this case, the JAX-RPC service will be
	 * lazily fetched on first access.
	 */
	public void setLookupServiceOnStartup(boolean lookupServiceOnStartup) {
		this.lookupServiceOnStartup = lookupServiceOnStartup;
	}

	/**
	 * Set whether to refresh the JAX-RPC service on connect failure,
	 * that is, whenever a JAX-RPC invocation throws a RemoteException.
	 * <p>Default is "false", keeping a reference to the JAX-RPC service
	 * in any case, retrying the next invocation on the same service
	 * even in case of failure. Turn this flag on to reinitialize the
	 * entire service in case of connect failures.
	 */
	public void setRefreshServiceAfterConnectFailure(boolean refreshServiceAfterConnectFailure) {
		this.refreshServiceAfterConnectFailure = refreshServiceAfterConnectFailure;
	}


	/**
	 * Prepares the JAX-RPC service and port if the "lookupServiceOnStartup"
	 * is turned on (which it is by default).
	 */
	public void afterPropertiesSet() {
		if (this.lookupServiceOnStartup) {
			prepare();
		}
	}

	/**
	 * Create and initialize the JAX-RPC service for the specified port.
	 * <p>Prepares a JAX-RPC stub if possible (if an RMI interface is available);
	 * falls back to JAX-RPC dynamic calls else. Using dynamic calls can be enforced
	 * through overriding {@link #alwaysUseJaxRpcCall} to return <code>true</code>.
	 * <p>{@link #postProcessJaxRpcService} and {@link #postProcessPortStub}
	 * hooks are available for customization in subclasses. When using dynamic calls,
	 * each can be post-processed via {@link #postProcessJaxRpcCall}.
	 * @throws RemoteLookupFailureException if service initialization or port stub creation failed
	 */
	public void prepare() throws RemoteLookupFailureException {
		if (getPortName() == null) {
			throw new IllegalArgumentException("Property 'portName' is required");
		}

		synchronized (this.preparationMonitor) {
			this.serviceToUse = null;

			// Cache the QName for the port.
			this.portQName = getQName(getPortName());

			try {
				Service service = getJaxRpcService();
				if (service == null) {
					service = createJaxRpcService();
				}
				else {
					postProcessJaxRpcService(service);
				}

				Class portInterface = getPortInterface();
				if (portInterface != null && !alwaysUseJaxRpcCall()) {
					// JAX-RPC-compliant port interface -> using JAX-RPC stub for port.

					if (logger.isDebugEnabled()) {
						logger.debug("Creating JAX-RPC proxy for JAX-RPC port [" + this.portQName +
								"], using port interface [" + portInterface.getName() + "]");
					}
					Remote remoteObj = service.getPort(this.portQName, portInterface);

					if (logger.isDebugEnabled()) {
						Class serviceInterface = getServiceInterface();
						if (serviceInterface != null) {
							boolean isImpl = serviceInterface.isInstance(remoteObj);
							logger.debug("Using service interface [" + serviceInterface.getName() + "] for JAX-RPC port [" +
									this.portQName + "] - " + (!isImpl ? "not" : "") + " directly implemented");
						}
					}

					if (!(remoteObj instanceof Stub)) {
						throw new RemoteLookupFailureException("Port stub of class [" + remoteObj.getClass().getName() +
								"] is not a valid JAX-RPC stub: it does not implement interface [javax.xml.rpc.Stub]");
					}
					Stub stub = (Stub) remoteObj;

					// Apply properties to JAX-RPC stub.
					preparePortStub(stub);

					// Allow for custom post-processing in subclasses.
					postProcessPortStub(stub);

					this.portStub = remoteObj;
				}

				else {
					// No JAX-RPC-compliant port interface -> using JAX-RPC dynamic calls.
					if (logger.isDebugEnabled()) {
						logger.debug("Using JAX-RPC dynamic calls for JAX-RPC port [" + this.portQName + "]");
					}
				}

				this.serviceToUse = service;
			}
			catch (ServiceException ex) {
				throw new RemoteLookupFailureException(
						"Failed to initialize service for JAX-RPC port [" + this.portQName + "]", ex);
			}
		}
	}

	/**
	 * Return whether to always use JAX-RPC dynamic calls.
	 * Called by <code>afterPropertiesSet</code>.
	 * <p>Default is "false"; if an RMI interface is specified as "portInterface"
	 * or "serviceInterface", it will be used to create a JAX-RPC port stub.
	 * <p>Can be overridden to enforce the use of the JAX-RPC Call API,
	 * for example if there is a need to customize at the Call level.
	 * This just necessary if you you want to use an RMI interface as
	 * "serviceInterface", though; in case of only a non-RMI interface being
	 * available, this interceptor will fall back to the Call API anyway.
	 * @see #postProcessJaxRpcCall
	 */
	protected boolean alwaysUseJaxRpcCall() {
		return false;
	}

	/**
	 * Reset the prepared service of this interceptor,
	 * allowing for reinitialization on next access.
	 */
	protected void reset() {
		synchronized (this.preparationMonitor) {
			this.serviceToUse = null;
		}
	}

	/**
	 * Return whether this client interceptor has already been prepared,
	 * i.e. has already looked up the JAX-RPC service and port.
	 */
	protected boolean isPrepared() {
		synchronized (this.preparationMonitor) {
			return (this.serviceToUse != null);
		}
	}

	/**
	 * Return the prepared QName for the port.
	 * @see #setPortName
	 * @see #getQName
	 */
	protected final QName getPortQName() {
		return this.portQName;
	}


	/**
	 * Prepare the given JAX-RPC port stub, applying properties to it.
	 * Called by {@link #prepare}.
	 * <p>Just applied when actually creating a JAX-RPC port stub, in case of a
	 * compliant port interface. Else, JAX-RPC dynamic calls will be used.
	 * @param stub the current JAX-RPC port stub
	 * @see #setUsername
	 * @see #setPassword
	 * @see #setEndpointAddress
	 * @see #setMaintainSession
	 * @see #setCustomProperties
	 * @see #setPortInterface
	 * @see #prepareJaxRpcCall
	 */
	protected void preparePortStub(Stub stub) {
		String username = getUsername();
		if (username != null) {
			stub._setProperty(Stub.USERNAME_PROPERTY, username);
		}
		String password = getPassword();
		if (password != null) {
			stub._setProperty(Stub.PASSWORD_PROPERTY, password);
		}
		String endpointAddress = getEndpointAddress();
		if (endpointAddress != null) {
			stub._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
		}
		if (isMaintainSession()) {
			stub._setProperty(Stub.SESSION_MAINTAIN_PROPERTY, Boolean.TRUE);
		}
		if (this.customPropertyMap != null) {
			for (Map.Entry<String, Object> entry : this.customPropertyMap.entrySet()) {
				stub._setProperty(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Post-process the given JAX-RPC port stub. Called by {@link #prepare}.
	 * <p>The default implementation is empty.
	 * <p>Just applied when actually creating a JAX-RPC port stub, in case of a
	 * compliant port interface. Else, JAX-RPC dynamic calls will be used.
	 * @param stub the current JAX-RPC port stub
	 * (can be cast to an implementation-specific class if necessary)
	 * @see #setPortInterface
	 * @see #postProcessJaxRpcCall
	 */
	protected void postProcessPortStub(Stub stub) {
	}

	/**
	 * Return the underlying JAX-RPC port stub that this interceptor delegates to
	 * for each method invocation on the proxy.
	 */
	protected Remote getPortStub() {
		return this.portStub;
	}


	/**
	 * Translates the method invocation into a JAX-RPC service invocation.
	 * <p>Prepares the service on the fly, if necessary, in case of lazy
	 * lookup or a connect failure having happened.
	 * @see #prepare()
	 * @see #doInvoke
	 */
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (AopUtils.isToStringMethod(invocation.getMethod())) {
			return "JAX-RPC proxy for port [" + getPortName() + "] of service [" + getServiceName() + "]";
		}
		// Lazily prepare service and stub if necessary.
		synchronized (this.preparationMonitor) {
			if (!isPrepared()) {
				prepare();
			}
		}
		return doInvoke(invocation);
	}

	/**
	 * Perform a JAX-RPC service invocation based on the given method invocation.
	 * <p>Uses traditional RMI stub invocation if a JAX-RPC port stub is available;
	 * falls back to JAX-RPC dynamic calls else.
	 * @param invocation the AOP method invocation
	 * @return the invocation result, if any
	 * @throws Throwable in case of invocation failure
	 * @see #getPortStub()
	 * @see #doInvoke(org.aopalliance.intercept.MethodInvocation, java.rmi.Remote)
	 * @see #performJaxRpcCall(org.aopalliance.intercept.MethodInvocation, javax.xml.rpc.Service)
	 */
	protected Object doInvoke(MethodInvocation invocation) throws Throwable {
		Remote stub = getPortStub();
		try {
			if (stub != null) {
				// JAX-RPC port stub available -> traditional RMI stub invocation.
				if (logger.isTraceEnabled()) {
					logger.trace("Invoking operation '" + invocation.getMethod().getName() + "' on JAX-RPC port stub");
				}
				return doInvoke(invocation, stub);
			}
			else {
				// No JAX-RPC stub -> using JAX-RPC dynamic calls.
				if (logger.isTraceEnabled()) {
					logger.trace("Invoking operation '" + invocation.getMethod().getName() + "' as JAX-RPC dynamic call");
				}
				return performJaxRpcCall(invocation, this.serviceToUse);
			}
		}
		catch (RemoteException ex) {
			throw handleRemoteException(invocation.getMethod(), ex);
		}
		catch (SOAPFaultException ex) {
			throw new JaxRpcSoapFaultException(ex);
		}
		catch (JAXRPCException ex) {
			throw new RemoteProxyFailureException("Invalid JAX-RPC call configuration", ex);
		}
	}

	/**
	 * Perform a JAX-RPC service invocation on the given port stub.
	 * @param invocation the AOP method invocation
	 * @param portStub the RMI port stub to invoke
	 * @return the invocation result, if any
	 * @throws Throwable in case of invocation failure
	 * @see #getPortStub()
	 * @see #doInvoke(org.aopalliance.intercept.MethodInvocation, java.rmi.Remote)
	 * @see #performJaxRpcCall
	 */
	protected Object doInvoke(MethodInvocation invocation, Remote portStub) throws Throwable {
		try {
			return RmiClientInterceptorUtils.invokeRemoteMethod(invocation, portStub);
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}

	/**
	 * Perform a JAX-RPC dynamic call for the given AOP method invocation.
	 * Delegates to {@link #prepareJaxRpcCall} and
	 * {@link #postProcessJaxRpcCall} for setting up the call object.
	 * <p>The default implementation uses method name as JAX-RPC operation name
	 * and method arguments as arguments for the JAX-RPC call. Can be
	 * overridden in subclasses for custom operation names and/or arguments.
	 * @param invocation the current AOP MethodInvocation that should
	 * be converted to a JAX-RPC call
	 * @param service the JAX-RPC Service to use for the call
	 * @return the return value of the invocation, if any
	 * @throws Throwable the exception thrown by the invocation, if any
	 * @see #prepareJaxRpcCall
	 * @see #postProcessJaxRpcCall
	 */
	protected Object performJaxRpcCall(MethodInvocation invocation, Service service) throws Throwable {
		Method method = invocation.getMethod();
		QName portQName = this.portQName;

		// Create JAX-RPC call object, using the method name as operation name.
		// Synchronized because of non-thread-safe Axis implementation!
		Call call = null;
		synchronized (service) {
			call = service.createCall(portQName, method.getName());
		}

		// Apply properties to JAX-RPC stub.
		prepareJaxRpcCall(call);

		// Allow for custom post-processing in subclasses.
		postProcessJaxRpcCall(call, invocation);

		// Perform actual invocation.
		return call.invoke(invocation.getArguments());
	}

	/**
	 * Prepare the given JAX-RPC call, applying properties to it. Called by {@link #invoke}.
	 * <p>Just applied when actually using JAX-RPC dynamic calls, i.e. if no compliant
	 * port interface was specified. Else, a JAX-RPC port stub will be used.
	 * @param call the current JAX-RPC call object
	 * @see #setUsername
	 * @see #setPassword
	 * @see #setEndpointAddress
	 * @see #setMaintainSession
	 * @see #setCustomProperties
	 * @see #setPortInterface
	 * @see #preparePortStub
	 */
	protected void prepareJaxRpcCall(Call call) {
		String username = getUsername();
		if (username != null) {
			call.setProperty(Call.USERNAME_PROPERTY, username);
		}
		String password = getPassword();
		if (password != null) {
			call.setProperty(Call.PASSWORD_PROPERTY, password);
		}
		String endpointAddress = getEndpointAddress();
		if (endpointAddress != null) {
			call.setTargetEndpointAddress(endpointAddress);
		}
		if (isMaintainSession()) {
			call.setProperty(Call.SESSION_MAINTAIN_PROPERTY, Boolean.TRUE);
		}
		if (this.customPropertyMap != null) {
			for (Map.Entry<String, Object> entry : this.customPropertyMap.entrySet()) {
				call.setProperty(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Post-process the given JAX-RPC call. Called by {@link #invoke}.
	 * <p>The default implementation is empty.
	 * <p>Just applied when actually using JAX-RPC dynamic calls, i.e. if no compliant
	 * port interface was specified. Else, a JAX-RPC port stub will be used.
	 * @param call the current JAX-RPC call object
	 * (can be cast to an implementation-specific class if necessary)
	 * @param invocation the current AOP MethodInvocation that the call was
	 * created for (can be used to check method name, method parameters
	 * and/or passed-in arguments)
	 * @see #setPortInterface
	 * @see #postProcessPortStub
	 */
	protected void postProcessJaxRpcCall(Call call, MethodInvocation invocation) {
	}

	/**
	 * Handle the given RemoteException that was thrown from a JAX-RPC port stub
	 * or JAX-RPC call invocation.
	 * @param method the service interface method that we invoked
	 * @param ex the original RemoteException
	 * @return the exception to rethrow (may be the original RemoteException
	 * or an extracted/wrapped exception, but never <code>null</code>)
	 */
	protected Throwable handleRemoteException(Method method, RemoteException ex) {
		boolean isConnectFailure = isConnectFailure(ex);
		if (isConnectFailure && this.refreshServiceAfterConnectFailure) {
			reset();
		}
		Throwable cause = ex.getCause();
		if (cause != null && ReflectionUtils.declaresException(method, cause.getClass())) {
			if (logger.isDebugEnabled()) {
				logger.debug("Rethrowing wrapped exception of type [" + cause.getClass().getName() + "] as-is");
			}
			// Declared on the service interface: probably a wrapped business exception.
			return ex.getCause();
		}
		else {
			// Throw either a RemoteAccessException or the original RemoteException,
			// depending on what the service interface declares.
			return RmiClientInterceptorUtils.convertRmiAccessException(
					method, ex, isConnectFailure, this.portQName.toString());
		}
	}

	/**
	 * Determine whether the given RMI exception indicates a connect failure.
	 * <p>The default implementation returns <code>true</code> unless the
	 * exception class name (or exception superclass name) contains the term
	 * "Fault" (e.g. "AxisFault"), assuming that the JAX-RPC provider only
	 * throws RemoteException in case of WSDL faults and connect failures.
	 * @param ex the RMI exception to check
	 * @return whether the exception should be treated as connect failure
	 * @see org.springframework.remoting.rmi.RmiClientInterceptorUtils#isConnectFailure
	 */
	protected boolean isConnectFailure(RemoteException ex) {
		return (!ex.getClass().getName().contains("Fault") &&
				!ex.getClass().getSuperclass().getName().contains("Fault"));
	}

}
