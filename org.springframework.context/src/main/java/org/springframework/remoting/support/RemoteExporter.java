/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.remoting.support;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for classes that export a remote service.
 * Provides "service" and "serviceInterface" bean properties.
 *
 * <p>Note that the service interface being used will show some signs of
 * remotability, like the granularity of method calls that it offers.
 * Furthermore, it has to have serializable arguments etc.
 *
 * @author Juergen Hoeller
 * @since 26.12.2003
 */
public abstract class RemoteExporter extends RemotingSupport {

	private Object service;

	private Class serviceInterface;

	private Boolean registerTraceInterceptor;

	private Object[] interceptors;


	/**
	 * Set the service to export.
	 * Typically populated via a bean reference.
	 */
	public void setService(Object service) {
		this.service = service;
	}

	/**
	 * Return the service to export.
	 */
	public Object getService() {
		return this.service;
	}

	/**
	 * Set the interface of the service to export.
	 * The interface must be suitable for the particular service and remoting strategy.
	 */
	public void setServiceInterface(Class serviceInterface) {
		if (serviceInterface != null && !serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	/**
	 * Return the interface of the service to export.
	 */
	public Class getServiceInterface() {
		return this.serviceInterface;
	}

	/**
	 * Set whether to register a RemoteInvocationTraceInterceptor for exported
	 * services. Only applied when a subclass uses <code>getProxyForService</code>
	 * for creating the proxy to expose.
	 * <p>Default is "true". RemoteInvocationTraceInterceptor's most important value
	 * is that it logs exception stacktraces on the server, before propagating an
	 * exception to the client. Note that RemoteInvocationTraceInterceptor will <i>not</i>
	 * be registered by default if the "interceptors" property has been specified.
	 * @see #setInterceptors
	 * @see #getProxyForService
	 * @see RemoteInvocationTraceInterceptor
	 */
	public void setRegisterTraceInterceptor(boolean registerTraceInterceptor) {
		this.registerTraceInterceptor = Boolean.valueOf(registerTraceInterceptor);
	}

	/**
	 * Set additional interceptors (or advisors) to be applied before the
	 * remote endpoint, e.g. a PerformanceMonitorInterceptor.
	 * <p>You may specify any AOP Alliance MethodInterceptors or other
	 * Spring AOP Advices, as well as Spring AOP Advisors.
	 * @see #getProxyForService
	 * @see org.springframework.aop.interceptor.PerformanceMonitorInterceptor
	 */
	public void setInterceptors(Object[] interceptors) {
		this.interceptors = interceptors;
	}


	/**
	 * Check whether the service reference has been set.
	 * @see #setService
	 */
	protected void checkService() throws IllegalArgumentException {
		if (getService() == null) {
			throw new IllegalArgumentException("Property 'service' is required");
		}
	}

	/**
	 * Check whether a service reference has been set,
	 * and whether it matches the specified service.
	 * @see #setServiceInterface
	 * @see #setService
	 */
	protected void checkServiceInterface() throws IllegalArgumentException {
		Class serviceInterface = getServiceInterface();
		Object service = getService();
		if (serviceInterface == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}
		if (service instanceof String) {
			throw new IllegalArgumentException("Service [" + service + "] is a String " +
					"rather than an actual service reference: Have you accidentally specified " +
					"the service bean name as value instead of as reference?");
		}
		if (!serviceInterface.isInstance(service)) {
			throw new IllegalArgumentException("Service interface [" + serviceInterface.getName() +
					"] needs to be implemented by service [" + service + "] of class [" +
					service.getClass().getName() + "]");
		}
	}

	/**
	 * Get a proxy for the given service object, implementing the specified
	 * service interface.
	 * <p>Used to export a proxy that does not expose any internals but just
	 * a specific interface intended for remote access. Furthermore, a
	 * {@link RemoteInvocationTraceInterceptor} will be registered (by default).
	 * @return the proxy
	 * @see #setServiceInterface
	 * @see #setRegisterTraceInterceptor
	 * @see RemoteInvocationTraceInterceptor
	 */
	protected Object getProxyForService() {
		checkService();
		checkServiceInterface();
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.addInterface(getServiceInterface());
		if (this.registerTraceInterceptor != null ?
				this.registerTraceInterceptor.booleanValue() : this.interceptors == null) {
			proxyFactory.addAdvice(new RemoteInvocationTraceInterceptor(getExporterName()));
		}
		if (this.interceptors != null) {
			AdvisorAdapterRegistry adapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();
			for (int i = 0; i < this.interceptors.length; i++) {
				proxyFactory.addAdvisor(adapterRegistry.wrap(this.interceptors[i]));
			}
		}
		proxyFactory.setTarget(getService());
		return proxyFactory.getProxy(getBeanClassLoader());
	}

	/**
	 * Return a short name for this exporter.
	 * Used for tracing of remote invocations.
	 * <p>Default is the unqualified class name (without package).
	 * Can be overridden in subclasses.
	 * @see #getProxyForService
	 * @see RemoteInvocationTraceInterceptor
	 * @see org.springframework.util.ClassUtils#getShortName
	 */
	protected String getExporterName() {
		return ClassUtils.getShortName(getClass());
	}

}
