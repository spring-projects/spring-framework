/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.List;

import org.assertj.core.util.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.support.EncodedResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.r2dbc.connection.init.ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER;
import static org.springframework.r2dbc.connection.init.ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER;
import static org.springframework.r2dbc.connection.init.ScriptUtils.DEFAULT_COMMENT_PREFIXES;
import static org.springframework.r2dbc.connection.init.ScriptUtils.DEFAULT_STATEMENT_SEPARATOR;

/**
 * Tests for {@link ScriptUtils}.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author Phillip Webb
 * @author Chris Baldwin
 * @author Nicolas Debeissat
 * @author Mark Paluch
 * @since 5.3
 */
class ScriptUtilsTests {

	@Test
	void splitSqlScriptDelimitedWithSemicolon() {
		String rawStatement1 = "insert into customer (id, name)\nvalues (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
		String cleanedStatement1 = "insert into customer (id, name) values (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
		String rawStatement2 = "insert into orders(id, order_date, customer_id)\nvalues (1, '2008-01-02', 2)";
		String cleanedStatement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String rawStatement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String cleanedStatement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";

		String delimiter = ";";
		String script = Strings.join(rawStatement1, rawStatement2, rawStatement3).with(delimiter);

		List<String> statements = splitSqlScript(script, delimiter);

		assertThat(statements).containsExactly(cleanedStatement1, cleanedStatement2, cleanedStatement3);
	}

	@Test
	void splitSqlScriptDelimitedWithNewLine() {
		String statement1 = "insert into customer (id, name) values (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
		String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String statement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";

		String delimiter = "\n";
		String script = Strings.join(statement1, statement2, statement3).with(delimiter);

		List<String> statements = splitSqlScript(script, delimiter);

		assertThat(statements).containsExactly(statement1, statement2, statement3);
	}

	@Test
	void splitSqlScriptDelimitedWithNewLineButDefaultDelimiterSpecified() {
		String statement1 = "do something";
		String statement2 = "do something else";

		String script = Strings.join(statement1, statement2).with("\n");

		List<String> statements = splitSqlScript(script, DEFAULT_STATEMENT_SEPARATOR);

		assertThat(statements).as("stripped but not split statements").containsExactly(script.replace('\n', ' '));
	}

	@Test  // SPR-13218
	public void splitScriptWithSingleQuotesNestedInsideDoubleQuotes() {
		String statement1 = "select '1' as \"Dogbert's owner's\" from dual";
		String statement2 = "select '2' as \"Dilbert's\" from dual";

		String delimiter = ";";
		String script = Strings.join(statement1, statement2).with(delimiter);

		List<String> statements = splitSqlScript(script, delimiter);

		assertThat(statements).containsExactly(statement1, statement2);
	}

	@Test  // SPR-11560
	public void readAndSplitScriptWithMultipleNewlinesAsSeparator() {
		String script = readScript("db-test-data-multi-newline.sql");
		List<String> statements = splitSqlScript(script, "\n\n");

		String statement1 = "insert into T_TEST (NAME) values ('Keith')";
		String statement2 = "insert into T_TEST (NAME) values ('Dave')";

		assertThat(statements).containsExactly(statement1, statement2);
	}

	@Test
	void readAndSplitScriptContainingComments() {
		String script = readScript("test-data-with-comments.sql");
		splitScriptContainingComments(script, DEFAULT_COMMENT_PREFIXES);
	}

	@Test
	void readAndSplitScriptContainingCommentsWithWindowsLineEnding() {
		String script = readScript("test-data-with-comments.sql").replaceAll("\n", "\r\n");
		splitScriptContainingComments(script, DEFAULT_COMMENT_PREFIXES);
	}

	@Test
	void readAndSplitScriptContainingCommentsWithMultiplePrefixes() {
		String script = readScript("test-data-with-multi-prefix-comments.sql");
		splitScriptContainingComments(script, "--", "#", "^");
	}

	private void splitScriptContainingComments(String script, String... commentPrefixes) {
		List<String> statements = ScriptUtils.splitSqlScript(null, script, ";", commentPrefixes,
				DEFAULT_BLOCK_COMMENT_START_DELIMITER, DEFAULT_BLOCK_COMMENT_END_DELIMITER);

		String statement1 = "insert into customer (id, name) values (1, 'Rod; Johnson'), (2, 'Adrian Collier')";
		String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		String statement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
		// Statement 4 addresses the error described in SPR-9982.
		String statement4 = "INSERT INTO persons( person_id , name) VALUES( 1 , 'Name' )";

		assertThat(statements).containsExactly(statement1, statement2, statement3, statement4);
	}

