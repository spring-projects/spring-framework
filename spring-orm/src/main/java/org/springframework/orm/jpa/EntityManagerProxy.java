/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa;

import jakarta.persistence.EntityManager;

/**
 * Subinterface of {@link jakarta.persistence.EntityManager} to be implemented by
 * EntityManager proxies. Allows access to the underlying target EntityManager.
 *
 * <p>This interface is mainly intended for framework usage. Application code
 * should prefer the use of the {@link jakarta.persistence.EntityManager#getDelegate()}
 * method to access native functionality of the underlying resource.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public interface EntityManagerProxy extends EntityManager {

	/**
	 * Return the underlying EntityManager that this proxy will delegate to.
	 * <p>In case of an extended EntityManager, this will be the associated
	 * raw EntityManager.
	 * <p>In case of a shared ("transactional") EntityManager, this will be
	 * the raw EntityManager that is currently associated with the transaction.
	 * Outside a transaction, an IllegalStateException will be thrown.
	 * @return the underlying raw EntityManager (never {@code null})
	 * @throws IllegalStateException if no underlying EntityManager is available
	 */
	EntityManager getTargetEntityManager() throws IllegalStateException;

}
