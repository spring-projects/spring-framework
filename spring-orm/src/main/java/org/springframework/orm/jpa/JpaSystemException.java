/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * JPA-specific subclass of UncategorizedDataAccessException,
 * for JPA system errors that do not match any concrete
 * {@code org.springframework.dao} exceptions.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible
 */
@SuppressWarnings("serial")
public class JpaSystemException extends UncategorizedDataAccessException {

	public JpaSystemException(RuntimeException ex) {
		super(ex.getMessage(), ex);
	}

}
