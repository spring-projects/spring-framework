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

import java.net.URL;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.ServiceFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;

/**
 * Factory for locally defined JAX-RPC {@link javax.xml.rpc.Service} references.
 * Uses a JAX-RPC {@link javax.xml.rpc.ServiceFactory} underneath.
 *
 * <p>Serves as base class for {@link LocalJaxRpcServiceFactoryBean} as well as
 * {@link JaxRpcPortClientInterceptor} and {@link JaxRpcPortProxyFactoryBean}.
 *
 * @author Juergen Hoeller
 * @since 15.12.2003
 * @see javax.xml.rpc.ServiceFactory
 * @see javax.xml.rpc.Service
 * @see LocalJaxRpcServiceFactoryBean
 * @see JaxRpcPortClientInterceptor
 * @see JaxRpcPortProxyFactoryBean
 * @deprecated in favor of JAX-WS support in <code>org.springframework.remoting.jaxws</code>
 */
@Deprecated
public class LocalJaxRpcServiceFactory {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private ServiceFactory serviceFactory;

	private Class serviceFactoryClass;

	private URL wsdlDocumentUrl;

	private String namespaceUri;

	private String serviceName;

	private Class jaxRpcServiceInterface;

	private Properties jaxRpcServiceProperties;

	private JaxRpcServicePostProcessor[] servicePostProcessors;


