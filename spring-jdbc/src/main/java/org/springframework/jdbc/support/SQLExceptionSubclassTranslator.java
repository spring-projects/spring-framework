/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support;

import java.sql.BatchUpdateException;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLNonTransientException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLTransientException;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;

/**
 * {@link SQLExceptionTranslator} implementation which analyzes the specific
 * {@link java.sql.SQLException} subclass thrown by the JDBC driver.
 *
 * <p>Falls back to a standard {@link SQLStateSQLExceptionTranslator} if the JDBC
 * driver does not actually expose JDBC 4 compliant {@code SQLException} subclasses.
 *
 * <p>This translator serves as the default JDBC exception translator as of 6.0.
 * As of 6.2.12, it specifically introspects {@link java.sql.BatchUpdateException}
 * to look at the underlying exception, analogous to the former default
 * {@link SQLErrorCodeSQLExceptionTranslator}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 * @see java.sql.SQLTransientException
 * @see java.sql.SQLNonTransientException
 * @see java.sql.SQLRecoverableException
 */
public class SQLExceptionSubclassTranslator extends AbstractFallbackSQLExceptionTranslator {

	public SQLExceptionSubclassTranslator() {
		setFallbackTranslator(new SQLStateSQLExceptionTranslator());
	}

	@Override
	protected @Nullable DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
		SQLException sqlEx = ex;
		if (sqlEx instanceof BatchUpdateException && sqlEx.getNextException() != null) {
			sqlEx = sqlEx.getNextException();
		}

		if (sqlEx instanceof SQLTransientException) {
			if (sqlEx instanceof SQLTransientConnectionException) {
				return new TransientDataAccessResourceException(buildMessage(task, sql, sqlEx), ex);
			}
			if (sqlEx instanceof SQLTransactionRollbackException) {
				if (SQLStateSQLExceptionTranslator.indicatesCannotAcquireLock(sqlEx.getSQLState())) {
					return new CannotAcquireLockException(buildMessage(task, sql, sqlEx), ex);
				}
				return new PessimisticLockingFailureException(buildMessage(task, sql, sqlEx), ex);
			}
			if (sqlEx instanceof SQLTimeoutException) {
				return new QueryTimeoutException(buildMessage(task, sql, sqlEx), ex);
			}
		}
		else if (sqlEx instanceof SQLNonTransientException) {
			if (sqlEx instanceof SQLNonTransientConnectionException) {
				return new DataAccessResourceFailureException(buildMessage(task, sql, sqlEx), ex);
			}
			if (sqlEx instanceof SQLDataException) {
				return new DataIntegrityViolationException(buildMessage(task, sql, sqlEx), ex);
			}
			if (sqlEx instanceof SQLIntegrityConstraintViolationException) {
				if (SQLStateSQLExceptionTranslator.indicatesDuplicateKey(sqlEx.getSQLState(), sqlEx.getErrorCode())) {
					return new DuplicateKeyException(buildMessage(task, sql, sqlEx), ex);
				}
				return new DataIntegrityViolationException(buildMessage(task, sql, sqlEx), ex);
			}
			if (sqlEx instanceof SQLInvalidAuthorizationSpecException) {
				return new PermissionDeniedDataAccessException(buildMessage(task, sql, sqlEx), ex);
			}
			if (sqlEx instanceof SQLSyntaxErrorException) {
				return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
			}
			if (sqlEx instanceof SQLFeatureNotSupportedException) {
				return new InvalidDataAccessApiUsageException(buildMessage(task, sql, sqlEx), ex);
			}
		}
		else if (sqlEx instanceof SQLRecoverableException) {
			return new RecoverableDataAccessException(buildMessage(task, sql, sqlEx), ex);
		}

		// Fallback to Spring's own SQL state translation...
		return null;
	}

}
