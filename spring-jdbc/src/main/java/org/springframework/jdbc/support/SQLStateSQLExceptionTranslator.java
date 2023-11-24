/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.support;

import java.sql.SQLException;
import java.util.Set;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.lang.Nullable;

/**
 * {@link SQLExceptionTranslator} implementation that analyzes the SQL state in
 * the {@link SQLException} based on the first two digits (the SQL state "class").
 * Detects standard SQL state values and well-known vendor-specific SQL states.
 *
 * <p>Not able to diagnose all problems, but is portable between databases and
 * does not require special initialization (no database vendor detection, etc.).
 * For more precise translation, consider {@link SQLErrorCodeSQLExceptionTranslator}.
 *
 * <p>This translator is commonly used as a {@link #setFallbackTranslator fallback}
 * behind a primary translator such as {@link SQLErrorCodeSQLExceptionTranslator} or
 * {@link SQLExceptionSubclassTranslator}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @see java.sql.SQLException#getSQLState()
 * @see SQLErrorCodeSQLExceptionTranslator
 * @see SQLExceptionSubclassTranslator
 */
public class SQLStateSQLExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {

	private static final Set<String> BAD_SQL_GRAMMAR_CODES = Set.of(
			"07",  // Dynamic SQL error
			"21",  // Cardinality violation
			"2A",  // Syntax error direct SQL
			"37",  // Syntax error dynamic SQL
			"42",  // General SQL syntax error
			"65"   // Oracle: unknown identifier
		);

	private static final Set<String> DATA_INTEGRITY_VIOLATION_CODES = Set.of(
			"01",  // Data truncation
			"02",  // No data found
			"22",  // Value out of range
			"23",  // Integrity constraint violation
			"27",  // Triggered data change violation
			"44"   // With check violation
		);

	private static final Set<String> DATA_ACCESS_RESOURCE_FAILURE_CODES = Set.of(
			"08",  // Connection exception
			"53",  // PostgreSQL: insufficient resources (e.g. disk full)
			"54",  // PostgreSQL: program limit exceeded (e.g. statement too complex)
			"57",  // DB2: out-of-memory exception / database not started
			"58"   // DB2: unexpected system error
		);

	private static final Set<String> TRANSIENT_DATA_ACCESS_RESOURCE_CODES = Set.of(
			"JW",  // Sybase: internal I/O error
			"JZ",  // Sybase: unexpected I/O error
			"S1"   // DB2: communication failure
		);

	private static final Set<String> PESSIMISTIC_LOCKING_FAILURE_CODES = Set.of(
			"40",  // Transaction rollback
			"61"   // Oracle: deadlock
		);

	private static final Set<Integer> DUPLICATE_KEY_ERROR_CODES = Set.of(
			1,     // Oracle
			301,   // SAP HANA
			1062,  // MySQL/MariaDB
			2601,  // MS SQL Server
			2627   // MS SQL Server
		);


	@Override
	@Nullable
	protected DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
		// First, the getSQLState check...
		String sqlState = getSqlState(ex);
		if (sqlState != null && sqlState.length() >= 2) {
			String classCode = sqlState.substring(0, 2);
			if (logger.isDebugEnabled()) {
				logger.debug("Extracted SQL state class '" + classCode + "' from value '" + sqlState + "'");
			}
			if (BAD_SQL_GRAMMAR_CODES.contains(classCode)) {
				return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
			}
			else if (DATA_INTEGRITY_VIOLATION_CODES.contains(classCode)) {
				if (indicatesDuplicateKey(sqlState, ex.getErrorCode())) {
					return new DuplicateKeyException(buildMessage(task, sql, ex), ex);
				}
				return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
			}
			else if (DATA_ACCESS_RESOURCE_FAILURE_CODES.contains(classCode)) {
				return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
			}
			else if (TRANSIENT_DATA_ACCESS_RESOURCE_CODES.contains(classCode)) {
				return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
			}
			else if (PESSIMISTIC_LOCKING_FAILURE_CODES.contains(classCode)) {
				if (indicatesCannotAcquireLock(sqlState)) {
					return new CannotAcquireLockException(buildMessage(task, sql, ex), ex);
				}
				return new PessimisticLockingFailureException(buildMessage(task, sql, ex), ex);
			}
		}

		// For MySQL: exception class name indicating a timeout?
		// (since MySQL doesn't throw the JDBC 4 SQLTimeoutException)
		if (ex.getClass().getName().contains("Timeout")) {
			return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
		}

		// Couldn't resolve anything proper - resort to UncategorizedSQLException.
		return null;
	}

	/**
	 * Gets the SQL state code from the supplied {@link SQLException exception}.
	 * <p>Some JDBC drivers nest the actual exception from a batched update, so we
	 * might need to dig down into the nested exception.
	 * @param ex the exception from which the {@link SQLException#getSQLState() SQL state}
	 * is to be extracted
	 * @return the SQL state code
	 */
	@Nullable
	private String getSqlState(SQLException ex) {
		String sqlState = ex.getSQLState();
		if (sqlState == null) {
			SQLException nestedEx = ex.getNextException();
			if (nestedEx != null) {
				sqlState = nestedEx.getSQLState();
			}
		}
		return sqlState;
	}


	/**
	 * Check whether the given SQL state and the associated error code (in case
	 * of a generic SQL state value) indicate a {@link DuplicateKeyException}:
	 * either SQL state 23505 as a specific indication, or the generic SQL state
	 * 23000 with a well-known vendor code.
	 * @param sqlState the SQL state value
	 * @param errorCode the error code
	 */
	static boolean indicatesDuplicateKey(@Nullable String sqlState, int errorCode) {
		return ("23505".equals(sqlState) ||
				("23000".equals(sqlState) && DUPLICATE_KEY_ERROR_CODES.contains(errorCode)));
	}

	/**
	 * Check whether the given SQL state indicates a {@link CannotAcquireLockException},
	 * with SQL state 40001 as a specific indication.
	 * @param sqlState the SQL state value
	 */
	static boolean indicatesCannotAcquireLock(@Nullable String sqlState) {
		return "40001".equals(sqlState);
	}

}