	@Test  // SPR-10330
	public void readAndSplitScriptContainingCommentsWithLeadingTabs() {
		String script = readScript("test-data-with-comments-and-leading-tabs.sql");
		List<String> statements = splitSqlScript(script, ";");

		String statement1 = "insert into customer (id, name) values (1, 'Sam Brannen')";
		String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2013-06-08', 1)";
		String statement3 = "insert into orders(id, order_date, customer_id) values (2, '2013-06-08', 1)";

		assertThat(statements).containsExactly(statement1, statement2, statement3);
	}

	@Test  // SPR-9531
	public void readAndSplitScriptContainingMultiLineComments() {
		String script = readScript("test-data-with-multi-line-comments.sql");
		List<String> statements = splitSqlScript(script, ";");

		String statement1 = "INSERT INTO users(first_name, last_name) VALUES('Juergen', 'Hoeller')";
		String statement2 = "INSERT INTO users(first_name, last_name) VALUES( 'Sam' , 'Brannen' )";

		assertThat(statements).containsExactly(statement1, statement2);
	}

	@Test
	void readAndSplitScriptContainingMultiLineNestedComments() {
		String script = readScript("test-data-with-multi-line-nested-comments.sql");
		List<String> statements = splitSqlScript(script, ";");

		String statement1 = "INSERT INTO users(first_name, last_name) VALUES('Juergen', 'Hoeller')";
		String statement2 = "INSERT INTO users(first_name, last_name) VALUES( 'Sam' , 'Brannen' )";

		assertThat(statements).containsExactly(statement1, statement2);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', quoteCharacter = '~', textBlock = """
		# semicolon
		~select 1\n select ';'~                                                            | ;      | false
		~select 1\n select ";"~                                                            | ;      | false
		~select 1; select 2~                                                               | ;      | true

		# newline
		~select 1; select '\n'~                                                            | ~\n~   | false
		~select 1; select "\n"~                                                            | ~\n~   | false
		~select 1\n select 2~                                                              | ~\n~   | true

		# double newline
		~select 1\n select 2~                                                              | ~\n\n~ | false
		~select 1\n\n select 2~                                                            | ~\n\n~ | true

		# semicolon with MySQL style escapes '\\'
		~insert into users(first, last)\n values('a\\\\', 'b;')~                           | ;      | false
		~insert into users(first, last)\n values('Charles', 'd\\'Artagnan'); select 1~     | ;      | true

		# semicolon inside comments
		~-- a;b;c\n insert into colors(color_num) values(42);~                             | ;      | true
		~/* a;b;c */\n insert into colors(color_num) values(42);~                          | ;      | true
		~-- a;b;c\n insert into colors(color_num) values(42)~                              | ;      | false
		~/* a;b;c */\n insert into colors(color_num) values(42)~                           | ;      | false

		# single quotes inside comments
		~-- What\\'s your favorite color?\n insert into colors(color_num) values(42);~     | ;      | true
		~-- What's your favorite color?\n insert into colors(color_num) values(42);~       | ;      | true
		~/* What\\'s your favorite color? */\n insert into colors(color_num) values(42);~  | ;      | true
		~/* What's your favorite color? */\n insert into colors(color_num) values(42);~    | ;      | true

		# double quotes inside comments
		~-- double " quotes\n insert into colors(color_num) values(42);~                   | ;      | true
		~-- double \\" quotes\n insert into colors(color_num) values(42);~                 | ;      | true
		~/* double " quotes */\n insert into colors(color_num) values(42);~                | ;      | true
		~/* double \\" quotes */\n insert into colors(color_num) values(42);~              | ;      | true
		""")
	public void containsStatementSeparator(String script, String delimiter, boolean expected) {
		boolean contains = ScriptUtils.containsStatementSeparator(null, script, delimiter,
				DEFAULT_COMMENT_PREFIXES, DEFAULT_BLOCK_COMMENT_START_DELIMITER, DEFAULT_BLOCK_COMMENT_END_DELIMITER);

		assertThat(contains).isEqualTo(expected);
	}


	private String readScript(String path) {
		EncodedResource resource = new EncodedResource(new ClassPathResource(path, getClass()));
		return ScriptUtils.readScript(resource, DefaultDataBufferFactory.sharedInstance, DEFAULT_STATEMENT_SEPARATOR).block();
	}

	private static List<String> splitSqlScript(String script, String separator) throws ScriptException {
		return ScriptUtils.splitSqlScript(null, script, separator, DEFAULT_COMMENT_PREFIXES,
				DEFAULT_BLOCK_COMMENT_START_DELIMITER, DEFAULT_BLOCK_COMMENT_END_DELIMITER);
	}

}
