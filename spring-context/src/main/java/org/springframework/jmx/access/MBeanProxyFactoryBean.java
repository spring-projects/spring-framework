/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.jmx.access;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.util.ClassUtils;

/**
 * Creates a proxy to a managed resource running either locally or remotely.
 * The "proxyInterface" property defines the interface that the generated
 * proxy is supposed to implement. This interface should define methods and
 * properties that correspond to operations and attributes in the management
 * interface of the resource you wish to proxy.
 *
 * <p>There is no need for the managed resource to implement the proxy interface,
 * although you may find it convenient to do. It is not required that every
 * operation and attribute in the management interface is matched by a
 * corresponding property or method in the proxy interface.
 *
 * <p>Attempting to invoke or access any method or property on the proxy
 * interface that does not correspond to the management interface will lead
 * to an {@code InvalidInvocationException}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see MBeanClientInterceptor
 * @see InvalidInvocationException
 */
public class MBeanProxyFactoryBean extends MBeanClientInterceptor
		implements FactoryBean<Object>, BeanClassLoaderAware, InitializingBean {

	private @Nullable Class<?> proxyInterface;

	private @Nullable ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private @Nullable Object mbeanProxy;


	/**
	 * Set the interface that the generated proxy will implement.
	 * <p>This will usually be a management interface that matches the target MBean,
	 * exposing bean property setters and getters for MBean attributes and
	 * conventional Java methods for MBean operations.
	 * @see #setObjectName
	 */
	public void setProxyInterface(Class<?> proxyInterface) {
		this.proxyInterface = proxyInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Checks that the {@code proxyInterface} has been specified and then
	 * generates the proxy for the target MBean.
	 */
	@Override
	public void afterPropertiesSet() throws MBeanServerNotFoundException, MBeanInfoRetrievalException {
		super.afterPropertiesSet();

		Class<?> interfaceToUse;
		if (this.proxyInterface == null) {
			interfaceToUse = getManagementInterface();
			if (interfaceToUse == null) {
				throw new IllegalArgumentException("Property 'proxyInterface' or 'managementInterface' is required");
			}
			this.proxyInterface = interfaceToUse;
		}
		else {
			interfaceToUse = this.proxyInterface;
			if (getManagementInterface() == null) {
				setManagementInterface(interfaceToUse);
			}
		}
		this.mbeanProxy = new ProxyFactory(interfaceToUse, this).getProxy(this.beanClassLoader);
	}


	@Override
	public @Nullable Object getObject() {
		return this.mbeanProxy;
	}

	@Override
	public @Nullable Class<?> getObjectType() {
		return this.proxyInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
