/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.orm.toplink.support;

import oracle.toplink.sessions.Session;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.orm.toplink.SessionFactory;

/**
 * This adapter FactoryBean takes a TopLink SessionFactory and exposes a
 * corresponding transaction-aware TopLink Session as bean reference.
 *
 * <p>This adapter bean will usually be defined in front of a Spring
 * LocalSessionFactoryBean, to allow for passing Session references to DAOs
 * that expect to work on a raw TopLink Session. Your DAOs can then,
 * for example, access the currently active Session and UnitOfWork via
 * <code>Session.getActiveSession()</code> and
 * <code>Session.getActiveUnitOfWork()</code>, respectively.
 *
 * <p>The main advantage of this proxy is that it allows DAOs to work with a
 * plain TopLink Session reference, while still participating in Spring's
 * (or a J2EE server's) resource and transaction management. DAOs will only
 * rely on the TopLink API in such a scenario, without any Spring dependencies.
 *
 * <p>It is usually preferable to write your TopLink-based DAOs with Spring's
 * TopLinkTemplate, offering benefits such as consistent data access exceptions
 * instead of TopLinkExceptions at the DAO layer. However, Spring's resource
 * and transaction management (and Dependency	Injection) will work for DAOs
 * written against the plain TopLink API too.
 *
 * <p>Of course, you can still access the target TopLink SessionFactory
 * even when your DAOs go through this adapter, by defining a bean reference
 * that points directly at your target SessionFactory bean.
 *
 * <p>Note that the actual creation of a transaction-aware TopLink Session
 * is available on the TopLink SessionFactory itself. This adapter FactoryBean
 * is just a convenient way to expose such a Session in a declarative fashion.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.springframework.orm.toplink.LocalSessionFactoryBean
 * @see org.springframework.orm.toplink.SessionFactory#createTransactionAwareSession()
 * @see oracle.toplink.sessions.Session#getActiveSession()
 * @see oracle.toplink.sessions.Session#getActiveUnitOfWork()
 */
public class TransactionAwareSessionAdapter implements FactoryBean {

	private Session session;


	/**
	 * Set the SessionFactory that this adapter is supposed to expose a
	 * transaction-aware TopLink Session for. This should be the raw
	 * SessionFactory, as accessed by TopLinkTransactionManager.
	 * @see org.springframework.orm.toplink.TopLinkTransactionManager
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.session = sessionFactory.createTransactionAwareSession();
	}


	public Object getObject() {
		return this.session;
	}

	public Class getObjectType() {
		return Session.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
