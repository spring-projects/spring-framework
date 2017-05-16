/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;

/**
 * {@link FactoryBean} for RMI proxies, supporting both conventional RMI services
 * and RMI invokers. Exposes the proxied service for use as a bean reference,
 * using the specified service interface. Proxies will throw Spring's unchecked
 * RemoteAccessException on remote invocation failure instead of RMI's RemoteException.
 *
 * <p>The service URL must be a valid RMI URL like "rmi://localhost:1099/myservice".
 * RMI invokers work at the RmiInvocationHandler level, using the same invoker stub
 * for any service. Service interfaces do not have to extend {@code java.rmi.Remote}
 * or throw {@code java.rmi.RemoteException}. Of course, in and out parameters
 * have to be serializable.
 *
 * <p>With conventional RMI services, this proxy factory is typically used with the
 * RMI service interface. Alternatively, this factory can also proxy a remote RMI
 * service with a matching non-RMI business interface, i.e. an interface that mirrors
 * the RMI service methods but does not declare RemoteExceptions. In the latter case,
 * RemoteExceptions thrown by the RMI stub will automatically get converted to
 * Spring's unchecked RemoteAccessException.
 *
 * <p>The major advantage of RMI, compared to Hessian, is serialization.
 * Effectively, any serializable Java object can be transported without hassle.
 * Hessian has its own (de-)serialization mechanisms, but is HTTP-based and thus
 * much easier to setup than RMI. Alternatively, consider Spring's HTTP invoker
 * to combine Java serialization with HTTP-based transport.
 *
 * @author Juergen Hoeller
 * @since 13.05.2003
 * @see #setServiceInterface
 * @see #setServiceUrl
 * @see RmiClientInterceptor
 * @see RmiServiceExporter
 * @see java.rmi.Remote
 * @see java.rmi.RemoteException
 * @see org.springframework.remoting.RemoteAccessException
 * @see org.springframework.remoting.caucho.HessianProxyFactoryBean
 * @see org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean
 */
public class RmiProxyFactoryBean extends RmiClientInterceptor implements FactoryBean<Object>, BeanClassLoaderAware {

	private Object serviceProxy;


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (getServiceInterface() == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}
		this.serviceProxy = new ProxyFactory(getServiceInterface(), this).getProxy(getBeanClassLoader());
	}


	@Override
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return getServiceInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
