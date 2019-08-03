/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.remoting.jaxws;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.SOAPFaultException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor} for accessing a
 * specific port of a JAX-WS service.
 *
 * <p>Uses either {@link LocalJaxWsServiceFactory}'s facilities underneath,
 * or takes an explicit reference to an existing JAX-WS Service instance
 * (e.g. obtained via {@link org.springframework.jndi.JndiObjectFactoryBean}).
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setPortName
 * @see #setServiceInterface
 * @see javax.xml.ws.Service#getPort
 * @see org.springframework.remoting.RemoteAccessException
 * @see org.springframework.jndi.JndiObjectFactoryBean
 */
public class JaxWsPortClientInterceptor extends LocalJaxWsServiceFactory
		implements MethodInterceptor, BeanClassLoaderAware, InitializingBean {

	@Nullable
	private Service jaxWsService;

	@Nullable
	private String portName;

	@Nullable
	private String username;

	@Nullable
	private String password;

	@Nullable
	private String endpointAddress;

	private boolean maintainSession;

	private boolean useSoapAction;

	@Nullable
	private String soapActionUri;

	@Nullable
	private Map<String, Object> customProperties;

	@Nullable
	private WebServiceFeature[] portFeatures;

	@Nullable
	private Class<?> serviceInterface;

	private boolean lookupServiceOnStartup = true;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private QName portQName;

	@Nullable
	private Object portStub;

	private final Object preparationMonitor = new Object();


	/**
	 * Set a reference to an existing JAX-WS Service instance,
	 * for example obtained via {@link org.springframework.jndi.JndiObjectFactoryBean}.
	 * If not set, {@link LocalJaxWsServiceFactory}'s properties have to be specified.
	 * @see #setWsdlDocumentUrl
	 * @see #setNamespaceUri
	 * @see #setServiceName
	 * @see org.springframework.jndi.JndiObjectFactoryBean
	 */
	public void setJaxWsService(@Nullable Service jaxWsService) {
		this.jaxWsService = jaxWsService;
	}

	/**
	 * Return a reference to an existing JAX-WS Service instance, if any.
	 */
	@Nullable
	public Service getJaxWsService() {
		return this.jaxWsService;
	}

	/**
	 * Set the name of the port.
	 * Corresponds to the "wsdl:port" name.
	 */
	public void setPortName(@Nullable String portName) {
		this.portName = portName;
	}

	/**
	 * Return the name of the port.
	 */
	@Nullable
	public String getPortName() {
		return this.portName;
	}

	/**
	 * Set the username to specify on the stub.
	 * @see javax.xml.ws.BindingProvider#USERNAME_PROPERTY
	 */
	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	/**
	 * Return the username to specify on the stub.
	 */
	@Nullable
	public String getUsername() {
		return this.username;
	}

	/**
	 * Set the password to specify on the stub.
	 * @see javax.xml.ws.BindingProvider#PASSWORD_PROPERTY
	 */
	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	/**
	 * Return the password to specify on the stub.
	 */
	@Nullable
	public String getPassword() {
		return this.password;
	}

	/**
	 * Set the endpoint address to specify on the stub.
	 * @see javax.xml.ws.BindingProvider#ENDPOINT_ADDRESS_PROPERTY
	 */
	public void setEndpointAddress(@Nullable String endpointAddress) {
		this.endpointAddress = endpointAddress;
	}

	/**
	 * Return the endpoint address to specify on the stub.
	 */
	@Nullable
	public String getEndpointAddress() {
		return this.endpointAddress;
	}

	/**
	 * Set the "session.maintain" flag to specify on the stub.
	 * @see javax.xml.ws.BindingProvider#SESSION_MAINTAIN_PROPERTY
	 */
	public void setMaintainSession(boolean maintainSession) {
		this.maintainSession = maintainSession;
	}

	/**
	 * Return the "session.maintain" flag to specify on the stub.
	 */
	public boolean isMaintainSession() {
		return this.maintainSession;
	}

	/**
	 * Set the "soapaction.use" flag to specify on the stub.
	 * @see javax.xml.ws.BindingProvider#SOAPACTION_USE_PROPERTY
	 */
	public void setUseSoapAction(boolean useSoapAction) {
		this.useSoapAction = useSoapAction;
	}

	/**
	 * Return the "soapaction.use" flag to specify on the stub.
	 */
	public boolean isUseSoapAction() {
		return this.useSoapAction;
	}

	/**
	 * Set the SOAP action URI to specify on the stub.
	 * @see javax.xml.ws.BindingProvider#SOAPACTION_URI_PROPERTY
	 */
	public void setSoapActionUri(@Nullable String soapActionUri) {
		this.soapActionUri = soapActionUri;
	}

	/**
	 * Return the SOAP action URI to specify on the stub.
	 */
	@Nullable
	public String getSoapActionUri() {
		return this.soapActionUri;
	}

	/**
	 * Set custom properties to be set on the stub.
	 * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
	 * or a "props" element in XML bean definitions.
	 * @see javax.xml.ws.BindingProvider#getRequestContext()
	 */
	public void setCustomProperties(Map<String, Object> customProperties) {
		this.customProperties = customProperties;
	}

	/**
	 * Allow Map access to the custom properties to be set on the stub,
	 * with the option to add or override specific entries.
	 * <p>Useful for specifying entries directly, for example via
	 * "customProperties[myKey]". This is particularly useful for
	 * adding or overriding entries in child bean definitions.
	 */
	public Map<String, Object> getCustomProperties() {
		if (this.customProperties == null) {
			this.customProperties = new HashMap<>();
		}
		return this.customProperties;
	}

	/**
	 * Add a custom property to this JAX-WS BindingProvider.
	 * @param name the name of the attribute to expose
	 * @param value the attribute value to expose
	 * @see javax.xml.ws.BindingProvider#getRequestContext()
	 */
	public void addCustomProperty(String name, Object value) {
		getCustomProperties().put(name, value);
	}

	/**
	 * Specify WebServiceFeature objects (e.g. as inner bean definitions)
	 * to apply to JAX-WS port stub creation.
	 * @since 4.0
	 * @see Service#getPort(Class, javax.xml.ws.WebServiceFeature...)
	 * @see #setServiceFeatures
	 */
	public void setPortFeatures(WebServiceFeature... features) {
		this.portFeatures = features;
	}

	/**
	 * Set the interface of the service that this factory should create a proxy for.
	 */
	public void setServiceInterface(@Nullable Class<?> serviceInterface) {
		if (serviceInterface != null) {
			Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	/**
	 * Return the interface of the service that this factory should create a proxy for.
	 */
	@Nullable
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}

	/**
	 * Set whether to look up the JAX-WS service on startup.
	 * <p>Default is "true". Turn this flag off to allow for late start
	 * of the target server. In this case, the JAX-WS service will be
	 * lazily fetched on first access.
	 */
	public void setLookupServiceOnStartup(boolean lookupServiceOnStartup) {
		this.lookupServiceOnStartup = lookupServiceOnStartup;
	}

	/**
	 * Set the bean ClassLoader to use for this interceptor: primarily for
	 * building a client proxy in the {@link JaxWsPortProxyFactoryBean} subclass.
	 */
	@Override
	public void setBeanClassLoader(@Nullable ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Return the bean ClassLoader to use for this interceptor.
	 */
	@Nullable
	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.lookupServiceOnStartup) {
			prepare();
		}
	}

	/**
	 * Initialize the JAX-WS port for this interceptor.
	 */
	public void prepare() {
		Class<?> ifc = getServiceInterface();
		Assert.notNull(ifc, "Property 'serviceInterface' is required");

		WebService ann = ifc.getAnnotation(WebService.class);
		if (ann != null) {
			applyDefaultsFromAnnotation(ann);
		}

		Service serviceToUse = getJaxWsService();
		if (serviceToUse == null) {
			serviceToUse = createJaxWsService();
		}

		this.portQName = getQName(getPortName() != null ? getPortName() : ifc.getName());
		Object stub = getPortStub(serviceToUse, (getPortName() != null ? this.portQName : null));
		preparePortStub(stub);
		this.portStub = stub;
	}

	/**
	 * Initialize this client interceptor's properties from the given WebService annotation,
	 * if necessary and possible (i.e. if "wsdlDocumentUrl", "namespaceUri", "serviceName"
	 * and "portName" haven't been set but corresponding values are declared at the
	 * annotation level of the specified service interface).
	 * @param ann the WebService annotation found on the specified service interface
	 */
	protected void applyDefaultsFromAnnotation(WebService ann) {
		if (getWsdlDocumentUrl() == null) {
			String wsdl = ann.wsdlLocation();
			if (StringUtils.hasText(wsdl)) {
				try {
					setWsdlDocumentUrl(new URL(wsdl));
				}
				catch (MalformedURLException ex) {
					throw new IllegalStateException(
							"Encountered invalid @Service wsdlLocation value [" + wsdl + "]", ex);
				}
			}
		}
		if (getNamespaceUri() == null) {
			String ns = ann.targetNamespace();
			if (StringUtils.hasText(ns)) {
				setNamespaceUri(ns);
			}
		}
		if (getServiceName() == null) {
			String sn = ann.serviceName();
			if (StringUtils.hasText(sn)) {
				setServiceName(sn);
			}
		}
		if (getPortName() == null) {
			String pn = ann.portName();
			if (StringUtils.hasText(pn)) {
				setPortName(pn);
			}
		}
	}

	/**
	 * Return whether this client interceptor has already been prepared,
	 * i.e. has already looked up the JAX-WS service and port.
	 */
	protected boolean isPrepared() {
		synchronized (this.preparationMonitor) {
			return (this.portStub != null);
		}
	}

	/**
	 * Return the prepared QName for the port.
	 * @see #setPortName
	 * @see #getQName
	 */
	@Nullable
	protected final QName getPortQName() {
		return this.portQName;
	}

	/**
	 * Obtain the port stub from the given JAX-WS Service.
	 * @param service the Service object to obtain the port from
	 * @param portQName the name of the desired port, if specified
	 * @return the corresponding port object as returned from
	 * {@code Service.getPort(...)}
	 */
	protected Object getPortStub(Service service, @Nullable QName portQName) {
		if (this.portFeatures != null) {
			return (portQName != null ? service.getPort(portQName, getServiceInterface(), this.portFeatures) :
					service.getPort(getServiceInterface(), this.portFeatures));
		}
		else {
			return (portQName != null ? service.getPort(portQName, getServiceInterface()) :
					service.getPort(getServiceInterface()));
		}
	}

	/**
	 * Prepare the given JAX-WS port stub, applying properties to it.
	 * Called by {@link #prepare}.
	 * @param stub the current JAX-WS port stub
	 * @see #setUsername
	 * @see #setPassword
	 * @see #setEndpointAddress
	 * @see #setMaintainSession
	 * @see #setCustomProperties
	 */
	protected void preparePortStub(Object stub) {
		Map<String, Object> stubProperties = new HashMap<>();
		String username = getUsername();
		if (username != null) {
			stubProperties.put(BindingProvider.USERNAME_PROPERTY, username);
		}
		String password = getPassword();
		if (password != null) {
			stubProperties.put(BindingProvider.PASSWORD_PROPERTY, password);
		}
		String endpointAddress = getEndpointAddress();
		if (endpointAddress != null) {
			stubProperties.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
		}
		if (isMaintainSession()) {
			stubProperties.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, Boolean.TRUE);
		}
		if (isUseSoapAction()) {
			stubProperties.put(BindingProvider.SOAPACTION_USE_PROPERTY, Boolean.TRUE);
		}
		String soapActionUri = getSoapActionUri();
		if (soapActionUri != null) {
			stubProperties.put(BindingProvider.SOAPACTION_URI_PROPERTY, soapActionUri);
		}
		stubProperties.putAll(getCustomProperties());
		if (!stubProperties.isEmpty()) {
			if (!(stub instanceof BindingProvider)) {
				throw new RemoteLookupFailureException("Port stub of class [" + stub.getClass().getName() +
						"] is not a customizable JAX-WS stub: it does not implement interface [javax.xml.ws.BindingProvider]");
			}
			((BindingProvider) stub).getRequestContext().putAll(stubProperties);
		}
	}

	/**
	 * Return the underlying JAX-WS port stub that this interceptor delegates to
	 * for each method invocation on the proxy.
	 */
	@Nullable
	protected Object getPortStub() {
		return this.portStub;
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (AopUtils.isToStringMethod(invocation.getMethod())) {
			return "JAX-WS proxy for port [" + getPortName() + "] of service [" + getServiceName() + "]";
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
	 * Perform a JAX-WS service invocation based on the given method invocation.
	 * @param invocation the AOP method invocation
	 * @return the invocation result, if any
	 * @throws Throwable in case of invocation failure
	 * @see #getPortStub()
	 * @see #doInvoke(org.aopalliance.intercept.MethodInvocation, Object)
	 */
	@Nullable
	protected Object doInvoke(MethodInvocation invocation) throws Throwable {
		try {
			return doInvoke(invocation, getPortStub());
		}
		catch (SOAPFaultException ex) {
			throw new JaxWsSoapFaultException(ex);
		}
		catch (ProtocolException ex) {
			throw new RemoteConnectFailureException(
					"Could not connect to remote service [" + getEndpointAddress() + "]", ex);
		}
		catch (WebServiceException ex) {
			throw new RemoteAccessException(
					"Could not access remote service at [" + getEndpointAddress() + "]", ex);
		}
	}

	/**
	 * Perform a JAX-WS service invocation on the given port stub.
	 * @param invocation the AOP method invocation
	 * @param portStub the RMI port stub to invoke
	 * @return the invocation result, if any
	 * @throws Throwable in case of invocation failure
	 * @see #getPortStub()
	 */
	@Nullable
	protected Object doInvoke(MethodInvocation invocation, @Nullable Object portStub) throws Throwable {
		Method method = invocation.getMethod();
		try {
			return method.invoke(portStub, invocation.getArguments());
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
		catch (Throwable ex) {
			throw new RemoteProxyFailureException("Invocation of stub method failed: " + method, ex);
		}
	}

}
