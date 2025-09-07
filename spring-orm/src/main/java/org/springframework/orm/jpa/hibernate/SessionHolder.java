/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa.hibernate;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.jspecify.annotations.Nullable;

import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.util.Assert;

/**
 * Resource holder wrapping a Hibernate {@link Session} (plus an optional {@link Transaction}).
 * {@link HibernateTransactionManager} binds instances of this class to the thread,
 * for a given {@link org.hibernate.SessionFactory}. Extends {@link EntityManagerHolder},
 * automatically exposing an {@code EntityManager} handle.
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see HibernateTransactionManager
 */
class SessionHolder extends EntityManagerHolder {

	private @Nullable StatelessSession statelessSession;

	private @Nullable Transaction transaction;

	private @Nullable FlushMode previousFlushMode;


	public SessionHolder(Session session) {
		super(session);
	}

	public SessionHolder(StatelessSession session) {
		super(null);
		this.statelessSession = session;
	}


	public void setSession(Session session) {
		this.entityManager = session;
	}

	public Session getSession() {
		return (Session) getEntityManager();
	}

	public boolean hasSession() {
		return (this.entityManager != null);
	}

	public void setStatelessSession(StatelessSession statelessSession) {
		this.statelessSession = statelessSession;
	}

	public StatelessSession getStatelessSession() {
		Assert.state(this.statelessSession != null, "No StatelessSession available");
		return this.statelessSession;
	}

	public boolean hasStatelessSession() {
		return (this.statelessSession != null);
	}

	public void setTransaction(@Nullable Transaction transaction) {
		this.transaction = transaction;
		setTransactionActive(transaction != null);
	}

	public @Nullable Transaction getTransaction() {
		return this.transaction;
	}

	public void setPreviousFlushMode(@Nullable FlushMode previousFlushMode) {
		this.previousFlushMode = previousFlushMode;
	}

	public @Nullable FlushMode getPreviousFlushMode() {
		return this.previousFlushMode;
	}


	@Override
	public void clear() {
		super.clear();
		this.transaction = null;
		this.previousFlushMode = null;
	}

	@Override
	protected void closeAll() {
		super.closeAll();
		if (this.statelessSession != null && this.statelessSession.isOpen()) {
			this.statelessSession.close();
		}
	}

}
