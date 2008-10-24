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
import oracle.toplink.sessions.UnitOfWork;

/**
 * Convenient abstract implementation of the TopLinkCallback interface,
 * exposing either the plain TopLink Session or the TopLink UnitOfWork
 * (which extends the Session interface) to code that reads persistent objects.
 *
 * <p>Exposes the UnitOfWork if there is an active one (that is, if we're running
 * within a non-read-only transaction); else exposes the Session itself.
 * This allows to modify returned objects within a transaction, which is
 * often desired, while the same code will return shared cache objects
 * if running outside a transaction.
 *
 * <p>If "enforceReadOnly" is demanded, the callback will always expose the
 * Session itself, avoiding the UnitOfWork overhead in any case.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see oracle.toplink.sessions.Session#getActiveUnitOfWork()
 * @see #readFromSession(oracle.toplink.sessions.Session)
 */
public abstract class SessionReadCallback implements TopLinkCallback {

	private final boolean enforceReadOnly;

	/**
	 * Create a new SessionReadCallback, not enforcing read-only objects.
	 */
	public SessionReadCallback() {
		this.enforceReadOnly = false;
	}

	/**
	 * Create a new SessionReadCallback, enforcing read-only objects if demanded.
	 * @param enforceReadOnly whether to enforce returning read-only objects,
	 * even if running within a non-read-only transaction
	 */
	public SessionReadCallback(boolean enforceReadOnly) {
		this.enforceReadOnly = enforceReadOnly;
	}

	/**
	 * Determines the Session to work on (either the active UnitOfWork
	 * or the plain Session) and delegates to <code>readFromSession</code>.
	 * @see #readFromSession(oracle.toplink.sessions.Session)
	 */
	public final Object doInTopLink(Session session) throws TopLinkException {
		Session sessionToUse = session;
		if (!this.enforceReadOnly) {
			UnitOfWork unitOfWork = session.getActiveUnitOfWork();
			if (unitOfWork != null) {
				sessionToUse = unitOfWork;
			}
		}
		return readFromSession(sessionToUse);
	}

	/**
	 * Called with a Session to work on, either the active UnitOfWork
	 * or the plain Session (as determined by the transaction status).
	 * @param session the TopLink Session to perform read operations on
	 * @return a result object, or <code>null</code> if none
	 * @throws TopLinkException in case of TopLink errors
	 */
	protected abstract Object readFromSession(Session session) throws TopLinkException;

}
