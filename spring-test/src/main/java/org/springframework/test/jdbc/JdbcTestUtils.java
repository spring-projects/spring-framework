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
import org.springframework.util.StringUtils;

/**
 * JdbcTestUtils is a collection of JDBC related utility methods for use in unit
 * and integration testing scenarios.
 *
 * @author Thomas Risberg
 * @since 2.5.4
 */
public class JdbcTestUtils {

	private static final Log logger = LogFactory.getLog(JdbcTestUtils.class);

	/**
	 * Read a script from the LineNumberReader and build a String containing the
	 * lines.
	 *
	 * @param lineNumberReader the <code>LineNumberReader</code> containing the
	 * script to be processed
	 * @return <code>String</code> containing the script lines
	 * @throws IOException
	 */
	public static String readScript(LineNumberReader lineNumberReader) throws IOException {
		String currentStatement = lineNumberReader.readLine();
		StringBuilder scriptBuilder = new StringBuilder();
		while (currentStatement != null) {
			if (StringUtils.hasText(currentStatement)) {
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
	 * Does the provided SQL script contain the specified delimiter?
	 *
	 * @param script the SQL script
	 * @param delim character delimiting each statement - typically a ';'
	 * character
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
	 * Split an SQL script into separate statements delimited with the provided
	 * delimiter character. Each individual statement will be added to the
	 * provided <code>List</code>.
	 *
	 * @param script the SQL script
	 * @param delim character delimiting each statement - typically a ';'
	 * character
	 * @param statements the List that will contain the individual statements
	 */
	public static void splitSqlScript(String script, char delim, List<String> statements) {
		StringBuilder sb = new StringBuilder();
		boolean inLiteral = false;
		char[] content = script.toCharArray();
		for (int i = 0; i < script.length(); i++) {
			if (content[i] == '\'') {
				inLiteral = !inLiteral;
			}
			if (content[i] == delim && !inLiteral) {
				if (sb.length() > 0) {
					statements.add(sb.toString());
					sb = new StringBuilder();
				}
			}
			else {
				sb.append(content[i]);
			}
		}
		if (sb.length() > 0) {
			statements.add(sb.toString());
		}
	}

	/**
	 * Count the rows in the given table.
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableName table name to count rows in
	 * @return the number of rows in the table
	 */
	public static int countRowsInTable(JdbcTemplate jdbcTemplate, String tableName) {
		return countRowsInTable(jdbcTemplate, tableName, null);
	}

	/**
	 * Count the rows in the given table.
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableName table name to count rows in
	 * @param whereClause where clause applied to count query
	 * @return the number of rows in the table
	 * @since 3.2
	 */
	public static int countRowsInTable(JdbcTemplate jdbcTemplate, String tableName,
			String whereClause) {

		String sql = "SELECT COUNT(0) FROM " + tableName;

		if (StringUtils.hasText(whereClause)) {
			sql += " WHERE " + whereClause;
		}

		return jdbcTemplate.queryForInt(sql);
	}

	/**
	 * Delete all rows from the specified tables.
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableNames the names of the tables from which to delete
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
	 * Drop the specified tables.
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableNames the names of the tables to drop
	 * @since 3.2
	 */
	protected void dropTables(JdbcTemplate jdbcTemplate, String... tableNames) {
		for (String tableName : tableNames) {
			jdbcTemplate.execute("DROP TABLE `" + tableName + "`");
			if (logger.isInfoEnabled()) {
				logger.info(tableName + " was dropped");
			}
		}
	}

	/**
	 * Execute the given SQL script.
	 * <p>The script will normally be loaded by classpath. There should be one statement
	 * per line. Any semicolons will be removed. <b>Do not use this method to execute
	 * DDL if you expect rollback.</b>
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param resourceLoader the resource loader (with which to load the SQL script
	 * @param sqlResourcePath the Spring resource path for the SQL script
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error
	 * @throws DataAccessException if there is an error executing a statement
	 * and continueOnError was <code>false</code>
	 */
	public static void executeSqlScript(JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader,
			String sqlResourcePath, boolean continueOnError) throws DataAccessException {

		Resource resource = resourceLoader.getResource(sqlResourcePath);
		executeSqlScript(jdbcTemplate, resource, continueOnError);
	}

	/**
	 * Execute the given SQL script. The script will normally be loaded by classpath.
	 * <p>Statements should be delimited with a semicolon.  If statements are not delimited with
	 * a semicolon then there should be one statement per line.  Statements are allowed to span
	 * lines only if they are delimited with a semicolon.
	 * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param resource the resource to load the SQL script from.
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error.
	 * @throws DataAccessException if there is an error executing a statement
	 * and continueOnError was <code>false</code>
	 */
	public static void executeSqlScript(JdbcTemplate jdbcTemplate, Resource resource,
			boolean continueOnError) throws DataAccessException {

		executeSqlScript(jdbcTemplate, new EncodedResource(resource), continueOnError);
	}

	/**
	 * Execute the given SQL script.
	 * <p>The script will normally be loaded by classpath. There should be one statement
	 * per line. Any semicolons will be removed. <b>Do not use this method to execute
	 * DDL if you expect rollback.</b>
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param resource the resource (potentially associated with a specific encoding)
	 * to load the SQL script from.
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error.
	 * @throws DataAccessException if there is an error executing a statement
	 * and continueOnError was <code>false</code>
	 */
	public static void executeSqlScript(JdbcTemplate jdbcTemplate, EncodedResource resource,
			boolean continueOnError) throws DataAccessException {

		if (logger.isInfoEnabled()) {
			logger.info("Executing SQL script from " + resource);
		}

		long startTime = System.currentTimeMillis();
		List<String> statements = new LinkedList<String>();
		LineNumberReader reader = null;
		try {
			reader = new LineNumberReader(resource.getReader());
			String script = readScript(reader);
			char delimiter = ';';
			if (!containsSqlScriptDelimiters(script, delimiter)) {
				delimiter = '\n';
			}
			splitSqlScript(script, delimiter, statements);
			for (String statement : statements) {
				try {
					int rowsAffected = jdbcTemplate.update(statement);
					if (logger.isDebugEnabled()) {
						logger.debug(rowsAffected + " rows affected by SQL: " + statement);
					}
				}
				catch (DataAccessException ex) {
					if (continueOnError) {
						if (logger.isWarnEnabled()) {
							logger.warn("SQL: " + statement + " failed", ex);
						}
					}
					else {
						throw ex;
					}
				}
			}
			long elapsedTime = System.currentTimeMillis() - startTime;
			if (logger.isInfoEnabled()) {
				logger.info("Done executing SQL scriptBuilder from " + resource + " in " + elapsedTime + " ms.");
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
}
