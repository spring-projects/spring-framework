/*
 * Copyright 2002-2013 the original author or authors.
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor} for accessing a
 * specific port of a JAX-WS service. Compatible with JAX-WS 2.1 and 2.2,
 * as included in JDK 6 update 4+ and Java 7/8.
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

	private Service jaxWsService;

	private String portName;

	private String username;

	private String password;

	private String endpointAddress;

	private boolean maintainSession;

	private boolean useSoapAction;

	private String soapActionUri;

	private Map<String, Object> customProperties;

	private WebServiceFeature[] portFeatures;

	private Object[] webServiceFeatures;

	private Class<?> serviceInterface;

	private boolean lookupServiceOnStartup = true;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private QName portQName;

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
	public void setJaxWsService(Service jaxWsService) {
		this.jaxWsService = jaxWsService;
	}

	/**
	 * Return a reference to an existing JAX-WS Service instance, if any.
	 */
	public Service getJaxWsService() {
		return this.jaxWsService;
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
	 * Set the username to specify on the stub.
	 * @see javax.xml.ws.BindingProvider#USERNAME_PROPERTY
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Return the username to specify on the stub.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Set the password to specify on the stub.
	 * @see javax.xml.ws.BindingProvider#PASSWORD_PROPERTY
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Return the password to specify on the stub.
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Set the endpoint address to specify on the stub.
	 * @see javax.xml.ws.BindingProvider#ENDPOINT_ADDRESS_PROPERTY
	 */
	public void setEndpointAddress(String endpointAddress) {
		this.endpointAddress = endpointAddress;
	}

	/**
	 * Return the endpoint address to specify on the stub.
	 */
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
	public void setSoapActionUri(String soapActionUri) {
		this.soapActionUri = soapActionUri;
	}

	/**
	 * Return the SOAP action URI to specify on the stub.
	 */
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
			this.customProperties = new HashMap<String, Object>();
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
	 * Specify WebServiceFeature specifications for the JAX-WS port stub:
	 * in the form of actual {@link javax.xml.ws.WebServiceFeature} objects,
	 * WebServiceFeature Class references, or WebServiceFeature class names.
	 * <p>As of Spring 4.0, this is effectively just an alternative way of
	 * specifying {@link #setPortFeatures "portFeatures"}. Do not specify
	 * both properties at the same time; prefer "portFeatures" moving forward.
	 * @deprecated as of Spring 4.0, in favor of the differentiated
	 * {@link #setServiceFeatures "serviceFeatures"} and
	 * {@link #setPortFeatures "portFeatures"} properties
	 */
	@Deprecated
	public void setWebServiceFeatures(Object[] webServiceFeatures) {
		this.webServiceFeatures = webServiceFeatures;
	}

	/**
	 * Set the interface of the service that this factory should create a proxy for.
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		if (serviceInterface != null && !serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	/**
	 * Return the interface of the service that this factory should create a proxy for.
	 */
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
	 * Set the bean ClassLoader to use for this interceptor:
	 * for resolving WebServiceFeature class names as specified through
	 * {@link #setWebServiceFeatures}, and also for building a client
	 * proxy in the {@link JaxWsPortProxyFactoryBean} subclass.
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Return the bean ClassLoader to use for this interceptor.
	 */
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
		if (ifc == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}
		WebService ann = ifc.getAnnotation(WebService.class);
		if (ann != null) {
			applyDefaultsFromAnnotation(ann);
		}
		Service serviceToUse = getJaxWsService();
		if (serviceToUse == null) {
			serviceToUse = createJaxWsService();
		}
		this.portQName = getQName(getPortName() != null ? getPortName() : getServiceInterface().getName());
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
	protected Object getPortStub(Service service, QName portQName) {
		if (this.portFeatures != null || this.webServiceFeatures != null) {
			WebServiceFeature[] portFeaturesToUse = this.portFeatures;
			if (portFeaturesToUse == null) {
				portFeaturesToUse = new WebServiceFeature[this.webServiceFeatures.length];
				for (int i = 0; i < this.webServiceFeatures.length; i++) {
					portFeaturesToUse[i] = convertWebServiceFeature(this.webServiceFeatures[i]);
				}
			}
			return (portQName != null ? service.getPort(portQName, getServiceInterface(), portFeaturesToUse) :
					service.getPort(getServiceInterface(), portFeaturesToUse));
		}
		else {
			return (portQName != null ? service.getPort(portQName, getServiceInterface()) :
					service.getPort(getServiceInterface()));
		}
	}

	/**
	 * Convert the given feature specification object to a WebServiceFeature instance
	 * @param feature the feature specification object, as passed into the
	 * {@link #setWebServiceFeatures "webServiceFeatures"} bean property
	 * @return the WebServiceFeature instance (never {@code null})
	 */
	private WebServiceFeature convertWebServiceFeature(Object feature) {
		Assert.notNull(feature, "WebServiceFeature specification object must not be null");
		if (feature instanceof WebServiceFeature) {
			return (WebServiceFeature) feature;
		}
		else if (feature instanceof Class) {
			return (WebServiceFeature) BeanUtils.instantiate((Class<?>) feature);
		}
		else if (feature instanceof String) {
			try {
				Class<?> featureClass = getBeanClassLoader().loadClass((String) feature);
				return (WebServiceFeature) BeanUtils.instantiate(featureClass);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalArgumentException("Could not load WebServiceFeature class [" + feature + "]");
			}
		}
		else {
			throw new IllegalArgumentException("Unknown WebServiceFeature specification type: " + feature.getClass());
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
		Map<String, Object> stubProperties = new HashMap<String, Object>();
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
	protected Object getPortStub() {
		return this.portStub;
	}


	@Override
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
	protected Object doInvoke(MethodInvocation invocation, Object portStub) throws Throwable {
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
