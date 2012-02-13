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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;

import static org.junit.Assert.*;

/**
 * Tests for custom translator.
 *
 * @author Thomas Risberg
 */
public class CustomSQLExceptionTranslatorRegistrarTests {

	@Before
	public void setUp() {
		new ClassPathXmlApplicationContext("test-custom-translators-context.xml",
				CustomSQLExceptionTranslatorRegistrarTests.class);
	}

	@Test
	public void testCustomErrorCodeTranslation() {

		SQLErrorCodes codes = SQLErrorCodesFactory.getInstance().getErrorCodes("H2");
		SQLErrorCodeSQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator();
		sext.setSqlErrorCodes(codes);

		DataAccessException exFor4200 = sext.doTranslate("", "", new SQLException("Ouch", "42000", 42000));
		assertNotNull("Should have been translated", exFor4200);
		assertTrue("Should have been instance of BadSqlGrammarException",
				BadSqlGrammarException.class.isAssignableFrom(exFor4200.getClass()));

		DataAccessException exFor2 = sext.doTranslate("", "", new SQLException("Ouch", "42000", 2));
		assertNotNull("Should have been translated", exFor2);
		assertTrue("Should have been instance of TransientDataAccessResourceException",
				TransientDataAccessResourceException.class.isAssignableFrom(exFor2.getClass()));

		DataAccessException exFor3 = sext.doTranslate("", "", new SQLException("Ouch", "42000", 3));
		assertNull("Should not have been translated", exFor3);
	}

}
