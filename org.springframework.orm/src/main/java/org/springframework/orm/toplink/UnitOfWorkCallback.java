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
 * exposing a UnitOfWork to perform write operations on.
 *
 * <p>The exposed UnitOfWork will either be be the active UnitOfWork of
 * the current transaction, if any, or a temporarily acquired UnitOfWork
 * that will be committed at the end of the operation.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #doInUnitOfWork(oracle.toplink.sessions.UnitOfWork)
 * @see oracle.toplink.sessions.Session#getActiveUnitOfWork()
 */
public abstract class UnitOfWorkCallback implements TopLinkCallback {

	/**
	 * Determines the UnitOfWork to work on (either the active UnitOfWork or a
	 * temporarily acquired UnitOfWork) and delegates to <code>doInUnitOfWork</code>.
	 * @see #doInUnitOfWork(oracle.toplink.sessions.UnitOfWork)
	 */
	public final Object doInTopLink(Session session) throws TopLinkException {
		// Fetch active UnitOfWork or acquire temporary UnitOfWork.
		UnitOfWork unitOfWork = session.getActiveUnitOfWork();
		boolean newUnitOfWork = false;
		if (unitOfWork == null) {
			unitOfWork = session.acquireUnitOfWork();
			newUnitOfWork = true;
		}

		// Perform callback operation, committing the UnitOfWork unless
		// it is the active UnitOfWork of an externally managed transaction.
		try {
			Object result = doInUnitOfWork(unitOfWork);
			if (newUnitOfWork) {
				unitOfWork.commit();
			}
			return result;
		}
		finally {
			if (newUnitOfWork) {
				unitOfWork.release();
			}
		}
	}

	/**
	 * Called with a UnitOfWork to work on, either the active UnitOfWork or a
	 * temporarily acquired UnitOfWork (as determined by the transaction status).
	 * @param unitOfWork the TopLink UnitOfWork to perform write operations on
	 * @return a result object, or <code>null</code> if none
	 * @throws TopLinkException in case of TopLink errors
	 */
	protected abstract Object doInUnitOfWork(UnitOfWork unitOfWork) throws TopLinkException;

}
