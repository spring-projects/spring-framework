/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jdbc.datasource.init;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_STATEMENT_SEPARATOR;
import static org.springframework.jdbc.datasource.init.ScriptUtils.containsSqlScriptDelimiters;
import static org.springframework.jdbc.datasource.init.ScriptUtils.splitSqlScript;

/**
 * Unit tests for {@link ScriptUtils}.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author Phillip Webb
 * @author Chris Baldwin
 * @author Nicolas Debeissat
 * @since 4.0.3
 * @see ScriptUtilsIntegrationTests
 */
public class ScriptUtilsUnitTests {

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
		List<String> statements = new ArrayList<>();
		splitSqlScript(script, delim, statements);
		assertThat(statements.size()).as("wrong number of statements").isEqualTo(3);
		assertThat(statements.get(0)).as("statement 1 not split correctly").isEqualTo(cleanedStatement1);
		assertThat(statements.get(1)).as("statement 2 not split correctly").isEqualTo(cleanedStatement2);
		assertThat(statements.get(2)).as("statement 3 not split correctly").isEqualTo(cleanedStatement3);
	}

	@Test
	public void splitSqlScriptDelimitedWithNewLine() {
		String statement1 = "insert into customer (id, name) values (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
		String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String statement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		char delim = '\n';
		String script = statement1 + delim + statement2 + delim + statement3 + delim;
		List<String> statements = new ArrayList<>();
		splitSqlScript(script, delim, statements);
		assertThat(statements.size()).as("wrong number of statements").isEqualTo(3);
		assertThat(statements.get(0)).as("statement 1 not split correctly").isEqualTo(statement1);
		assertThat(statements.get(1)).as("statement 2 not split correctly").isEqualTo(statement2);
		assertThat(statements.get(2)).as("statement 3 not split correctly").isEqualTo(statement3);
	}

	@Test
	public void splitSqlScriptDelimitedWithNewLineButDefaultDelimiterSpecified() {
		String statement1 = "do something";
		String statement2 = "do something else";
		char delim = '\n';
		String script = statement1 + delim + statement2 + delim;
		List<String> statements = new ArrayList<>();
		splitSqlScript(script, DEFAULT_STATEMENT_SEPARATOR, statements);
		assertThat(statements.size()).as("wrong number of statements").isEqualTo(1);
		assertThat(statements.get(0)).as("script should have been 'stripped' but not actually 'split'").isEqualTo(script.replace('\n', ' '));
	}

	@Test  // SPR-13218
	public void splitScriptWithSingleQuotesNestedInsideDoubleQuotes() throws Exception {
		String statement1 = "select '1' as \"Dogbert's owner's\" from dual";
		String statement2 = "select '2' as \"Dilbert's\" from dual";
		char delim = ';';
		String script = statement1 + delim + statement2 + delim;
		List<String> statements = new ArrayList<>();
		splitSqlScript(script, ';', statements);
		assertThat(statements.size()).as("wrong number of statements").isEqualTo(2);
		assertThat(statements.get(0)).as("statement 1 not split correctly").isEqualTo(statement1);
		assertThat(statements.get(1)).as("statement 2 not split correctly").isEqualTo(statement2);
	}

	@Test  // SPR-11560
	public void readAndSplitScriptWithMultipleNewlinesAsSeparator() throws Exception {
		String script = readScript("db-test-data-multi-newline.sql");
		List<String> statements = new ArrayList<>();
		splitSqlScript(script, "\n\n", statements);

		String statement1 = "insert into T_TEST (NAME) values ('Keith')";
		String statement2 = "insert into T_TEST (NAME) values ('Dave')";

		assertThat(statements.size()).as("wrong number of statements").isEqualTo(2);
		assertThat(statements.get(0)).as("statement 1 not split correctly").isEqualTo(statement1);
		assertThat(statements.get(1)).as("statement 2 not split correctly").isEqualTo(statement2);
	}

	@Test
	public void readAndSplitScriptContainingComments() throws Exception {
		String script = readScript("test-data-with-comments.sql");
		splitScriptContainingComments(script);
	}

	@Test
	public void readAndSplitScriptContainingCommentsWithWindowsLineEnding() throws Exception {
		String script = readScript("test-data-with-comments.sql").replaceAll("\n", "\r\n");
		splitScriptContainingComments(script);
	}

	private void splitScriptContainingComments(String script) throws Exception {
		List<String> statements = new ArrayList<>();
		splitSqlScript(script, ';', statements);

		String statement1 = "insert into customer (id, name) values (1, 'Rod; Johnson'), (2, 'Adrian Collier')";
		String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String statement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		// Statement 4 addresses the error described in SPR-9982.
		String statement4 = "INSERT INTO persons( person_id , name) VALUES( 1 , 'Name' )";

		assertThat(statements.size()).as("wrong number of statements").isEqualTo(4);
		assertThat(statements.get(0)).as("statement 1 not split correctly").isEqualTo(statement1);
		assertThat(statements.get(1)).as("statement 2 not split correctly").isEqualTo(statement2);
		assertThat(statements.get(2)).as("statement 3 not split correctly").isEqualTo(statement3);
		assertThat(statements.get(3)).as("statement 4 not split correctly").isEqualTo(statement4);
	}

	@Test  // SPR-10330
	public void readAndSplitScriptContainingCommentsWithLeadingTabs() throws Exception {
		String script = readScript("test-data-with-comments-and-leading-tabs.sql");
		List<String> statements = new ArrayList<>();
		splitSqlScript(script, ';', statements);

		String statement1 = "insert into customer (id, name) values (1, 'Sam Brannen')";
		String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2013-06-08', 1)";
		String statement3 = "insert into orders(id, order_date, customer_id) values (2, '2013-06-08', 1)";

		assertThat(statements.size()).as("wrong number of statements").isEqualTo(3);
		assertThat(statements.get(0)).as("statement 1 not split correctly").isEqualTo(statement1);
		assertThat(statements.get(1)).as("statement 2 not split correctly").isEqualTo(statement2);
		assertThat(statements.get(2)).as("statement 3 not split correctly").isEqualTo(statement3);
	}

	@Test  // SPR-9531
	public void readAndSplitScriptContainingMultiLineComments() throws Exception {
		String script = readScript("test-data-with-multi-line-comments.sql");
		List<String> statements = new ArrayList<>();
		splitSqlScript(script, ';', statements);

		String statement1 = "INSERT INTO users(first_name, last_name) VALUES('Juergen', 'Hoeller')";
		String statement2 = "INSERT INTO users(first_name, last_name) VALUES( 'Sam' , 'Brannen' )";

		assertThat(statements.size()).as("wrong number of statements").isEqualTo(2);
		assertThat(statements.get(0)).as("statement 1 not split correctly").isEqualTo(statement1);
		assertThat(statements.get(1)).as("statement 2 not split correctly").isEqualTo(statement2);
	}

	@Test
	public void readAndSplitScriptContainingMultiLineNestedComments() throws Exception {
		String script = readScript("test-data-with-multi-line-nested-comments.sql");
		List<String> statements = new ArrayList<>();
		splitSqlScript(script, ';', statements);

		String statement1 = "INSERT INTO users(first_name, last_name) VALUES('Juergen', 'Hoeller')";
		String statement2 = "INSERT INTO users(first_name, last_name) VALUES( 'Sam' , 'Brannen' )";

		assertThat(statements.size()).as("wrong number of statements").isEqualTo(2);
		assertThat(statements.get(0)).as("statement 1 not split correctly").isEqualTo(statement1);
		assertThat(statements.get(1)).as("statement 2 not split correctly").isEqualTo(statement2);
	}

	@Test
	public void containsDelimiters() {
		assertThat(containsSqlScriptDelimiters("select 1\n select ';'", ";")).isFalse();
		assertThat(containsSqlScriptDelimiters("select 1; select 2", ";")).isTrue();

		assertThat(containsSqlScriptDelimiters("select 1; select '\\n\n';", "\n")).isFalse();
		assertThat(containsSqlScriptDelimiters("select 1\n select 2", "\n")).isTrue();

		assertThat(containsSqlScriptDelimiters("select 1\n select 2", "\n\n")).isFalse();
		assertThat(containsSqlScriptDelimiters("select 1\n\n select 2", "\n\n")).isTrue();

		// MySQL style escapes '\\'
		assertThat(containsSqlScriptDelimiters("insert into users(first_name, last_name)\nvalues('a\\\\', 'b;')", ";")).isFalse();
		assertThat(containsSqlScriptDelimiters("insert into users(first_name, last_name)\nvalues('Charles', 'd\\'Artagnan'); select 1;", ";")).isTrue();
	}

	private String readScript(String path) throws Exception {
		EncodedResource resource = new EncodedResource(new ClassPathResource(path, getClass()));
		return ScriptUtils.readScript(resource);
	}

}
