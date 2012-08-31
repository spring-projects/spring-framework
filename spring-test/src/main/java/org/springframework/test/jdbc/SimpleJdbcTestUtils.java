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
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * A Java-5-based collection of JDBC related utility functions intended to
 * simplify standard database testing scenarios.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since 2.5
 * @deprecated as of Spring 3.1.3; use {@link JdbcTestUtils} instead.
 */
@Deprecated
public abstract class SimpleJdbcTestUtils {

	private static final Log logger = LogFactory.getLog(SimpleJdbcTestUtils.class);


	/**
	 * Count the rows in the given table.
	 * @param simpleJdbcTemplate the SimpleJdbcTemplate with which to perform JDBC operations
	 * @param tableName table name to count rows in
	 * @return the number of rows in the table
	 */
	public static int countRowsInTable(SimpleJdbcTemplate simpleJdbcTemplate, String tableName) {
		return simpleJdbcTemplate.queryForInt("SELECT COUNT(0) FROM " + tableName);
	}

	/**
	 * Delete all rows from the specified tables.
	 * @param simpleJdbcTemplate the SimpleJdbcTemplate with which to perform JDBC operations
	 * @param tableNames the names of the tables from which to delete
	 * @return the total number of rows deleted from all specified tables
	 */
	public static int deleteFromTables(SimpleJdbcTemplate simpleJdbcTemplate, String... tableNames) {
		int totalRowCount = 0;
		for (String tableName : tableNames) {
			int rowCount = simpleJdbcTemplate.update("DELETE FROM " + tableName);
			totalRowCount += rowCount;
			if (logger.isInfoEnabled()) {
				logger.info("Deleted " + rowCount + " rows from table " + tableName);
			}
		}
		return totalRowCount;
	}

	/**
	 * Execute the given SQL script.
	 * <p>The script will normally be loaded by classpath. There should be one statement
	 * per line. Any semicolons will be removed. <b>Do not use this method to execute
	 * DDL if you expect rollback.</b>
	 * @param simpleJdbcTemplate the SimpleJdbcTemplate with which to perform JDBC operations
	 * @param resourceLoader the resource loader (with which to load the SQL script
	 * @param sqlResourcePath the Spring resource path for the SQL script
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error
	 * @throws DataAccessException if there is an error executing a statement
	 * and continueOnError was <code>false</code>
	 */
	public static void executeSqlScript(SimpleJdbcTemplate simpleJdbcTemplate, ResourceLoader resourceLoader,
			String sqlResourcePath, boolean continueOnError) throws DataAccessException {

		Resource resource = resourceLoader.getResource(sqlResourcePath);
		executeSqlScript(simpleJdbcTemplate, resource, continueOnError);
	}

	/**
	 * Execute the given SQL script. The script will normally be loaded by classpath.
	 * <p>Statements should be delimited with a semicolon.  If statements are not delimited with
	 * a semicolon then there should be one statement per line.  Statements are allowed to span
	 * lines only if they are delimited with a semicolon.
	 * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
	 * @param simpleJdbcTemplate the SimpleJdbcTemplate with which to perform JDBC operations
	 * @param resource the resource to load the SQL script from.
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error.
	 * @throws DataAccessException if there is an error executing a statement
	 * and continueOnError was <code>false</code>
	 */
	public static void executeSqlScript(SimpleJdbcTemplate simpleJdbcTemplate, Resource resource,
			boolean continueOnError) throws DataAccessException {

		executeSqlScript(simpleJdbcTemplate, new EncodedResource(resource), continueOnError);
	}

	/**
	 * Execute the given SQL script.
	 * <p>The script will normally be loaded by classpath. There should be one statement
	 * per line. Any semicolons will be removed. <b>Do not use this method to execute
	 * DDL if you expect rollback.</b>
	 * @param simpleJdbcTemplate the SimpleJdbcTemplate with which to perform JDBC operations
	 * @param resource the resource (potentially associated with a specific encoding)
	 * to load the SQL script from.
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error.
	 * @throws DataAccessException if there is an error executing a statement
	 * and continueOnError was <code>false</code>
	 */
	public static void executeSqlScript(SimpleJdbcTemplate simpleJdbcTemplate, EncodedResource resource,
			boolean continueOnError) throws DataAccessException {

		if (logger.isInfoEnabled()) {
			logger.info("Executing SQL script from " + resource);
		}

		long startTime = System.currentTimeMillis();
		List<String> statements = new LinkedList<String>();
		LineNumberReader reader = null;
		try {
			reader = new LineNumberReader(resource.getReader());
			String script = JdbcTestUtils.readScript(reader);
			char delimiter = ';';
			if (!JdbcTestUtils.containsSqlScriptDelimiters(script, delimiter)) {
				delimiter = '\n';
			}
			JdbcTestUtils.splitSqlScript(script, delimiter, statements);
			for (String statement : statements) {
				try {
					int rowsAffected = simpleJdbcTemplate.update(statement);
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
