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

import oracle.toplink.sessions.DatabaseSession;
import oracle.toplink.sessions.Session;

/**
 * Simple implementation of the SessionFactory interface: always returns
 * the passed-in Session as-is.
 *
 * <p>Useful for testing or standalone usage of TopLink-based data access objects.
 * <b>In a server environment, use ServerSessionFactory instead.</code>
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see ServerSessionFactory
 */
public class SingleSessionFactory implements SessionFactory {

	private final Session session;


	/**
	 * Create a new SingleSessionFactory with the given Session.
	 * @param session the TopLink Session to hold
	 */
	public SingleSessionFactory(Session session) {
		this.session = session;
	}


	/**
	 * Return the held TopLink Session as-is.
	 */
	public Session createSession() {
		return this.session;
	}

	/**
	 * Throws an UnsupportedOperationException: SingleSessionFactory does not
	 * support managed client Sessions. Use ServerSessionFactory instead.
	 */
	public Session createManagedClientSession() {
		throw new UnsupportedOperationException("SingleSessionFactory does not support managed client Sessions");
	}

	/**
	 * Throws an UnsupportedOperationException: SingleSessionFactory does not
	 * support transaction-aware Sessions. Use ServerSessionFactory instead.
	 */
	public Session createTransactionAwareSession() {
		throw new UnsupportedOperationException("SingleSessionFactory does not support transaction-aware Sessions");
	}


	/**
	 * Shut the pre-configured TopLink Session down.
	 * @see oracle.toplink.sessions.DatabaseSession#logout()
	 * @see oracle.toplink.sessions.Session#release()
	 */
	public void close() {
		if (this.session instanceof DatabaseSession) {
			((DatabaseSession) this.session).logout();
		}
		this.session.release();
	}

}
