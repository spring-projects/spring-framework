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

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;

/**
 * {@code JdbcTestUtils} is a collection of JDBC related utility functions
 * intended to simplify standard database testing scenarios.
 *
 * <p>As of Spring 3.1.3, {@code JdbcTestUtils} supersedes {@link SimpleJdbcTestUtils}.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 2.5.4
 */
public class JdbcTestUtils {

	private static final Log logger = LogFactory.getLog(JdbcTestUtils.class);

	private static final String DEFAULT_COMMENT_PREFIX = "--";

	private static final char DEFAULT_STATEMENT_SEPARATOR = ';';


	/**
	 * Count the rows in the given table.
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableName name of the table to count rows in
	 * @return the number of rows in the table
	 */
	public static int countRowsInTable(JdbcTemplate jdbcTemplate, String tableName) {
		return jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + tableName, Integer.class);
	}

	/**
	 * Count the rows in the given table, using the provided {@code WHERE} clause.
	 * <p>If the provided {@code WHERE} clause contains text, it will be prefixed
	 * with {@code " WHERE "} and then appended to the generated {@code SELECT}
	 * statement. For example, if the provided table name is {@code "person"} and
	 * the provided where clause is {@code "name = 'Bob' and age > 25"}, the
	 * resulting SQL statement to execute will be
	 * {@code "SELECT COUNT(0) FROM person WHERE name = 'Bob' and age > 25"}.
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableName the name of the table to count rows in
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @return the number of rows in the table that match the provided
	 * {@code WHERE} clause
	 */
	public static int countRowsInTableWhere(JdbcTemplate jdbcTemplate, String tableName, String whereClause) {
		String sql = "SELECT COUNT(0) FROM " + tableName;
		if (StringUtils.hasText(whereClause)) {
			sql += " WHERE " + whereClause;
		}
		return jdbcTemplate.queryForObject(sql, Integer.class);
	}

	/**
	 * Delete all rows from the specified tables.
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableNames the names of the tables to delete from
	 * @return the total number of rows deleted from all specified tables
	 */
	public static int deleteFromTables(JdbcTemplate jdbcTemplate, String... tableNames) {
		int totalRowCount = 0;
		for (String tableName : tableNames) {
			int rowCount = jdbcTemplate.update("DELETE FROM " + tableName);
			totalRowCount += rowCount;
			if (logger.isInfoEnabled()) {
				logger.info("Deleted " + rowCount + " rows from table " + tableName);
			}
		}
		return totalRowCount;
	}

	/**
	 * Delete rows from the given table, using the provided {@code WHERE} clause.
	 * <p>If the provided {@code WHERE} clause contains text, it will be prefixed
	 * with {@code " WHERE "} and then appended to the generated {@code DELETE}
	 * statement. For example, if the provided table name is {@code "person"} and
	 * the provided where clause is {@code "name = 'Bob' and age > 25"}, the
	 * resulting SQL statement to execute will be
	 * {@code "DELETE FROM person WHERE name = 'Bob' and age > 25"}.
	 * <p>As an alternative to hard-coded values, the {@code "?"} placeholder can
	 * be used within the {@code WHERE} clause, binding to the given arguments.
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableName the name of the table to delete rows from
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @param args arguments to bind to the query (leaving it to the PreparedStatement
	 * to guess the corresponding SQL type); may also contain {@link SqlParameterValue}
	 * objects which indicate not only the argument value but also the SQL type and
	 * optionally the scale.
	 * @return the number of rows deleted from the table
	 */
	public static int deleteFromTableWhere(JdbcTemplate jdbcTemplate, String tableName,
			String whereClause, Object... args) {
		String sql = "DELETE FROM " + tableName;
		if (StringUtils.hasText(whereClause)) {
			sql += " WHERE " + whereClause;
		}
		int rowCount = (args != null && args.length > 0 ? jdbcTemplate.update(sql, args)
				: jdbcTemplate.update(sql));
		if (logger.isInfoEnabled()) {
			logger.info("Deleted " + rowCount + " rows from table " + tableName);
		}
		return rowCount;
	}

	/**
	 * Drop the specified tables.
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableNames the names of the tables to drop
	 */
	public static void dropTables(JdbcTemplate jdbcTemplate, String... tableNames) {
		for (String tableName : tableNames) {
			jdbcTemplate.execute("DROP TABLE " + tableName);
			if (logger.isInfoEnabled()) {
				logger.info("Dropped table " + tableName);
			}
		}
	}

	/**
	 * Execute the given SQL script.
	 * <p>The script will typically be loaded from the classpath. There should
	 * be one statement per line. Any semicolons and line comments will be removed.
	 * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param resourceLoader the resource loader with which to load the SQL script
	 * @param sqlResourcePath the Spring resource path for the SQL script
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error
	 * @throws DataAccessException if there is an error executing a statement
	 * and {@code continueOnError} is {@code false}
	 * @see ResourceDatabasePopulator
	 * @see #executeSqlScript(JdbcTemplate, Resource, boolean)
	 */
	public static void executeSqlScript(JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader,
			String sqlResourcePath, boolean continueOnError) throws DataAccessException {
		Resource resource = resourceLoader.getResource(sqlResourcePath);
		executeSqlScript(jdbcTemplate, resource, continueOnError);
	}

	/**
	 * Execute the given SQL script.
	 * <p>The script will typically be loaded from the classpath. Statements
	 * should be delimited with a semicolon. If statements are not delimited with
	 * a semicolon then there should be one statement per line. Statements are
	 * allowed to span lines only if they are delimited with a semicolon. Any
	 * line comments will be removed.
	 * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param resource the resource to load the SQL script from
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error
	 * @throws DataAccessException if there is an error executing a statement
	 * and {@code continueOnError} is {@code false}
	 * @see ResourceDatabasePopulator
	 * @see #executeSqlScript(JdbcTemplate, EncodedResource, boolean)
	 */
	public static void executeSqlScript(JdbcTemplate jdbcTemplate, Resource resource, boolean continueOnError)
			throws DataAccessException {
		executeSqlScript(jdbcTemplate, new EncodedResource(resource), continueOnError);
	}

	/**
	 * Execute the given SQL script.
	 * <p>The script will typically be loaded from the classpath. There should
	 * be one statement per line. Any semicolons and line comments will be removed.
	 * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param resource the resource (potentially associated with a specific encoding)
	 * to load the SQL script from
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error
	 * @throws DataAccessException if there is an error executing a statement
	 * and {@code continueOnError} is {@code false}
	 * @see ResourceDatabasePopulator
	 */
	public static void executeSqlScript(JdbcTemplate jdbcTemplate, EncodedResource resource, boolean continueOnError)
			throws DataAccessException {

		if (logger.isInfoEnabled()) {
			logger.info("Executing SQL script from " + resource);
		}
		long startTime = System.currentTimeMillis();
		List<String> statements = new LinkedList<String>();
		LineNumberReader reader = null;
		try {
			reader = new LineNumberReader(resource.getReader());
			String script = readScript(reader);
			char delimiter = DEFAULT_STATEMENT_SEPARATOR;
			if (!containsSqlScriptDelimiters(script, delimiter)) {
				delimiter = '\n';
			}
			splitSqlScript(script, delimiter, statements);
			int lineNumber = 0;
			for (String statement : statements) {
				lineNumber++;
				try {
					int rowsAffected = jdbcTemplate.update(statement);
					if (logger.isDebugEnabled()) {
						logger.debug(rowsAffected + " rows affected by SQL: " + statement);
					}
				}
				catch (DataAccessException ex) {
					if (continueOnError) {
						if (logger.isWarnEnabled()) {
							logger.warn("Failed to execute SQL script statement at line " + lineNumber
									+ " of resource " + resource + ": " + statement, ex);
						}
					}
					else {
						throw ex;
					}
				}
			}
			long elapsedTime = System.currentTimeMillis() - startTime;
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Executed SQL script from %s in %s ms.", resource, elapsedTime));
			}
		}
		catch (IOException ex) {
			throw new DataAccessResourceFailureException("Failed to open SQL script from " + resource, ex);
		}
		finally {
			try {
				if (reader != null) {
					reader.close();
				}
			}
			catch (IOException ex) {
				// ignore
			}
		}
	}

	/**
	 * Read a script from the provided {@code LineNumberReader}, using
	 * "{@code --}" as the comment prefix, and build a {@code String} containing
	 * the lines.
	 * @param lineNumberReader the {@code LineNumberReader} containing the script
	 * to be processed
	 * @return a {@code String} containing the script lines
	 * @see #readScript(LineNumberReader, String)
	 */
	public static String readScript(LineNumberReader lineNumberReader) throws IOException {
		return readScript(lineNumberReader, DEFAULT_COMMENT_PREFIX);
	}

	/**
	 * Read a script from the provided {@code LineNumberReader}, using the supplied
	 * comment prefix, and build a {@code String} containing the lines.
	 * <p>Lines <em>beginning</em> with the comment prefix are excluded from the
	 * results; however, line comments anywhere else &mdash; for example, within
	 * a statement &mdash; will be included in the results.
	 * @param lineNumberReader the {@code LineNumberReader} containing the script
	 * to be processed
	 * @param commentPrefix the prefix that identifies comments in the SQL script &mdash; typically "--"
	 * @return a {@code String} containing the script lines
	 */
	public static String readScript(LineNumberReader lineNumberReader, String commentPrefix) throws IOException {
		String currentStatement = lineNumberReader.readLine();
		StringBuilder scriptBuilder = new StringBuilder();
		while (currentStatement != null) {
			if (StringUtils.hasText(currentStatement)
					&& (commentPrefix != null && !currentStatement.startsWith(commentPrefix))) {
				if (scriptBuilder.length() > 0) {
					scriptBuilder.append('\n');
				}
				scriptBuilder.append(currentStatement);
			}
			currentStatement = lineNumberReader.readLine();
		}
		return scriptBuilder.toString();
	}

	/**
	 * Determine if the provided SQL script contains the specified delimiter.
	 * @param script the SQL script
	 * @param delim character delimiting each statement &mdash; typically a ';' character
	 * @return {@code true} if the script contains the delimiter; {@code false} otherwise
	 */
	public static boolean containsSqlScriptDelimiters(String script, char delim) {
		boolean inLiteral = false;
		char[] content = script.toCharArray();
		for (int i = 0; i < script.length(); i++) {
			if (content[i] == '\'') {
				inLiteral = !inLiteral;
			}
			if (content[i] == delim && !inLiteral) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Split an SQL script into separate statements delimited by the provided
	 * delimiter character. Each individual statement will be added to the
	 * provided {@code List}.
	 * <p>Within a statement, "{@code --}" will be used as the comment prefix;
	 * any text beginning with the comment prefix and extending to the end of
	 * the line will be omitted from the statement. In addition, multiple adjacent
	 * whitespace characters will be collapsed into a single space.
	 * @param script the SQL script
	 * @param delim character delimiting each statement &mdash; typically a ';' character
	 * @param statements the list that will contain the individual statements
	 */
	public static void splitSqlScript(String script, char delim, List<String> statements) {
		splitSqlScript(script, "" + delim, DEFAULT_COMMENT_PREFIX, statements);
	}

	/**
	 * Split an SQL script into separate statements delimited by the provided
	 * delimiter string. Each individual statement will be added to the provided
	 * {@code List}.
	 * <p>Within a statement, the provided {@code commentPrefix} will be honored;
	 * any text beginning with the comment prefix and extending to the end of the
	 * line will be omitted from the statement. In addition, multiple adjacent
	 * whitespace characters will be collapsed into a single space.
	 * @param script the SQL script
	 * @param delim character delimiting each statement &mdash; typically a ';' character
	 * @param commentPrefix the prefix that identifies line comments in the SQL script &mdash; typically "--"
	 * @param statements the List that will contain the individual statements
	 */
	private static void splitSqlScript(String script, String delim, String commentPrefix, List<String> statements) {
		StringBuilder sb = new StringBuilder();
		boolean inLiteral = false;
		boolean inEscape = false;
		char[] content = script.toCharArray();
		for (int i = 0; i < script.length(); i++) {
			char c = content[i];
			if (inEscape) {
				inEscape = false;
				sb.append(c);
				continue;
			}
			// MySQL style escapes
			if (c == '\\') {
				inEscape = true;
				sb.append(c);
				continue;
			}
			if (c == '\'') {
				inLiteral = !inLiteral;
			}
			if (!inLiteral) {
				if (script.startsWith(delim, i)) {
					// we've reached the end of the current statement
					if (sb.length() > 0) {
						statements.add(sb.toString());
						sb = new StringBuilder();
					}
					i += delim.length() - 1;
					continue;
				}
				else if (script.startsWith(commentPrefix, i)) {
					// skip over any content from the start of the comment to the EOL
					int indexOfNextNewline = script.indexOf("\n", i);
					if (indexOfNextNewline > i) {
						i = indexOfNextNewline;
						continue;
					}
					else {
						// if there's no newline after the comment, we must be at the end
						// of the script, so stop here.
						break;
					}
				}
				else if (c == ' ' || c == '\n' || c == '\t') {
					// avoid multiple adjacent whitespace characters
					if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
						c = ' ';
					}
					else {
						continue;
					}
				}
			}
			sb.append(c);
		}
		if (StringUtils.hasText(sb)) {
			statements.add(sb.toString());
		}
	}

}
