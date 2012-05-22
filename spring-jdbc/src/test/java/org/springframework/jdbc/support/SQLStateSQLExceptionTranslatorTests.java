/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.junit.Test;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.UncategorizedSQLException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class SQLStateSQLExceptionTranslatorTests {

	private static final String REASON = "The game is afoot!";

	private static final String TASK = "Counting sheep... yawn.";

	private static final String SQL = "select count(0) from t_sheep where over_fence = ... yawn... 1";


	@Test(expected=IllegalArgumentException.class)
	public void testTranslateNullException() throws Exception {
		new SQLStateSQLExceptionTranslator().translate("", "", null);
	}

	@Test
	public void testTranslateBadSqlGrammar() throws Exception {
		doTest("07", BadSqlGrammarException.class);
	}

	@Test
	public void testTranslateDataIntegrityViolation() throws Exception {
		doTest("23", DataIntegrityViolationException.class);
	}

	@Test
	public void testTranslateDataAccessResourceFailure() throws Exception {
		doTest("53", DataAccessResourceFailureException.class);
	}

	@Test
	public void testTranslateTransientDataAccessResourceFailure() throws Exception {
		doTest("S1", TransientDataAccessResourceException.class);
	}

	@Test
	public void testTranslateConcurrencyFailure() throws Exception {
		doTest("40", ConcurrencyFailureException.class);
	}

	@Test
	public void testTranslateUncategorized() throws Exception {
		doTest("00000000", UncategorizedSQLException.class);
	}


	private void doTest(String sqlState, Class<?> dataAccessExceptionType) {
		SQLException ex = new SQLException(REASON, sqlState);
		SQLExceptionTranslator translator = new SQLStateSQLExceptionTranslator();
		DataAccessException dax = translator.translate(TASK, SQL, ex);
		assertNotNull("Translation must *never* result in a null DataAccessException being returned.", dax);
		assertEquals("Wrong DataAccessException type returned as the result of the translation", dataAccessExceptionType, dax.getClass());
		assertNotNull("The original SQLException must be preserved in the translated DataAccessException", dax.getCause());
		assertSame("The exact same original SQLException must be preserved in the translated DataAccessException", ex, dax.getCause());
	}

}
