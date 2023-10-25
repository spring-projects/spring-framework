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

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class SQLErrorCodeSQLExceptionTranslatorTests {

	private static final SQLErrorCodes ERROR_CODES = new SQLErrorCodes();
	static {
		ERROR_CODES.setBadSqlGrammarCodes("1", "2");
		ERROR_CODES.setInvalidResultSetAccessCodes("3", "4");
		ERROR_CODES.setDuplicateKeyCodes("10");
		ERROR_CODES.setDataAccessResourceFailureCodes("5");
		ERROR_CODES.setDataIntegrityViolationCodes("6");
		ERROR_CODES.setCannotAcquireLockCodes("7");
		ERROR_CODES.setDeadlockLoserCodes("8");
		ERROR_CODES.setCannotSerializeTransactionCodes("9");
	}

	private SQLErrorCodeSQLExceptionTranslator translator = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);


	@Test
	@SuppressWarnings("deprecation")
	void errorCodeTranslation() {
		SQLException badSqlEx = new SQLException("", "", 1);
		BadSqlGrammarException bsgEx = (BadSqlGrammarException) translator.translate("task", "SQL", badSqlEx);
		assertThat(bsgEx.getSql()).isEqualTo("SQL");
		assertThat((Object) bsgEx.getSQLException()).isEqualTo(badSqlEx);

		SQLException cause = new SQLException("", "", 4);
		InvalidResultSetAccessException invResEx = (InvalidResultSetAccessException) translator.translate("task", "SQL", cause);
		assertThat(invResEx.getSql()).isEqualTo("SQL");
		assertThat((Object) invResEx.getSQLException()).isEqualTo(cause);

		checkTranslation(5, DataAccessResourceFailureException.class);
		checkTranslation(6, DataIntegrityViolationException.class);
		checkTranslation(7, CannotAcquireLockException.class);
		checkTranslation(8, org.springframework.dao.DeadlockLoserDataAccessException.class);
		checkTranslation(9, org.springframework.dao.CannotSerializeTransactionException.class);
		checkTranslation(10, DuplicateKeyException.class);

		SQLException dupKeyEx = new SQLException("", "", 10);
		DataAccessException dataAccessException = translator.translate("task", "SQL", dupKeyEx);
		assertThat(dataAccessException)
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasCause(dupKeyEx);

		// Test fallback. We assume that no database will ever return this error code,
		// but 07xxx will be bad grammar picked up by the fallback SQLState translator
		cause = new SQLException("", "07xxx", 666666666);
		bsgEx = (BadSqlGrammarException) translator.translate("task", "SQL2", cause);
		assertThat(bsgEx.getSql()).isEqualTo("SQL2");
		assertThat((Object) bsgEx.getSQLException()).isEqualTo(cause);
	}

	private void checkTranslation(int errorCode, Class<? extends Exception> expectedType) {
		SQLException sqlException = new SQLException("", "", errorCode);
		DataAccessException dataAccessException = this.translator.translate("", "", sqlException);
		assertThat(dataAccessException)
			.isInstanceOf(expectedType)
			.hasCause(sqlException);
	}

	@Test
	void batchExceptionTranslation() {
		SQLException badSqlEx = new SQLException("", "", 1);
		BatchUpdateException batchUpdateEx = new BatchUpdateException();
		batchUpdateEx.setNextException(badSqlEx);
		BadSqlGrammarException bsgEx = (BadSqlGrammarException) translator.translate("task", "SQL", batchUpdateEx);
		assertThat(bsgEx.getSql()).isEqualTo("SQL");
		assertThat((Object) bsgEx.getSQLException()).isEqualTo(badSqlEx);
	}

	@Test
	void dataTruncationTranslation() {
		SQLException dataAccessEx = new SQLException("", "", 5);
		DataTruncation dataTruncation = new DataTruncation(1, true, true, 1, 1, dataAccessEx);
		DataAccessException dataAccessException = translator.translate("task", "SQL", dataTruncation);
		assertThat(dataAccessException)
			.isInstanceOf(DataAccessResourceFailureException.class)
			.hasCause(dataTruncation);
	}

	@Test
	@SuppressWarnings("serial")
	void customTranslateMethodTranslation() {
		final String TASK = "TASK";
		final String SQL = "SQL SELECT *";
		final DataAccessException customDex = new DataAccessException("") {};

		final SQLException badSqlEx = new SQLException("", "", 1);
		SQLException integrityViolationEx = new SQLException("", "", 6);

		translator = new SQLErrorCodeSQLExceptionTranslator() {
			@SuppressWarnings("deprecation")
			@Override
			@Nullable
			protected DataAccessException customTranslate(String task, @Nullable String sql, SQLException sqlException) {
				assertThat(task).isEqualTo(TASK);
				assertThat(sql).isEqualTo(SQL);
				return (sqlException == badSqlEx) ? customDex : null;
			}
		};
		translator.setSqlErrorCodes(ERROR_CODES);

		// Should custom translate this
		assertThat(translator.translate(TASK, SQL, badSqlEx)).isEqualTo(customDex);

		// Shouldn't custom translate this
		DataAccessException dataAccessException = translator.translate(TASK, SQL, integrityViolationEx);
		assertThat(dataAccessException)
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasCause(integrityViolationEx);
	}

	@Test
	void customExceptionTranslation() {
		final String TASK = "TASK";
		final String SQL = "SQL SELECT *";
		final SQLErrorCodes customErrorCodes = new SQLErrorCodes();
		final CustomSQLErrorCodesTranslation customTranslation = new CustomSQLErrorCodesTranslation();

		customErrorCodes.setBadSqlGrammarCodes("1", "2");
		customErrorCodes.setDataIntegrityViolationCodes("3", "4");
		customTranslation.setErrorCodes("1");
		customTranslation.setExceptionClass(CustomErrorCodeException.class);
		customErrorCodes.setCustomTranslations(customTranslation);

		translator = new SQLErrorCodeSQLExceptionTranslator(customErrorCodes);

		// Should custom translate this
		SQLException badSqlEx = new SQLException("", "", 1);
		DataAccessException dataAccessException = translator.translate(TASK, SQL, badSqlEx);
		assertThat(dataAccessException)
			.isInstanceOf(CustomErrorCodeException.class)
			.hasCause(badSqlEx);

		// Shouldn't custom translate this
		SQLException invResEx = new SQLException("", "", 3);
		dataAccessException = translator.translate(TASK, SQL, invResEx);
		assertThat(dataAccessException)
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasCause(invResEx);

		// Shouldn't custom translate this - invalid class
		assertThatIllegalArgumentException().isThrownBy(() -> customTranslation.setExceptionClass(String.class));
	}

	@Test
	void dataSourceInitialization() throws Exception {
		SQLException connectionException = new SQLException();
		SQLException duplicateKeyException = new SQLException("test", "", 1);

		DataSource dataSource = mock();
		given(dataSource.getConnection()).willThrow(connectionException);

		translator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
		assertThat(translator.translate("test", null, duplicateKeyException)).isNull();

		DatabaseMetaData databaseMetaData = mock();
		given(databaseMetaData.getDatabaseProductName()).willReturn("Oracle");

		Connection connection = mock();
		given(connection.getMetaData()).willReturn(databaseMetaData);

		reset(dataSource);
		given(dataSource.getConnection()).willReturn(connection);
		assertThat(translator.translate("test", null, duplicateKeyException)).isInstanceOf(DuplicateKeyException.class);

		verify(connection).close();
	}

}
