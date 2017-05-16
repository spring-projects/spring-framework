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

package org.springframework.remoting.jaxws;

import javax.xml.ws.BindingProvider;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * {@link org.springframework.beans.factory.FactoryBean} for a specific port of a
 * JAX-WS service. Exposes a proxy for the port, to be used for bean references.
 * Inherits configuration properties from {@link JaxWsPortClientInterceptor}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setServiceInterface
 * @see LocalJaxWsServiceFactoryBean
 */
public class JaxWsPortProxyFactoryBean extends JaxWsPortClientInterceptor implements FactoryBean<Object> {

	private Object serviceProxy;


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// Build a proxy that also exposes the JAX-WS BindingProvider interface.
		ProxyFactory pf = new ProxyFactory();
		pf.addInterface(getServiceInterface());
		pf.addInterface(BindingProvider.class);
		pf.addAdvice(this);
		this.serviceProxy = pf.getProxy(getBeanClassLoader());
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
