/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa;

import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;

import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * Resource holder wrapping a JPA {@link EntityManager}.
 * {@link JpaTransactionManager} binds instances of this class to the thread,
 * for a given {@link jakarta.persistence.EntityManagerFactory}.
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see JpaTransactionManager
 * @see EntityManagerFactoryUtils
 */
public class EntityManagerHolder extends ResourceHolderSupport {

	protected @Nullable EntityManager entityManager;

	protected @Nullable Object entityAgent;

	private boolean transactionActive;

	private @Nullable SavepointManager savepointManager;


	public EntityManagerHolder(@Nullable EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	EntityManagerHolder(@Nullable Object entityAgent) {
		this.entityAgent = entityAgent;
	}

	protected EntityManagerHolder(EntityManagerHolder original) {
		this.entityManager = original.entityManager;
		this.entityAgent = original.entityAgent;
		this.transactionActive = original.transactionActive;
		this.savepointManager = original.savepointManager;
	}


	void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public EntityManager getEntityManager() {
		Assert.state(this.entityManager != null, "No EntityManager available");
		return this.entityManager;
	}

	void setEntityAgent(Object entityAgent) {
		this.entityAgent = entityAgent;
	}

	Object getEntityAgent() {
		Assert.state(this.entityAgent != null, "No EntityAgent available");
		return this.entityAgent;
	}

	boolean hasEntityManager() {
		return (this.entityManager != null);
	}

	boolean hasEntityAgent() {
		return (this.entityAgent != null);
	}

	protected void setTransactionActive(boolean transactionActive) {
		this.transactionActive = transactionActive;
	}

	protected boolean isTransactionActive() {
		return this.transactionActive;
	}

	protected void setSavepointManager(@Nullable SavepointManager savepointManager) {
		this.savepointManager = savepointManager;
	}

	protected @Nullable SavepointManager getSavepointManager() {
		return this.savepointManager;
	}


	@Override
	public void clear() {
		super.clear();
		this.transactionActive = false;
		this.savepointManager = null;
	}

	protected void closeAll() {
		EntityManagerFactoryUtils.closeEntityHandler(this.entityManager);
		EntityManagerFactoryUtils.closeEntityHandler(this.entityAgent);
		this.entityManager = null;
		this.entityAgent = null;
	}

}
