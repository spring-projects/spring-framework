/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa;

import jakarta.persistence.OptimisticLockException;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * JPA-specific subclass of ObjectOptimisticLockingFailureException.
 * Converts JPA's OptimisticLockException.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible
 */
@SuppressWarnings("serial")
public class JpaOptimisticLockingFailureException extends ObjectOptimisticLockingFailureException {

	public JpaOptimisticLockingFailureException(OptimisticLockException ex) {
		super(ex.getMessage(), ex);
	}

}
