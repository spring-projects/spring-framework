/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLException;

import junit.framework.TestCase;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Test;

/**
 * Class to test custom SQLException translation.
 *
 * @author Thomas Risberg
 */
@RunWith(JUnit4.class)
public class SQLExceptionCustomTranslatorTests extends TestCase {

	private static SQLErrorCodes ERROR_CODES = new SQLErrorCodes();

	static {
		ERROR_CODES.setBadSqlGrammarCodes(new String[] { "1" });
		ERROR_CODES.setDataAccessResourceFailureCodes(new String[] { "2" });
		ERROR_CODES.setCustomSqlExceptionTranslatorClass(CustomSqlExceptionTranslator.class);
	}


	@Test
	public void testCustomErrorCodeTranslation() {

		SQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);

		SQLException dataIntegrityViolationEx = SQLExceptionSubclassFactory.newSQLDataException("", "", 1);
		DataAccessException daeex = sext.translate("task", "SQL", dataIntegrityViolationEx);
		assertEquals(dataIntegrityViolationEx, daeex.getCause());
		assertTrue(daeex instanceof BadSqlGrammarException);

		SQLException dataAccessResourceEx = SQLExceptionSubclassFactory.newSQLDataException("", "", 2);
		DataAccessException darex = sext.translate("task", "SQL", dataAccessResourceEx);
		assertEquals(dataIntegrityViolationEx, daeex.getCause());
		assertTrue(darex instanceof TransientDataAccessResourceException);

	}

}