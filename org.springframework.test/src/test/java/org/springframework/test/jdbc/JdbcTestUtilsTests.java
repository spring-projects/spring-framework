/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.test.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link JdbcTestUtils}.
 * 
 * @author Thomas Risberg
 * @since 2.5.4
 */
public class JdbcTestUtilsTests {

	@Test
	public void containsDelimiters() {
		assertTrue("test with ';' is wrong", !JdbcTestUtils.containsSqlScriptDelimiters("select 1\n select ';'", ';'));
		assertTrue("test with delimiter ; is wrong",
			JdbcTestUtils.containsSqlScriptDelimiters("select 1; select 2", ';'));
		assertTrue("test with '\\n' is wrong",
			!JdbcTestUtils.containsSqlScriptDelimiters("select 1; select '\\n\n';", '\n'));
		assertTrue("test with delimiter \\n is wrong",
			JdbcTestUtils.containsSqlScriptDelimiters("select 1\n select 2", '\n'));
	}

	@Test
	public void splitSqlScriptDelimitedWithSemicolon() {
		String statement1 = "insert into customer (id, name) \n"
				+ "values (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
		String statement2 = "insert into orders(id, order_date, customer_id) \n" + "values (1, '2008-01-02', 2)";
		String statement3 = "insert into orders(id, order_date, customer_id) " + "values (1, '2008-01-02', 2)";
		char delim = ';';
		String script = statement1 + delim + statement2 + delim + statement3;
		List<String> statements = new ArrayList<String>();
		JdbcTestUtils.splitSqlScript(script, delim, statements);
		assertEquals("wrong number of statements", 3, statements.size());
		assertEquals("statement 1 not split correctly", statement1, statements.get(0));
		assertEquals("statement 2 not split correctly", statement2, statements.get(1));
		assertEquals("statement 3 not split correctly", statement3, statements.get(2));
	}

	@Test
	public void splitSqlScriptDelimitedWithNewLine() {
		String statement1 = "insert into customer (id, name) " + "values (1, 'Rod ; Johnson'), (2, 'Adrian ; Collier')";
		String statement2 = "insert into orders(id, order_date, customer_id) " + "values (1, '2008-01-02', 2)";
		String statement3 = "insert into orders(id, order_date, customer_id) " + "values (1, '2008-01-02', 2)";
		char delim = '\n';
		String script = statement1 + delim + statement2 + delim + statement3;
		List<String> statements = new ArrayList<String>();
		JdbcTestUtils.splitSqlScript(script, delim, statements);
		assertEquals("wrong number of statements", 3, statements.size());
		assertEquals("statement 1 not split correctly", statement1, statements.get(0));
		assertEquals("statement 2 not split correctly", statement2, statements.get(1));
		assertEquals("statement 3 not split correctly", statement3, statements.get(2));
	}

}
