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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either exprShess or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jdo.support;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

/**
 * Proxy that implements the {@link javax.jdo.PersistenceManager} interface,
 * delegating to a thread-bound PersistenceManager on each invocation -
 * as defined by the JDO 2.1 specification. This class makes such a standard
 * JDO PersistenceManager proxy available for bean references.
 *
 * <p>The main advantage of this proxy is that it allows DAOs to work with a
 * plain JDO PersistenceManager reference in JDO 2.1 style
 * (see {@link javax.jdo.PersistenceManagerFactory#getPersistenceManagerProxy()}),
 * exposing the exact behavior that the target JDO provider implements.
 *
 * <p>Note: This proxy requires JDO 2.1 or higher.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see SpringPersistenceManagerProxyBean
 * @see javax.jdo.PersistenceManagerFactory#getPersistenceManagerProxy()
 */
public class StandardPersistenceManagerProxyBean implements FactoryBean<PersistenceManager> {

	private PersistenceManager proxy;


	/**
	 * Set the target JDO PersistenceManagerFactory that this proxy should
	 * delegate to. This should be the raw PersistenceManagerFactory, as
	 * accessed by JdoTransactionManager.
	 * @see org.springframework.orm.jdo.JdoTransactionManager
	 */
	public void setPersistenceManagerFactory(PersistenceManagerFactory pmf) {
		Assert.notNull(pmf, "PersistenceManagerFactory must not be null");
		this.proxy = pmf.getPersistenceManagerProxy();
	}


	public PersistenceManager getObject() {
		return this.proxy;
	}

	public Class<? extends PersistenceManager> getObjectType() {
		return (this.proxy != null ? this.proxy.getClass() : PersistenceManager.class);
	}

	public boolean isSingleton() {
		return true;
	}

}
