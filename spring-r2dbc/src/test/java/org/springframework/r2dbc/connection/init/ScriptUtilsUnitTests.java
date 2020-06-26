/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.r2dbc.connection.init;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Strings;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.support.EncodedResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ScriptUtils}.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author Phillip Webb
 * @author Chris Baldwin
 * @author Nicolas Debeissat
 * @author Mark Paluch
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

		String script = Strings.join(rawStatement1, rawStatement2, rawStatement3).with(
				";");

		List<String> statements = new ArrayList<>();
		ScriptUtils.splitSqlScript(script, ";", statements);

		assertThat(statements).hasSize(3).containsSequence(cleanedStatement1,
				cleanedStatement2, cleanedStatement3);
	}

	@Test
	public void splitSqlScriptDelimitedWithNewLine() {
		String statement1 = "insert into customer (id, name) values (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
		String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String statement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";

		String script = Strings.join(statement1, statement2, statement3).with("\n");

		List<String> statements = new ArrayList<>();
		ScriptUtils.splitSqlScript(script, "\n", statements);

		assertThat(statements).hasSize(3).containsSequence(statement1, statement2,
				statement3);
	}

	@Test
	public void splitSqlScriptDelimitedWithNewLineButDefaultDelimiterSpecified() {
		String statement1 = "do something";
		String statement2 = "do something else";

		char delim = '\n';
		String script = statement1 + delim + statement2 + delim;

		List<String> statements = new ArrayList<>();

		ScriptUtils.splitSqlScript(script, ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
				statements);

		assertThat(statements).hasSize(1).contains(script.replace('\n', ' '));
	}

	@Test
	public void splitScriptWithSingleQuotesNestedInsideDoubleQuotes() {
		String statement1 = "select '1' as \"Dogbert's owner's\" from dual";
		String statement2 = "select '2' as \"Dilbert's\" from dual";

		char delim = ';';
		String script = statement1 + delim + statement2 + delim;

		List<String> statements = new ArrayList<>();
		ScriptUtils.splitSqlScript(script, ';', statements);

		assertThat(statements).hasSize(2).containsSequence(statement1, statement2);
	}

	@Test
	public void readAndSplitScriptWithMultipleNewlinesAsSeparator() {
		String script = readScript("db-test-data-multi-newline.sql");
		List<String> statements = new ArrayList<>();
		ScriptUtils.splitSqlScript(script, "\n\n", statements);

		String statement1 = "insert into users (last_name) values ('Walter')";
		String statement2 = "insert into users (last_name) values ('Jesse')";

		assertThat(statements.size()).as("wrong number of statements").isEqualTo(2);
		assertThat(statements.get(0)).as("statement 1 not split correctly").isEqualTo(
				statement1);
		assertThat(statements.get(1)).as("statement 2 not split correctly").isEqualTo(
				statement2);
	}

	@Test
	public void readAndSplitScriptContainingComments() {
		String script = readScript("test-data-with-comments.sql");
		splitScriptContainingComments(script);
	}

	@Test
	public void readAndSplitScriptContainingCommentsWithWindowsLineEnding() {
		String script = readScript("test-data-with-comments.sql").replaceAll("\n",
				"\r\n");
		splitScriptContainingComments(script);
	}

	private void splitScriptContainingComments(String script) {
		List<String> statements = new ArrayList<>();
		ScriptUtils.splitSqlScript(script, ';', statements);

		String statement1 = "insert into customer (id, name) values (1, 'Rod; Johnson'), (2, 'Adrian Collier')";
		String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String statement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String statement4 = "INSERT INTO persons( person_id , name) VALUES( 1 , 'Name' )";

		assertThat(statements).hasSize(4).containsSequence(statement1, statement2,
				statement3, statement4);
	}

	@Test
	public void readAndSplitScriptContainingCommentsWithLeadingTabs() {
		String script = readScript("test-data-with-comments-and-leading-tabs.sql");
		List<String> statements = new ArrayList<>();
		ScriptUtils.splitSqlScript(script, ';', statements);

		String statement1 = "insert into customer (id, name) values (1, 'Walter White')";
		String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2013-06-08', 1)";
		String statement3 = "insert into orders(id, order_date, customer_id) values (2, '2013-06-08', 1)";

		assertThat(statements).hasSize(3).containsSequence(statement1, statement2,
				statement3);
	}

	@Test
	public void readAndSplitScriptContainingMultiLineComments() {
		String script = readScript("test-data-with-multi-line-comments.sql");
		List<String> statements = new ArrayList<>();
		ScriptUtils.splitSqlScript(script, ';', statements);

		String statement1 = "INSERT INTO users(first_name, last_name) VALUES('Walter', 'White')";
		String statement2 = "INSERT INTO users(first_name, last_name) VALUES( 'Jesse' , 'Pinkman' )";

		assertThat(statements).hasSize(2).containsSequence(statement1, statement2);
	}

	@Test
	public void readAndSplitScriptContainingMultiLineNestedComments() {
		String script = readScript("test-data-with-multi-line-nested-comments.sql");
		List<String> statements = new ArrayList<>();
		ScriptUtils.splitSqlScript(script, ';', statements);

		String statement1 = "INSERT INTO users(first_name, last_name) VALUES('Walter', 'White')";
		String statement2 = "INSERT INTO users(first_name, last_name) VALUES( 'Jesse' , 'Pinkman' )";

		assertThat(statements).hasSize(2).containsSequence(statement1, statement2);
	}

	@Test
	public void containsDelimiters() {
		assertThat(ScriptUtils.containsSqlScriptDelimiters("select 1\n select ';'",
				";")).isFalse();
		assertThat(ScriptUtils.containsSqlScriptDelimiters("select 1; select 2",
				";")).isTrue();

		assertThat(ScriptUtils.containsSqlScriptDelimiters("select 1; select '\\n\n';",
				"\n")).isFalse();
		assertThat(ScriptUtils.containsSqlScriptDelimiters("select 1\n select 2",
				"\n")).isTrue();

		assertThat(ScriptUtils.containsSqlScriptDelimiters("select 1\n select 2",
				"\n\n")).isFalse();
		assertThat(ScriptUtils.containsSqlScriptDelimiters("select 1\n\n select 2",
				"\n\n")).isTrue();

		// MySQL style escapes '\\'
		assertThat(ScriptUtils.containsSqlScriptDelimiters(
				"insert into users(first_name, last_name)\nvalues('a\\\\', 'b;')",
				";")).isFalse();
		assertThat(ScriptUtils.containsSqlScriptDelimiters(
				"insert into users(first_name, last_name)\nvalues('Charles', 'd\\'Artagnan'); select 1;",
				";")).isTrue();
	}

	private String readScript(String path) {
		EncodedResource resource = new EncodedResource(
				new ClassPathResource(path, getClass()));
		return ScriptUtils.readScript(resource, DefaultDataBufferFactory.sharedInstance).block();
	}

}
