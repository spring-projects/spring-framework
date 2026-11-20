/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.orm.ObjectRetrievalFailureException;

/**
 * JPA-specific subclass of ObjectRetrievalFailureException.
 * Converts JPA's EntityNotFoundException.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible
 */
@SuppressWarnings("serial")
public class JpaObjectRetrievalFailureException extends ObjectRetrievalFailureException {

	public JpaObjectRetrievalFailureException(EntityNotFoundException ex) {
		super(ex.getMessage(), ex);
	}

}
