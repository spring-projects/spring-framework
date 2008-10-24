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
import oracle.toplink.exceptions.ValidationException;
import oracle.toplink.sessionbroker.SessionBroker;
import oracle.toplink.sessions.Session;

/**
 * Spring SessionFactory implementation allowing users to
 * inject a TopLink Session built from a TopLink SessionBroker.
 *
 * SessionBrokers are used identically to any other TopLink Session.  DAO code
 * should never have to distinguish between Sessions which broker requests to
 * multiple databases and Sessions which manage requests to a single database.
 *
 * The only pertinent difference in the SessionBroker api involves the method
 * for obtaining a thread-safe "client" Session from the SessionBroker.
 * Instead of the typical acquireClientSession
 * method, this SessionFactory implementation uses the
 * acquireClientSessionBroker method.
 * If a SessionBroker aggregates non thread-safe DatabaseSessions,
 * the factory will throw UnsupportedOperationExceptions
 * if used to create managed or transaction-aware Sessions.
 *
 * @author <a href="mailto:james.x.clark@oracle.com">James Clark</a>
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see org.springframework.orm.toplink.ServerSessionFactory
 * @see oracle.toplink.threetier.ServerSession#acquireClientSession()
 * @see oracle.toplink.sessionbroker.SessionBroker#acquireClientSessionBroker()
 */
public class SessionBrokerSessionFactory extends AbstractSessionFactory {

	private final SessionBroker sessionBroker;


	/**
	 * Create a new SessionBrokerSessionFactory for the given SessionBroker.
	 * @param broker the TopLink SessionBroker to fetch Sessions from
	 */
	public SessionBrokerSessionFactory(SessionBroker broker) {
		this.sessionBroker = broker;
	}


	/**
	 * Try to create a client Session; fall back to the master Session,
	 * if no client Session can be created (because of the session broker's
	 * configuration).
	 * @see #createClientSession()
	 * @see #getMasterSession()
	 */
	public Session createSession() throws TopLinkException {
		try {
			return createClientSession();
		}
		catch (ValidationException ex) {
			logger.debug(
					"Could not create TopLink client session for SessionBroker - returning SessionBroker itself", ex);
			return getMasterSession();
		}
	}

	/**
	 * Return this factory's SessionBroker as-is.
	 */
	protected Session getMasterSession() {
		return this.sessionBroker;
	}

	/**
	 * Create a plain client SessionBroker for this factory's ServerSession.
	 * @see oracle.toplink.sessionbroker.SessionBroker#acquireClientSessionBroker()
	 */
	protected Session createClientSession() throws TopLinkException {
		return this.sessionBroker.acquireClientSessionBroker();
	}


	/**
	 * Shut the pre-configured TopLink SessionBroker down.
	 * @see oracle.toplink.sessions.DatabaseSession#logout()
	 * @see oracle.toplink.sessions.Session#release()
	 */
	public void close() {
		this.sessionBroker.logout();
		this.sessionBroker.release();
	}

}
