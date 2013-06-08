/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Unit tests for {@link JdbcTestUtils}.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 2.5.4
 */
@RunWith(MockitoJUnitRunner.class)
public class JdbcTestUtilsTests {

	@Mock
	private JdbcTemplate jdbcTemplate;


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
		String rawStatement1 = "insert into customer (id, name)\nvalues (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
		String cleanedStatement1 = "insert into customer (id, name) values (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
		String rawStatement2 = "insert into orders(id, order_date, customer_id)\nvalues (1, '2008-01-02', 2)";
		String cleanedStatement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String rawStatement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String cleanedStatement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		char delim = ';';
		String script = rawStatement1 + delim + rawStatement2 + delim + rawStatement3 + delim;
		List<String> statements = new ArrayList<String>();
		JdbcTestUtils.splitSqlScript(script, delim, statements);
		assertEquals("wrong number of statements", 3, statements.size());
		assertEquals("statement 1 not split correctly", cleanedStatement1, statements.get(0));
		assertEquals("statement 2 not split correctly", cleanedStatement2, statements.get(1));
		assertEquals("statement 3 not split correctly", cleanedStatement3, statements.get(2));
	}

	@Test
	public void splitSqlScriptDelimitedWithNewLine() {
		String statement1 = "insert into customer (id, name) values (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
		String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String statement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		char delim = '\n';
		String script = statement1 + delim + statement2 + delim + statement3 + delim;
		List<String> statements = new ArrayList<String>();
		JdbcTestUtils.splitSqlScript(script, delim, statements);
		assertEquals("wrong number of statements", 3, statements.size());
		assertEquals("statement 1 not split correctly", statement1, statements.get(0));
		assertEquals("statement 2 not split correctly", statement2, statements.get(1));
		assertEquals("statement 3 not split correctly", statement3, statements.get(2));
	}

	@Test
	public void readAndSplitScriptContainingComments() throws Exception {

		EncodedResource resource = new EncodedResource(new ClassPathResource("test-data-with-comments.sql", getClass()));
		LineNumberReader lineNumberReader = new LineNumberReader(resource.getReader());

		String script = JdbcTestUtils.readScript(lineNumberReader);

		char delim = ';';
		List<String> statements = new ArrayList<String>();
		JdbcTestUtils.splitSqlScript(script, delim, statements);

		String statement1 = "insert into customer (id, name) values (1, 'Rod; Johnson'), (2, 'Adrian Collier')";
		String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String statement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		// Statement 4 addresses the error described in SPR-9982.
		String statement4 = "INSERT INTO persons( person_id , name) VALUES( 1 , 'Name' )";

		assertEquals("wrong number of statements", 4, statements.size());
		assertEquals("statement 1 not split correctly", statement1, statements.get(0));
		assertEquals("statement 2 not split correctly", statement2, statements.get(1));
		assertEquals("statement 3 not split correctly", statement3, statements.get(2));
		assertEquals("statement 4 not split correctly", statement4, statements.get(3));
	}

	/**
	 * See <a href="https://jira.springsource.org/browse/SPR-10330">SPR-10330</a>
	 * @since 4.0
	 */
	@Test
	public void readAndSplitScriptContainingCommentsWithLeadingTabs() throws Exception {

		EncodedResource resource = new EncodedResource(new ClassPathResource(
			"test-data-with-comments-and-leading-tabs.sql", getClass()));
		LineNumberReader lineNumberReader = new LineNumberReader(resource.getReader());

		String script = JdbcTestUtils.readScript(lineNumberReader);

		char delim = ';';
		List<String> statements = new ArrayList<String>();
		JdbcTestUtils.splitSqlScript(script, delim, statements);

		String statement1 = "insert into customer (id, name) values (1, 'Sam Brannen')";
		String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2013-06-08', 1)";
		String statement3 = "insert into orders(id, order_date, customer_id) values (2, '2013-06-08', 1)";

		assertEquals("wrong number of statements", 3, statements.size());
		assertEquals("statement 1 not split correctly", statement1, statements.get(0));
		assertEquals("statement 2 not split correctly", statement2, statements.get(1));
		assertEquals("statement 3 not split correctly", statement3, statements.get(2));
	}

	@Test
	public void deleteWithoutWhereClause() throws Exception {
		given(jdbcTemplate.update("DELETE FROM person")).willReturn(10);
		int deleted = JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "person", null);
		assertThat(deleted, equalTo(10));
	}

	@Test
	public void deleteWithWhereClause() throws Exception {
		given(jdbcTemplate.update("DELETE FROM person WHERE name = 'Bob' and age > 25")).willReturn(10);
		int deleted = JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "person", "name = 'Bob' and age > 25");
		assertThat(deleted, equalTo(10));
	}

	@Test
	public void deleteWithWhereClauseAndArguments() throws Exception {
		given(jdbcTemplate.update("DELETE FROM person WHERE name = ? and age > ?", "Bob", 25)).willReturn(10);
		int deleted = JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "person", "name = ? and age > ?", "Bob", 25);
		assertThat(deleted, equalTo(10));
	}

}
