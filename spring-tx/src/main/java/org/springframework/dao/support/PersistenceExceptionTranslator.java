/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao.support;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;

/**
 * Interface implemented by Spring integrations with data access technologies
 * that throw runtime exceptions, such as JPA and Hibernate.
 *
 * <p>This allows consistent usage of combined exception translation functionality,
 * without forcing a single translator to understand every single possible type
 * of exception.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
@FunctionalInterface
public interface PersistenceExceptionTranslator {

	/**
	 * Translate the given runtime exception thrown by a persistence framework to a
	 * corresponding exception from Spring's generic
	 * {@link org.springframework.dao.DataAccessException} hierarchy, if possible.
	 * <p>Do not translate exceptions that are not understood by this translator:
	 * for example, if coming from another persistence framework, or resulting
	 * from user code or otherwise unrelated to persistence.
	 * <p>Of particular importance is the correct translation to
	 * DataIntegrityViolationException, for example on constraint violation.
	 * Implementations may use Spring JDBC's sophisticated exception translation
	 * to provide further information in the event of SQLException as a root cause.
	 * @param ex a RuntimeException to translate
	 * @return the corresponding DataAccessException (or {@code null} if the
	 * exception could not be translated, as in this case it may result from
	 * user code rather than from an actual persistence problem)
	 * @see org.springframework.dao.DataIntegrityViolationException
	 * @see org.springframework.jdbc.support.SQLExceptionTranslator
	 */
	@Nullable DataAccessException translateExceptionIfPossible(RuntimeException ex);

}
