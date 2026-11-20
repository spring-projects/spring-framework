/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.hibernate;

import jakarta.persistence.EntityManager;
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
public class SessionHolder extends EntityManagerHolder {

	private @Nullable Transaction transaction;

	private @Nullable FlushMode previousFlushMode;


	public SessionHolder(Session session) {
		super(session);
	}

	SessionHolder(StatelessSession session) {
		super((EntityManager) null);
		setStatelessSession(session);
	}

	SessionHolder(EntityManagerHolder emHolder) {
		super(emHolder);
	}


	void setSession(Session session) {
		this.entityManager = session;
	}

	public Session getSession() {
		return (Session) getEntityManager();
	}

	boolean hasSession() {
		return (this.entityManager != null);
	}

	void setStatelessSession(StatelessSession statelessSession) {
		this.entityAgent = statelessSession;
	}

	StatelessSession getStatelessSession() {
		Assert.state(this.entityAgent != null, "No StatelessSession available");
		return (StatelessSession) this.entityAgent;
	}

	boolean hasStatelessSession() {
		return (this.entityAgent != null);
	}

	void setTransaction(@Nullable Transaction transaction) {
		this.transaction = transaction;
		setTransactionActive(transaction != null);
	}

	@Nullable Transaction getTransaction() {
		return this.transaction;
	}

	void setPreviousFlushMode(@Nullable FlushMode previousFlushMode) {
		this.previousFlushMode = previousFlushMode;
	}

	@Nullable FlushMode getPreviousFlushMode() {
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
	}

}
