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
import oracle.toplink.threetier.ServerSession;

/**
 * Full-fledged default implementation of the SessionFactory interface:
 * creates ClientSessions for a given ServerSession.
 *
 * <p>Can create a special ClientSession subclass for managed Sessions, carrying
 * an active UnitOfWork that expects to be committed at transaction completion
 * (just like a plain TopLink Session does within a JTA transaction).
 *
 * <p>Can also create a transaction-aware Session reference that returns the
 * active transactional Session on <code>getActiveSession</code>.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see SingleSessionFactory
 * @see oracle.toplink.sessions.Session#getActiveUnitOfWork()
 * @see oracle.toplink.sessions.Session#getActiveSession()
 */
public class ServerSessionFactory extends AbstractSessionFactory {

	private final ServerSession serverSession;


	/**
	 * Create a new ServerSessionFactory for the given ServerSession.
	 * @param serverSession the TopLink ServerSession to create ClientSessions for
	 */
	public ServerSessionFactory(ServerSession serverSession) {
		this.serverSession = serverSession;
	}


	/**
	 * Return this factory's ServerSession as-is.
	 */
	protected Session getMasterSession() {
		return this.serverSession;
	}

	/**
	 * Create a plain ClientSession for this factory's ServerSession.
	 * @see oracle.toplink.threetier.ServerSession#acquireClientSession()
	 */
	protected Session createClientSession() throws TopLinkException {
		return this.serverSession.acquireClientSession();
	}


	/**
	 * Shut the pre-configured TopLink ServerSession down.
	 * @see oracle.toplink.sessions.DatabaseSession#logout()
	 * @see oracle.toplink.sessions.Session#release()
	 */
	public void close() {
		this.serverSession.logout();
		this.serverSession.release();
	}

}
