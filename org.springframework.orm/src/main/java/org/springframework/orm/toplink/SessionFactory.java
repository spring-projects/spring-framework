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

package org.springframework.orm.toplink;

import oracle.toplink.exceptions.TopLinkException;
import oracle.toplink.sessions.Session;

/**
 * The SessionFactory interface serves as factory for TopLink Sessions,
 * allowing for dependency injection on thread-safe TopLink-based DAOs.
 * Used by TopLinkAccessor/Template and TopLinkTransactionManager.
 *
 * <p>In contrast to JDO or Hibernate (which define native PersistenceManagerFactory
 * and SessionFactory interfaces, respectively), TopLink itself does not provide
 * such a factory interface: hence, it is necessary to define it within Spring.
 * Note that this interface does not depend on any other Spring interfaces or
 * classes, to allow for keeping TopLink-based DAOs as independent as possible.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see TopLinkAccessor#setSessionFactory
 * @see TopLinkTransactionManager#setSessionFactory
 */
public interface SessionFactory {

	/**
	 * Create a plain TopLink Session for the current application context.
	 * Will usually be a new ClientSession for the current thread.
	 * <p>The returned Session will participate in JTA transactions (provided that
	 * TopLink is configured with a corresponding external transaction controller),
	 * but not in Spring-managed transactions (by TopLinkTransactionManager).
	 * <p>This is the factory method to be called by TopLink data access code,
	 * usually through the <code>SessionFactoryUtils.getSession</code> method
	 * that checks for a transactional (thread-bound) Session first.
	 * @return the new TopLink Session
	 * @throws TopLinkException in case of errors
	 * @see SessionFactoryUtils#getSession(SessionFactory, boolean)
	 */
	Session createSession() throws TopLinkException;

	/**
	 * Create a new managed TopLink client Session for the current context.
	 * Will usually be a new special ClientSession for the current thread.
	 * <p>The returned Session will be prepared to be managed within a Spring
	 * transaction (by TopLinkTransactionManager). It will carry an active
	 * UnitOfWork that expects to be committed at transaction completion,
	 * just like a plain TopLink Session does within a JTA transaction.
	 * <p>This method is only supposed to be called by Spring's
	 * TopLinkTransactionManager or similar TopLink-based transaction managers.
	 * If a SessionFactory does not support managed Sessions, it should throw
	 * an UnsupportedOperationException.
	 * @return the new TopLink Session
	 * @throws TopLinkException in case of errors
	 * @see oracle.toplink.sessions.Session#getActiveUnitOfWork()
	 */
	Session createManagedClientSession() throws TopLinkException;

	/**
	 * Create a new transaction-aware TopLink Session that exposes the currently
	 * active Session and UnitOfWork via <code>Session.getActiveSession()</code>
	 * and <code>Session.getActiveUnitOfWork()</code>, respectively.
	 * <p>Such a Session reference can be used analogously to a managed TopLink
	 * Session in a JTA environment, with Spring-managed transactions backing it.
	 * <p>It is usually preferable to let DAOs work with a full SessionFactory,
	 * accessing TopLink Sessions via <code>SessionFactoryUtils.getSession</code>.
	 * However, a transaction-aware TopLink Session reference does not impose any
	 * Spring dependency, so might be preferable if you'd like to keep your data
	 * access code tied to TopLink API only.
	 * @return the new TopLink Session
	 * @throws TopLinkException in case of errors
	 * @see oracle.toplink.sessions.Session#getActiveSession()
	 * @see oracle.toplink.sessions.Session#getActiveUnitOfWork()
	 */ 
	Session createTransactionAwareSession() throws TopLinkException;

	/**
	 * Close this SessionFactory, shutting down all internal resources.
	 */
	void close();

}
