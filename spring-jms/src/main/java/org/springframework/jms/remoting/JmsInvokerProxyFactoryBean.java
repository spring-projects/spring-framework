/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jms.remoting;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * FactoryBean for JMS invoker proxies. Exposes the proxied service for use
 * as a bean reference, using the specified service interface.
 *
 * <p>Serializes remote invocation objects and deserializes remote invocation
 * result objects. Uses Java serialization just like RMI, but with the JMS
 * provider as communication infrastructure.
 *
 * <p>To be configured with a {@link javax.jms.QueueConnectionFactory} and a
 * target queue (either as {@link javax.jms.Queue} reference or as queue name).
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setConnectionFactory
 * @see #setQueueName
 * @see #setServiceInterface
 * @see org.springframework.jms.remoting.JmsInvokerClientInterceptor
 * @see org.springframework.jms.remoting.JmsInvokerServiceExporter
 */
public class JmsInvokerProxyFactoryBean extends JmsInvokerClientInterceptor
		implements FactoryBean<Object>, BeanClassLoaderAware {

	@Nullable
	private Class<?> serviceInterface;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private Object serviceProxy;


	/**
	 * Set the interface that the proxy must implement.
	 * @param serviceInterface the interface that the proxy must implement
	 * @throws IllegalArgumentException if the supplied {@code serviceInterface}
	 * is not an interface type
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		Assert.notNull(serviceInterface, "'serviceInterface' must not be null");
		Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		this.serviceInterface = serviceInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Assert.notNull(this.serviceInterface, "Property 'serviceInterface' is required");
		this.serviceProxy = new ProxyFactory(this.serviceInterface, this).getProxy(this.beanClassLoader);
	}


	@Override
	@Nullable
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return this.serviceInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
