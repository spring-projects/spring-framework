/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.StringUtils;

/**
 * {@code JdbcTestUtils} is a collection of JDBC related utility functions
 * intended to simplify standard database testing scenarios.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Chris Baldwin
 * @since 2.5.4
 */
public class JdbcTestUtils {

	private static final Log logger = LogFactory.getLog(JdbcTestUtils.class);


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
	public static int deleteFromTableWhere(JdbcTemplate jdbcTemplate, String tableName, String whereClause,
			Object... args) {
		String sql = "DELETE FROM " + tableName;
		if (StringUtils.hasText(whereClause)) {
			sql += " WHERE " + whereClause;
		}
		int rowCount = (args != null && args.length > 0 ? jdbcTemplate.update(sql, args) : jdbcTemplate.update(sql));
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
	 * @see DatabasePopulatorUtils
	 * @see #executeSqlScript(JdbcTemplate, Resource, boolean)
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#executeSqlScript}
	 * or {@link org.springframework.jdbc.datasource.init.ResourceDatabasePopulator}.
	 */
	@Deprecated
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
	 * @see DatabasePopulatorUtils
	 * @see #executeSqlScript(JdbcTemplate, EncodedResource, boolean)
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#executeSqlScript}
	 * or {@link org.springframework.jdbc.datasource.init.ResourceDatabasePopulator}.
	 */
	@Deprecated
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
	 * @see DatabasePopulatorUtils
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#executeSqlScript}
	 * or {@link org.springframework.jdbc.datasource.init.ResourceDatabasePopulator}.
	 */
	@Deprecated
	public static void executeSqlScript(JdbcTemplate jdbcTemplate, EncodedResource resource, boolean continueOnError)
			throws DataAccessException {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.setContinueOnError(continueOnError);
		databasePopulator.addScript(resource.getResource());
		databasePopulator.setSqlScriptEncoding(resource.getEncoding());

		DatabasePopulatorUtils.execute(databasePopulator, jdbcTemplate.getDataSource());
	}

	/**
	 * Read a script from the provided {@code LineNumberReader}, using
	 * "{@code --}" as the comment prefix, and build a {@code String} containing
	 * the lines.
	 * @param lineNumberReader the {@code LineNumberReader} containing the script
	 * to be processed
	 * @return a {@code String} containing the script lines
	 * @see #readScript(LineNumberReader, String)
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#readScript(LineNumberReader, String, String)}
	 */
	@Deprecated
	public static String readScript(LineNumberReader lineNumberReader) throws IOException {
		return readScript(lineNumberReader, ScriptUtils.DEFAULT_COMMENT_PREFIX);
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
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#readScript(LineNumberReader, String, String)} 
	 */
	@Deprecated
	public static String readScript(LineNumberReader lineNumberReader, String commentPrefix) throws IOException {
		return ScriptUtils.readScript(lineNumberReader, commentPrefix, ScriptUtils.DEFAULT_STATEMENT_SEPARATOR);
	}

	/**
	 * Determine if the provided SQL script contains the specified delimiter.
	 * @param script the SQL script
	 * @param delim character delimiting each statement &mdash; typically a ';' character
	 * @return {@code true} if the script contains the delimiter; {@code false} otherwise
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#containsSqlScriptDelimiters}
	 */
	@Deprecated
	public static boolean containsSqlScriptDelimiters(String script, char delim) {
		return ScriptUtils.containsSqlScriptDelimiters(script, String.valueOf(delim));
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
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#splitSqlScript(String, char, List)} 
	 */
	@Deprecated
	public static void splitSqlScript(String script, char delim, List<String> statements) {
		ScriptUtils.splitSqlScript(script, delim, statements);
	}
}
