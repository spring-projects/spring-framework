/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Test;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for custom SQLException translation.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 */
public class SQLExceptionCustomTranslatorTests {

	private static SQLErrorCodes ERROR_CODES = new SQLErrorCodes();

	static {
		ERROR_CODES.setBadSqlGrammarCodes(new String[] { "1" });
		ERROR_CODES.setDataAccessResourceFailureCodes(new String[] { "2" });
		ERROR_CODES.setCustomSqlExceptionTranslatorClass(CustomSqlExceptionTranslator.class);
	}

	private final SQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);


	@Test
	public void badSqlGrammarException() {
		SQLException badSqlGrammarExceptionEx = SQLExceptionSubclassFactory.newSQLDataException("", "", 1);
		DataAccessException dae = sext.translate("task", "SQL", badSqlGrammarExceptionEx);
		assertEquals(badSqlGrammarExceptionEx, dae.getCause());
		assertThat(dae, instanceOf(BadSqlGrammarException.class));
	}

	@Test
	public void dataAccessResourceException() {
		SQLException dataAccessResourceEx = SQLExceptionSubclassFactory.newSQLDataException("", "", 2);
		DataAccessException dae = sext.translate("task", "SQL", dataAccessResourceEx);
		assertEquals(dataAccessResourceEx, dae.getCause());
		assertThat(dae, instanceOf(TransientDataAccessResourceException.class));
	}

}
