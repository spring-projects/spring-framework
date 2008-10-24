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

package org.springframework.orm.toplink;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import oracle.toplink.exceptions.TopLinkException;
import oracle.toplink.sessions.Session;
import oracle.toplink.sessions.UnitOfWork;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract SessionFactory implementation that creates proxies for
 * "managed" client Sessions and transaction-aware Session references.
 *
 * <p>Delegates to two template methods:
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see #getMasterSession()
 * @see #createClientSession()
 */
public abstract class AbstractSessionFactory implements SessionFactory {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * Create a plain client Session for this factory's master Session.
	 * @see #createClientSession()
	 */
	public Session createSession() throws TopLinkException {
		logger.debug("Creating TopLink client Session");
		return createClientSession();
	}

	/**
	 * Create a "managed" client Session reference for an underlying
	 * client Session created for this factory.
	 * @see #createClientSession()
	 */
	public Session createManagedClientSession() throws TopLinkException {
		logger.debug("Creating managed TopLink client Session");
		Session target = createClientSession();
		return (Session) Proxy.newProxyInstance(target.getClass().getClassLoader(),
				new Class[] {Session.class}, new ManagedClientInvocationHandler(target));
	}

	/**
	 * Create a transaction-aware Session reference for this factory's master Session,
	 * expecting transactions to be registered for this SessionFactory.
	 * @see #getMasterSession()
	 * @see oracle.toplink.sessions.Session#getActiveSession()
	 * @see oracle.toplink.sessions.Session#getActiveUnitOfWork()
	 */
	public Session createTransactionAwareSession() throws TopLinkException {
		logger.debug("Creating transaction-aware TopLink Session");
		return createTransactionAwareSession(this);
	}

	/**
	 * Create a transaction-aware Session reference for this factory's master Session,
	 * expecting transactions to be registered for the given SessionFactory.
	 * <p>This method is public to allow custom SessionFactory facades to access
	 * it directly, if necessary.
	 * @param sessionFactory the SessionFactory that transactions
	 * are expected to be registered for
	 * @see #getMasterSession()
	 * @see oracle.toplink.sessions.Session#getActiveSession()
	 * @see oracle.toplink.sessions.Session#getActiveUnitOfWork()
	 */
	public Session createTransactionAwareSession(SessionFactory sessionFactory) throws TopLinkException {
		Session target = getMasterSession();
		return (Session) Proxy.newProxyInstance(
				target.getClass().getClassLoader(), new Class[] {Session.class},
				new TransactionAwareInvocationHandler(sessionFactory, target));
	}


	/**
	 * Return this factory's "master" Session.
	 * For example, a TopLink ServerSession.
	 * <p>Used for creating transaction-aware Session reference.
	 */
	protected abstract Session getMasterSession();

	/**
	 * Create a new client Session for this factory's master Session.
	 * For example, a TopLink ClientSession.
	 * <p>Used for creating plain Sessions and "managed" client Sessions.
	 * @throws TopLinkException if creation of a client Session failed
	 */
	protected abstract Session createClientSession() throws TopLinkException;


	/**
	 * Invocation handler that decorates a client Session with an "active"
	 * UnitOfWork. For use in situations where Spring's TopLinkTransactionManager
	 * requires a "managed" thread-safe TopLink Session.
	 */
	private static class ManagedClientInvocationHandler implements InvocationHandler {

		private final Session target;

		private final UnitOfWork uow;

		public ManagedClientInvocationHandler(Session target) {
			this.target = target;
			this.uow = this.target.acquireUnitOfWork();
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on Session interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of SessionFactory proxy.
				return new Integer(System.identityHashCode(proxy));
			}
			else if (method.getName().equals("getActiveSession")) {
				return this.target;
			}
			else if (method.getName().equals("getActiveUnitOfWork")) {
				return this.uow;
			}
			else if (method.getName().equals("release")) {
				this.uow.release();
				this.target.release();
			}

			// Invoke method on target Session.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}


	/**
	 * Invocation handler that delegates <code>getActiveSession</code> calls
	 * to SessionFactoryUtils, for being aware of thread-bound transactions.
	 */
	private static class TransactionAwareInvocationHandler implements InvocationHandler {

		private final SessionFactory sessionFactory;

		private final Session target;

		public TransactionAwareInvocationHandler(SessionFactory sessionFactory, Session target) {
			this.sessionFactory = sessionFactory;
			this.target = target;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on Session interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of SessionFactory proxy.
				return new Integer(System.identityHashCode(proxy));
			}
			else if (method.getName().equals("getActiveSession")) {
				// Handle getActiveSession method: return transactional Session, if any.
				try {
					return SessionFactoryUtils.doGetSession(this.sessionFactory, false);
				}
				catch (IllegalStateException ex) {
					// getActiveSession is supposed to return the Session itself if no active one found.
					return this.target;
				}
			}
			else if (method.getName().equals("getActiveUnitOfWork")) {
				// Handle getActiveUnitOfWork method: return transactional UnitOfWork, if any.
				try {
					return SessionFactoryUtils.doGetSession(this.sessionFactory, false).getActiveUnitOfWork();
				}
				catch (IllegalStateException ex) {
					// getActiveUnitOfWork is supposed to return null if no active one found.
					return null;
				}
			}

			// Invoke method on target Session.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
