/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.dao.support;

import org.springframework.dao.DataAccessException;

/**
 * Interface implemented by Spring integrations with data access technologies
 * that throw runtime exceptions, such as JPA, TopLink, JDO and Hibernate.
 *
 * <p>This allows consistent usage of combined exception translation functionality,
 * without forcing a single translator to understand every single possible type
 * of exception.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface PersistenceExceptionTranslator {

	/**
	 * Translate the given runtime exception thrown by a persistence framework to a
	 * corresponding exception from Spring's generic DataAccessException hierarchy,
	 * if possible.
	 * <p>Do not translate exceptions that are not understand by this translator:
	 * for example, if coming from another persistence framework, or resulting
	 * from user code and unrelated to persistence.
	 * <p>Of particular importance is the correct translation to
	 * DataIntegrityViolationException, for example on constraint violation.
	 * Implementations may use Spring JDBC's sophisticated exception translation
	 * to provide further information in the event of SQLException as a root cause.
	 * @param ex a RuntimeException thrown
	 * @return the corresponding DataAccessException (or <code>null</code> if the
	 * exception could not be translated, as in this case it may result from
	 * user code rather than an actual persistence problem)
	 * @see org.springframework.dao.DataIntegrityViolationException
	 * @see org.springframework.jdbc.support.SQLExceptionTranslator
	 */
	DataAccessException translateExceptionIfPossible(RuntimeException ex);

}
