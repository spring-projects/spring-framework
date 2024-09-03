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

import org.junit.jupiter.api.Test;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SQLStateSQLExceptionTranslator}.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class SQLStateSQLExceptionTranslatorTests {

	private final SQLExceptionTranslator translator = new SQLStateSQLExceptionTranslator();

	@Test
	void translateNullException() {
		assertThatIllegalArgumentException().isThrownBy(() -> translator.translate("", "", null));
	}

	@Test
	void translateBadSqlGrammar() {
		assertTranslation("07", BadSqlGrammarException.class);
	}

	@Test
	void translateDataIntegrityViolation() {
		assertTranslation("23", DataIntegrityViolationException.class);
	}

	@Test
	void translateDuplicateKey() {
		assertTranslation("23505", DuplicateKeyException.class);
	}

	@Test
	void translateDuplicateKeyOracle() {
		assertTranslation("23000", 1, DuplicateKeyException.class);
	}

	@Test
	void translateDuplicateKeyMySQL() {
		assertTranslation("23000", 1062, DuplicateKeyException.class);
	}

	@Test
	void translateDuplicateKeyMSSQL1() {
		assertTranslation("23000", 2601, DuplicateKeyException.class);
	}

	@Test
	void translateDuplicateKeyMSSQL2() {
		assertTranslation("23000", 2627, DuplicateKeyException.class);
	}

	@Test  // gh-31554
	void translateDuplicateKeySapHana() {
		assertTranslation("23000", 301, DuplicateKeyException.class);
	}

	@Test
	void translateDataAccessResourceFailure() {
		assertTranslation("53", DataAccessResourceFailureException.class);
	}

	@Test
	void translateTransientDataAccessResourceFailure() {
		assertTranslation("S1", TransientDataAccessResourceException.class);
	}

	@Test
	void translatePessimisticLockingFailure() {
		assertTranslation("40", PessimisticLockingFailureException.class);
	}

	@Test
	void translateCannotAcquireLock() {
		assertTranslation("40001", CannotAcquireLockException.class);
	}

	@Test
	void translateUncategorized() {
		assertTranslation("00000000", null);
	}

	@Test
	void invalidSqlStateCode() {
		assertTranslation("NO SUCH CODE", null);
	}

	/**
	 * PostgreSQL can return null.
	 * SAP DB can apparently return empty SQL code.
	 * Bug 729170
	 */
	@Test
	void malformedSqlStateCodes() {
		assertTranslation(null, null);
		assertTranslation("", null);
		assertTranslation("I", null);
	}


	private void assertTranslation(@Nullable String sqlState, @Nullable Class<?> dataAccessExceptionType) {
		assertTranslation(sqlState, 0, dataAccessExceptionType);
	}

	private void assertTranslation(@Nullable String sqlState, int errorCode, @Nullable Class<?> dataAccessExceptionType) {
		SQLException ex = new SQLException("reason", sqlState, errorCode);
		DataAccessException dax = translator.translate("task", "SQL", ex);

		if (dataAccessExceptionType == null) {
			assertThat(dax).as("Expected translation to null").isNull();
			return;
		}

		assertThat(dax).as("Specific translation must not result in null").isNotNull();
		assertThat(dax).as("Wrong DataAccessException type returned").isExactlyInstanceOf(dataAccessExceptionType);
		assertThat(dax.getCause()).as("The exact same original SQLException must be preserved").isSameAs(ex);
	}

}
