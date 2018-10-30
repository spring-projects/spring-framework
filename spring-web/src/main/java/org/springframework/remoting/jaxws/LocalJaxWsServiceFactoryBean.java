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

package org.springframework.remoting.jaxws;

import javax.xml.ws.Service;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.beans.factory.FactoryBean} for locally
 * defined JAX-WS Service references.
 * Uses {@link LocalJaxWsServiceFactory}'s facilities underneath.
 *
 * <p>Alternatively, JAX-WS Service references can be looked up
 * in the JNDI environment of the Java EE container.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see javax.xml.ws.Service
 * @see org.springframework.jndi.JndiObjectFactoryBean
 * @see JaxWsPortProxyFactoryBean
 */
public class LocalJaxWsServiceFactoryBean extends LocalJaxWsServiceFactory
		implements FactoryBean<Service>, InitializingBean {

	@Nullable
	private Service service;


	@Override
	public void afterPropertiesSet() {
		this.service = createJaxWsService();
	}

	@Override
	@Nullable
	public Service getObject() {
		return this.service;
	}

	@Override
	public Class<? extends Service> getObjectType() {
		return (this.service != null ? this.service.getClass() : Service.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
