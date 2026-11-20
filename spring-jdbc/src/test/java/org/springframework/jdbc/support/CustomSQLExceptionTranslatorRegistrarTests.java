/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for custom {@link SQLExceptionTranslator}.
 *
 * @author Thomas Risberg
 */
class CustomSQLExceptionTranslatorRegistrarTests {

	@Test
	void customErrorCodeTranslation() {
		new ClassPathXmlApplicationContext("test-custom-translators-context.xml",
				CustomSQLExceptionTranslatorRegistrarTests.class);

		SQLErrorCodes codes = SQLErrorCodesFactory.getInstance().getErrorCodes("H2");
		SQLErrorCodeSQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator();
		sext.setSqlErrorCodes(codes);

		DataAccessException exFor4200 = sext.doTranslate("", "", new SQLException("Ouch", "42000", 42000));
		assertThat(exFor4200).as("Should have been translated").isNotNull();
		assertThat(exFor4200).isInstanceOf(BadSqlGrammarException.class);

		DataAccessException exFor2 = sext.doTranslate("", "", new SQLException("Ouch", "42000", 2));
		assertThat(exFor2).as("Should have been translated").isNotNull();
		assertThat(exFor2).isInstanceOf(TransientDataAccessResourceException.class);

		DataAccessException exFor3 = sext.doTranslate("", "", new SQLException("Ouch", "42000", 3));
		assertThat(exFor3).as("Should not have been translated").isNull();
	}

}
