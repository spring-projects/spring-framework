/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jdbc.support;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;

/**
 * Strategy interface for translating between {@link SQLException SQLExceptions}
 * and Spring's data access strategy-agnostic {@link DataAccessException}
 * hierarchy.
 *
 * <p>Implementations can be generic (for example, using
 * {@link java.sql.SQLException#getSQLState() SQLState} codes for JDBC) or wholly
 * proprietary (for example, using Oracle error codes) for greater precision.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.dao.DataAccessException
 */
@FunctionalInterface
public interface SQLExceptionTranslator {

	/**
	 * Translate the given {@link SQLException} into a generic {@link DataAccessException}.
	 * <p>The returned DataAccessException is supposed to contain the original
	 * {@code SQLException} as root cause. However, client code may not generally
	 * rely on this due to DataAccessExceptions possibly being caused by other resource
	 * APIs as well. That said, a {@code getRootCause() instanceof SQLException}
	 * check (and subsequent cast) is considered reliable when expecting JDBC-based
	 * access to have happened.
	 * @param task readable text describing the task being attempted
	 * @param sql SQL query or update that caused the problem (may be {@code null})
	 * @param ex the offending {@code SQLException}
	 * @return the DataAccessException, wrapping the {@code SQLException}
	 * @see org.springframework.dao.DataAccessException#getRootCause()
	 */
	DataAccessException translate(String task, String sql, SQLException ex);

}
