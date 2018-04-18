/*
 * Copyright 2002-2015 the original author or authors.
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

import java.sql.BatchUpdateException;
import java.sql.DataTruncation;
import java.sql.SQLException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.lang.Nullable;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class SQLErrorCodeSQLExceptionTranslatorTests {

	private static SQLErrorCodes ERROR_CODES = new SQLErrorCodes();
	static {
		ERROR_CODES.setBadSqlGrammarCodes(new String[] { "1", "2" });
		ERROR_CODES.setInvalidResultSetAccessCodes(new String[] { "3", "4" });
		ERROR_CODES.setDuplicateKeyCodes(new String[] {"10"});
		ERROR_CODES.setDataAccessResourceFailureCodes(new String[] { "5" });
		ERROR_CODES.setDataIntegrityViolationCodes(new String[] { "6" });
		ERROR_CODES.setCannotAcquireLockCodes(new String[] { "7" });
		ERROR_CODES.setDeadlockLoserCodes(new String[] { "8" });
		ERROR_CODES.setCannotSerializeTransactionCodes(new String[] { "9" });
	}

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void errorCodeTranslation() {
		SQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);

		SQLException badSqlEx = new SQLException("", "", 1);
		BadSqlGrammarException bsgex = (BadSqlGrammarException) sext.translate("task", "SQL", badSqlEx);
		assertEquals("SQL", bsgex.getSql());
		assertEquals(badSqlEx, bsgex.getSQLException());

		SQLException invResEx = new SQLException("", "", 4);
		InvalidResultSetAccessException irsex = (InvalidResultSetAccessException) sext.translate("task", "SQL", invResEx);
		assertEquals("SQL", irsex.getSql());
		assertEquals(invResEx, irsex.getSQLException());

		checkTranslation(sext, 5, DataAccessResourceFailureException.class);
		checkTranslation(sext, 6, DataIntegrityViolationException.class);
		checkTranslation(sext, 7, CannotAcquireLockException.class);
		checkTranslation(sext, 8, DeadlockLoserDataAccessException.class);
		checkTranslation(sext, 9, CannotSerializeTransactionException.class);
		checkTranslation(sext, 10, DuplicateKeyException.class);

		SQLException dupKeyEx = new SQLException("", "", 10);
		DataAccessException dksex = sext.translate("task", "SQL", dupKeyEx);
		assertTrue("Not instance of DataIntegrityViolationException",
				DataIntegrityViolationException.class.isAssignableFrom(dksex.getClass()));

		// Test fallback. We assume that no database will ever return this error code,
		// but 07xxx will be bad grammar picked up by the fallback SQLState translator
		SQLException sex = new SQLException("", "07xxx", 666666666);
		BadSqlGrammarException bsgex2 = (BadSqlGrammarException) sext.translate("task", "SQL2", sex);
		assertEquals("SQL2", bsgex2.getSql());
		assertEquals(sex, bsgex2.getSQLException());
	}

	private void checkTranslation(SQLExceptionTranslator sext, int errorCode, Class<?> exClass) {
		SQLException sex = new SQLException("", "", errorCode);
		DataAccessException ex = sext.translate("", "", sex);
		assertTrue(exClass.isInstance(ex));
		assertTrue(ex.getCause() == sex);
	}

	@Test
	public void batchExceptionTranslation() {
		SQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);

		SQLException badSqlEx = new SQLException("", "", 1);
		BatchUpdateException batchUpdateEx = new BatchUpdateException();
		batchUpdateEx.setNextException(badSqlEx);
		BadSqlGrammarException bsgex = (BadSqlGrammarException) sext.translate("task", "SQL", batchUpdateEx);
		assertEquals("SQL", bsgex.getSql());
		assertEquals(badSqlEx, bsgex.getSQLException());
	}

	@Test
	public void dataTruncationTranslation() {
		SQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);

		SQLException dataAccessEx = new SQLException("", "", 5);
		DataTruncation dataTruncation = new DataTruncation(1, true, true, 1, 1, dataAccessEx);
		DataAccessResourceFailureException daex = (DataAccessResourceFailureException) sext.translate("task", "SQL", dataTruncation);
		assertEquals(dataTruncation, daex.getCause());
	}

	@SuppressWarnings("serial")
	@Test
	public void customTranslateMethodTranslation() {
		final String TASK = "TASK";
		final String SQL = "SQL SELECT *";
		final DataAccessException customDex = new DataAccessException("") {};

		final SQLException badSqlEx = new SQLException("", "", 1);
		SQLException intVioEx = new SQLException("", "", 6);

		SQLErrorCodeSQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator() {
			@Override
			@Nullable
			protected DataAccessException customTranslate(String task, @Nullable String sql, SQLException sqlex) {
				assertEquals(TASK, task);
				assertEquals(SQL, sql);
				return (sqlex == badSqlEx) ? customDex : null;
			}
		};
		sext.setSqlErrorCodes(ERROR_CODES);

		// Shouldn't custom translate this
		assertEquals(customDex, sext.translate(TASK, SQL, badSqlEx));
		DataIntegrityViolationException diex = (DataIntegrityViolationException) sext.translate(TASK, SQL, intVioEx);
		assertEquals(intVioEx, diex.getCause());
	}

	@Test
	public void customExceptionTranslation() {
		final String TASK = "TASK";
		final String SQL = "SQL SELECT *";
		final SQLErrorCodes customErrorCodes = new SQLErrorCodes();
		final CustomSQLErrorCodesTranslation customTranslation = new CustomSQLErrorCodesTranslation();

		customErrorCodes.setBadSqlGrammarCodes(new String[] {"1", "2"});
		customErrorCodes.setDataIntegrityViolationCodes(new String[] {"3", "4"});
		customTranslation.setErrorCodes(new String[] {"1"});
		customTranslation.setExceptionClass(CustomErrorCodeException.class);
		customErrorCodes.setCustomTranslations(new CustomSQLErrorCodesTranslation[] {customTranslation});

		SQLErrorCodeSQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator();
		sext.setSqlErrorCodes(customErrorCodes);

		// Should custom translate this
		SQLException badSqlEx = new SQLException("", "", 1);
		assertEquals(CustomErrorCodeException.class, sext.translate(TASK, SQL, badSqlEx).getClass());
		assertEquals(badSqlEx, sext.translate(TASK, SQL, badSqlEx).getCause());

		// Shouldn't custom translate this
		SQLException invResEx = new SQLException("", "", 3);
		DataIntegrityViolationException diex = (DataIntegrityViolationException) sext.translate(TASK, SQL, invResEx);
		assertEquals(invResEx, diex.getCause());

		// Shouldn't custom translate this - invalid class
		exception.expect(IllegalArgumentException.class);
		customTranslation.setExceptionClass(String.class);
	}

}
