/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class SQLStateSQLExceptionTranslatorTests {

	@Test
	public void translateNullException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new SQLStateSQLExceptionTranslator().translate("", "", null));
	}

	@Test
	public void translateBadSqlGrammar() {
		doTest("07", BadSqlGrammarException.class);
	}

	@Test
	public void translateDataIntegrityViolation() {
		doTest("23", DataIntegrityViolationException.class);
	}

	@Test
	public void translateDataAccessResourceFailure() {
		doTest("53", DataAccessResourceFailureException.class);
	}

	@Test
	public void translateTransientDataAccessResourceFailure() {
		doTest("S1", TransientDataAccessResourceException.class);
	}

	@Test
	public void translateConcurrencyFailure() {
		doTest("40", ConcurrencyFailureException.class);
	}

	@Test
	public void translateUncategorized() {
		doTest("00000000", null);
	}

	@Test
	public void invalidSqlStateCode() {
		doTest("NO SUCH CODE", null);
	}

	/**
	 * PostgreSQL can return null.
	 * SAP DB can apparently return empty SQL code.
	 * Bug 729170
	 */
	@Test
	public void malformedSqlStateCodes() {
		doTest(null, null);
		doTest("", null);
		doTest("I", null);
	}


	private void doTest(@Nullable String sqlState, @Nullable Class<?> dataAccessExceptionType) {
		SQLExceptionTranslator translator = new SQLStateSQLExceptionTranslator();
		SQLException ex = new SQLException("reason", sqlState);
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
