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

import oracle.toplink.sessions.Session;

import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * Session holder, wrapping a TopLink Session.
 * TopLinkTransactionManager binds instances of this class
 * to the thread, for a given SessionFactory.
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 *
 * @author Juergen Hoeller
 * @since 1.2
 */
public class SessionHolder extends ResourceHolderSupport {

	private final Session session;


	/**
	 * Create a new SessionHolder for the given TopLink Session.
	 * @param session the TopLink Session
	 */
	public SessionHolder(Session session) {
		Assert.notNull(session, "Session must not be null");
		this.session = session;
	}

	/**
	 * Return this holder's TopLink Session.
	 */
	public Session getSession() {
		return session;
	}

}
