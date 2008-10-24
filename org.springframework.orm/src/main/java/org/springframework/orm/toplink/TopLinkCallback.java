/*
 * Copyright 2002-2006 the original author or authors.
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
 * Callback interface for TopLink code. To be used with {@link TopLinkTemplate}'s
 * execution methods, often as anonymous classes within a method implementation.
 * A typical implementation will call TopLink Session CRUD to perform some
 * operations on persistent objects.
 *
 * <p>The <code>Session</code> that gets passed into the <code>doInTopLink</code> method
 * is usually a thread-safe <code>ClientSession</code>. Since this provides access to the
 * TopLink shared cache, it is possible for implementations of this interface to return
 * references to <i>read-only objects from the shared cache</i>. These objects
 * <i>must not be modified</i> by application code outside of the DAO layer.
 * If persistent objects need to be edited, they should be loaded from (or registered with)
 * a TopLink UnitOfWork, or they should be explicitly copied and merged back into a
 * <code>UnitOfWork</code> at a later point of time.
 *
 * <p>Users can access a <code>UnitOfWork</code> by using the <code>getActiveUnitOfWork</code>
 * method on the <code>Session</code>. Normally, this will only be done when there is an
 * active non-read-only transaction being managed by Spring's {@link TopLinkTransactionManager}
 * or by an external transaction controller (usually a J2EE server's JTA provider,
 * configured in TopLink). The <code>getActiveUnitOfWork</code> method will return
 * <code>null</code> outside of a managed transaction.
 *
 * @author Juergen Hoeller
 * @author <a href="mailto:@james.x.clark@oracle.com">James Clark</a>
 * @see TopLinkTemplate
 * @see TopLinkTransactionManager
 */
public interface TopLinkCallback {

	/**
	 * Gets called by <code>TopLinkTemplate.execute</code> with an active
	 * <code>Session</code>. Does not need to care about activating or closing
	 * the TopLink <code>Session</code>, or handling transactions.
	 *
	 * <p>Note that write operations should usually be performed on the active
	 * <code>UnitOfWork</code> within an externally controlled transaction, through
	 * calling <code>getActiveUnitOfWork</code>. However, an implementation can also
	 * choose to use <code>acquireUnitOfWork</code> to create an independent
	 * <code>UnitOfWork</code>, which it needs to commit at the end of the operation.
	 *
	 * <p>Allows for returning a result object created within the callback,
	 * i.e. a domain object or a collection of domain objects.
	 * A thrown custom RuntimeException is treated as an application exception:
	 * It gets propagated to the caller of the template.
	 *
	 * @param session active TopLink Session
	 * @return a result object, or <code>null</code> if none
	 * @throws TopLinkException if thrown by the TopLink API
	 * @see oracle.toplink.sessions.Session#getActiveUnitOfWork()
	 * @see oracle.toplink.sessions.Session#acquireUnitOfWork()
	 * @see TopLinkTemplate#execute
	 * @see TopLinkTemplate#executeFind
	 */
	Object doInTopLink(Session session) throws TopLinkException;

}
