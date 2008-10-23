/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.ejb.support;

import javax.ejb.SessionContext;

/**
 * Base class for Spring-based EJB 2.x session beans. Not intended for direct
 * subclassing: Extend {@link AbstractStatelessSessionBean} or
 * {@link AbstractStatefulSessionBean} instead.
 *
 * <p>This class saves the session context provided by the EJB container in an
 * instance variable and exposes it through the {@link SmartSessionBean} interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class AbstractSessionBean extends AbstractEnterpriseBean implements SmartSessionBean {

	/** The SessionContext passed to this EJB */
	private SessionContext sessionContext;


	/**
	 * Set the session context for this EJB.
	 * <p><b>When overriding this method, be sure to invoke this form of it first.</b>
	 */
	public void setSessionContext(SessionContext sessionContext) {
		this.sessionContext = sessionContext;
	}

	/**
	 * Convenience method for subclasses, returning the EJB session context
	 * saved on initialization ({@link #setSessionContext}).
	 */
	public final SessionContext getSessionContext() {
		return this.sessionContext;
	}

}
