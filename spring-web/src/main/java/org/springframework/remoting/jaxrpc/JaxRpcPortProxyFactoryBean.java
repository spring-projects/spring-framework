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

package org.springframework.remoting.jaxrpc;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.ClassUtils;

/**
 * {@link FactoryBean} for a specific port of a JAX-RPC service.
 * Exposes a proxy for the port, to be used for bean references.
 * Inherits configuration properties from {@link JaxRpcPortClientInterceptor}.
 *
 * <p>This factory is typically used with an RMI service interface. Alternatively,
 * this factory can also proxy a JAX-RPC service with a matching non-RMI business
 * interface, i.e. an interface that mirrors the RMI service methods but does not
 * declare RemoteExceptions. In the latter case, RemoteExceptions thrown by the
 * JAX-RPC stub will automatically get converted to Spring's unchecked
 * RemoteAccessException.
 *
 * <p>If exposing the JAX-RPC port interface (i.e. an RMI interface) directly,
 * setting "serviceInterface" is sufficient. If exposing a non-RMI business
 * interface, the business interface needs to be set as "serviceInterface",
 * and the JAX-RPC port interface as "portInterface".
 *
 * @author Juergen Hoeller
 * @since 15.12.2003
 * @see #setServiceInterface
 * @see #setPortInterface
 * @see LocalJaxRpcServiceFactoryBean
 * @deprecated in favor of JAX-WS support in {@code org.springframework.remoting.jaxws}
 */
@Deprecated
public class JaxRpcPortProxyFactoryBean extends JaxRpcPortClientInterceptor
		implements FactoryBean<Object>, BeanClassLoaderAware {

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Object serviceProxy;


	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		if (getServiceInterface() == null) {
			// Use JAX-RPC port interface (a traditional RMI interface)
			// as service interface if none explicitly specified.
			if (getPortInterface() != null) {
				setServiceInterface(getPortInterface());
			}
			else {
				throw new IllegalArgumentException("Property 'serviceInterface' is required");
			}
		}
		super.afterPropertiesSet();
		this.serviceProxy = new ProxyFactory(getServiceInterface(), this).getProxy(this.beanClassLoader);
	}


	public Object getObject() {
		return this.serviceProxy;
	}

	public Class<?> getObjectType() {
		return getServiceInterface();
	}

	public boolean isSingleton() {
		return true;
	}

}
