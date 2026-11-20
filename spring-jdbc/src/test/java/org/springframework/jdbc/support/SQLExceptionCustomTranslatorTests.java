/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support;

import java.sql.SQLDataException;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for custom SQLException translation.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 */
class SQLExceptionCustomTranslatorTests {

	private static SQLErrorCodes ERROR_CODES = new SQLErrorCodes();

	static {
		ERROR_CODES.setBadSqlGrammarCodes("1");
		ERROR_CODES.setDataAccessResourceFailureCodes("2");
		ERROR_CODES.setCustomSqlExceptionTranslatorClass(CustomSqlExceptionTranslator.class);
	}

	private final SQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);


	@Test
	void badSqlGrammarException() {
		SQLException badSqlGrammarExceptionEx = new SQLDataException("", "", 1);
		DataAccessException dae = sext.translate("task", "SQL", badSqlGrammarExceptionEx);
		assertThat(dae.getCause()).isEqualTo(badSqlGrammarExceptionEx);
		assertThat(dae).isInstanceOf(BadSqlGrammarException.class);
	}

	@Test
	void dataAccessResourceException() {
		SQLException dataAccessResourceEx = new SQLDataException("", "", 2);
		DataAccessException dae = sext.translate("task", "SQL", dataAccessResourceEx);
		assertThat(dae.getCause()).isEqualTo(dataAccessResourceEx);
		assertThat(dae).isInstanceOf(TransientDataAccessResourceException.class);
	}

}