	/**
	 * Set the ServiceFactory instance to use.
	 * <p>This is an alternative to the common "serviceFactoryClass" property,
	 * allowing for a pre-initialized ServiceFactory instance to be specified.
	 * @see #setServiceFactoryClass
	 */
	public void setServiceFactory(ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	/**
	 * Return the specified ServiceFactory instance, if any.
	 */
	public ServiceFactory getServiceFactory() {
		return this.serviceFactory;
	}

	/**
	 * Set the ServiceFactory class to use, for example
	 * "org.apache.axis.client.ServiceFactory".
	 * <p>Does not need to be set if the JAX-RPC implementation has registered
	 * itself with the JAX-RPC system property "SERVICEFACTORY_PROPERTY".
	 * @see javax.xml.rpc.ServiceFactory
	 */
	public void setServiceFactoryClass(Class serviceFactoryClass) {
		if (serviceFactoryClass != null && !ServiceFactory.class.isAssignableFrom(serviceFactoryClass)) {
			throw new IllegalArgumentException("'serviceFactoryClass' must implement [javax.xml.rpc.ServiceFactory]");
		}
		this.serviceFactoryClass = serviceFactoryClass;
	}

	/**
	 * Return the ServiceFactory class to use, or <code>null</code> if default.
	 */
	public Class getServiceFactoryClass() {
		return this.serviceFactoryClass;
	}

	/**
	 * Set the URL of the WSDL document that describes the service.
	 */
	public void setWsdlDocumentUrl(URL wsdlDocumentUrl) {
		this.wsdlDocumentUrl = wsdlDocumentUrl;
	}

	/**
	 * Return the URL of the WSDL document that describes the service.
	 */
	public URL getWsdlDocumentUrl() {
		return this.wsdlDocumentUrl;
	}

	/**
	 * Set the namespace URI of the service.
	 * Corresponds to the WSDL "targetNamespace".
	 */
	public void setNamespaceUri(String namespaceUri) {
		this.namespaceUri = (namespaceUri != null ? namespaceUri.trim() : null);
	}

	/**
	 * Return the namespace URI of the service.
	 */
	public String getNamespaceUri() {
		return this.namespaceUri;
	}

	/**
	 * Set the name of the service to look up.
	 * Corresponds to the "wsdl:service" name.
	 * @see javax.xml.rpc.ServiceFactory#createService(javax.xml.namespace.QName)
	 * @see javax.xml.rpc.ServiceFactory#createService(java.net.URL, javax.xml.namespace.QName)
	 * @see javax.xml.rpc.ServiceFactory#loadService(java.net.URL, javax.xml.namespace.QName, java.util.Properties)
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Return the name of the service.
	 */
	public String getServiceName() {
		return this.serviceName;
	}

	/**
	 * Set the JAX-RPC service interface to use for looking up the service.
	 * If specified, this will override a "serviceName" setting.
	 * <p>The specified interface will usually be a generated JAX-RPC service
	 * interface that directly corresponds to the WSDL service declaration.
	 * Note that this is not a port interface or the application-level service
	 * interface to be exposed by a port proxy!
	 * <p>Only supported by JAX-RPC 1.1 providers.
	 * @see #setServiceName
	 * @see javax.xml.rpc.ServiceFactory#loadService(Class)
	 * @see javax.xml.rpc.ServiceFactory#loadService(java.net.URL, Class, java.util.Properties)
	 */
	public void setJaxRpcServiceInterface(Class jaxRpcServiceInterface) {
		this.jaxRpcServiceInterface = jaxRpcServiceInterface;
	}

	/**
	 * Return the JAX-RPC service interface to use for looking up the service.
	 */
	public Class getJaxRpcServiceInterface() {
		return this.jaxRpcServiceInterface;
	}

	/**
	 * Set JAX-RPC service properties to be passed to the ServiceFactory, if any.
	 * <p>Only supported by JAX-RPC 1.1 providers.
	 * @see javax.xml.rpc.ServiceFactory#loadService(java.net.URL, javax.xml.namespace.QName, java.util.Properties)
	 * @see javax.xml.rpc.ServiceFactory#loadService(java.net.URL, Class, java.util.Properties)
	 */
	public void setJaxRpcServiceProperties(Properties jaxRpcServiceProperties) {
		this.jaxRpcServiceProperties = jaxRpcServiceProperties;
	}

	/**
	 * Return JAX-RPC service properties to be passed to the ServiceFactory, if any.
	 */
	public Properties getJaxRpcServiceProperties() {
		return this.jaxRpcServiceProperties;
	}

	/**
	 * Set the JaxRpcServicePostProcessors to be applied to JAX-RPC Service
	 * instances created by this factory.
	 * <p>Such post-processors can, for example, register custom type mappings.
	 * They are reusable across all pre-built subclasses of this factory:
	 * LocalJaxRpcServiceFactoryBean, JaxRpcPortClientInterceptor,
	 * JaxRpcPortProxyFactoryBean.
	 * @see LocalJaxRpcServiceFactoryBean
	 * @see JaxRpcPortClientInterceptor
	 * @see JaxRpcPortProxyFactoryBean
	 */
	public void setServicePostProcessors(JaxRpcServicePostProcessor[] servicePostProcessors) {
		this.servicePostProcessors = servicePostProcessors;
	}

	/**
	 * Return the JaxRpcServicePostProcessors to be applied to JAX-RPC Service
	 * instances created by this factory.
	 */
	public JaxRpcServicePostProcessor[] getServicePostProcessors() {
		return this.servicePostProcessors;
	}


	/**
	 * Create a JAX-RPC Service according to the parameters of this factory.
	 * @see #setServiceName
	 * @see #setWsdlDocumentUrl
	 * @see #postProcessJaxRpcService
	 */
	public Service createJaxRpcService() throws ServiceException {
		ServiceFactory serviceFactory = getServiceFactory();
		if (serviceFactory == null) {
			serviceFactory = createServiceFactory();
		}

		// Create service based on this factory's settings.
		Service service = createService(serviceFactory);

		// Allow for custom post-processing in subclasses.
		postProcessJaxRpcService(service);

		return service;
	}

	/**
	 * Return a QName for the given name, relative to the namespace URI
	 * of this factory, if given.
	 * @see #setNamespaceUri
	 */
	protected QName getQName(String name) {
		return (getNamespaceUri() != null ? new QName(getNamespaceUri(), name) : new QName(name));
	}

	/**
	 * Create a JAX-RPC ServiceFactory, either of the specified class
	 * or the default.
	 * @throws ServiceException if thrown by JAX-RPC methods
	 * @see #setServiceFactoryClass
	 * @see javax.xml.rpc.ServiceFactory#newInstance()
	 */
	protected ServiceFactory createServiceFactory() throws ServiceException {
		if (getServiceFactoryClass() != null) {
			return (ServiceFactory) BeanUtils.instantiateClass(getServiceFactoryClass());
		}
		else {
			return ServiceFactory.newInstance();
		}
	}

	/**
	 * Actually create the JAX-RPC Service instance,
	 * based on this factory's settings.
	 * @param serviceFactory the JAX-RPC ServiceFactory to use
	 * @return the newly created JAX-RPC Service
	 * @throws ServiceException if thrown by JAX-RPC methods
	 * @see javax.xml.rpc.ServiceFactory#createService
	 * @see javax.xml.rpc.ServiceFactory#loadService
	 */
	protected Service createService(ServiceFactory serviceFactory) throws ServiceException {
		if (getServiceName() == null && getJaxRpcServiceInterface() == null) {
			throw new IllegalArgumentException("Either 'serviceName' or 'jaxRpcServiceInterface' is required");
		}

		if (getJaxRpcServiceInterface() != null) {
			// Create service via generated JAX-RPC service interface.
			// Only supported on JAX-RPC 1.1
			if (getWsdlDocumentUrl() != null || getJaxRpcServiceProperties() != null) {
				return serviceFactory.loadService(
						getWsdlDocumentUrl(), getJaxRpcServiceInterface(), getJaxRpcServiceProperties());
			}
			return serviceFactory.loadService(getJaxRpcServiceInterface());
		}

		// Create service via specified JAX-RPC service name.
		QName serviceQName = getQName(getServiceName());
		if (getJaxRpcServiceProperties() != null) {
			// Only supported on JAX-RPC 1.1
			return serviceFactory.loadService(getWsdlDocumentUrl(), serviceQName, getJaxRpcServiceProperties());
		}
		if (getWsdlDocumentUrl() != null) {
			return serviceFactory.createService(getWsdlDocumentUrl(), serviceQName);
		}
		return serviceFactory.createService(serviceQName);
	}

	/**
	 * Post-process the given JAX-RPC Service. Called by {@link #createJaxRpcService}.
	 * Useful, for example, to register custom type mappings.
	 * <p>The default implementation delegates to all registered
	 * {@link JaxRpcServicePostProcessor JaxRpcServicePostProcessors}.
	 * It is usually preferable to implement custom type mappings etc there rather
	 * than in a subclass of this factory, to allow for reuse of the post-processors.
	 * @param service the current JAX-RPC Service
	 * (can be cast to an implementation-specific class if necessary)
	 * @see #setServicePostProcessors
	 * @see javax.xml.rpc.Service#getTypeMappingRegistry()
	 */
	protected void postProcessJaxRpcService(Service service) {
		JaxRpcServicePostProcessor[] postProcessors = getServicePostProcessors();
		if (postProcessors != null) {
			for (int i = 0; i < postProcessors.length; i++) {
				postProcessors[i].postProcessJaxRpcService(service);
			}
		}
	}

}
