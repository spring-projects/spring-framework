/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;
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
 * @see org.springframework.jdbc.core.JdbcOperations
 * @see org.springframework.jdbc.core.JdbcTemplate
 * @see org.springframework.jdbc.datasource.init.ScriptUtils
 * @see org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
 * @see org.springframework.jdbc.datasource.init.DatabasePopulatorUtils
 */
public abstract class JdbcTestUtils {

	private static final Log logger = LogFactory.getLog(JdbcTestUtils.class);


	/**
	 * Count the rows in the given table.
	 * @param jdbcTemplate the {@link JdbcOperations} with which to perform JDBC
	 * operations
	 * @param tableName name of the table to count rows in
	 * @return the number of rows in the table
	 */
	public static int countRowsInTable(JdbcOperations jdbcTemplate, String tableName) {
		return countRowsInTable(JdbcClient.create(jdbcTemplate), tableName);
	}

	/**
	 * Count the rows in the given table.
	 * @param jdbcClient the {@link JdbcClient} with which to perform JDBC
	 * operations
	 * @param tableName name of the table to count rows in
	 * @return the number of rows in the table
	 * @since 6.1
	 */
	public static int countRowsInTable(JdbcClient jdbcClient, String tableName) {
		return countRowsInTableWhere(jdbcClient, tableName, null);
	}

	/**
	 * Count the rows in the given table, using the provided {@code WHERE} clause.
	 * <p>If the provided {@code WHERE} clause contains text, it will be prefixed
	 * with {@code " WHERE "} and then appended to the generated {@code SELECT}
	 * statement. For example, if the provided table name is {@code "person"} and
	 * the provided where clause is {@code "name = 'Bob' and age > 25"}, the
	 * resulting SQL statement to execute will be
	 * {@code "SELECT COUNT(0) FROM person WHERE name = 'Bob' and age > 25"}.
	 * @param jdbcTemplate the {@link JdbcOperations} with which to perform JDBC
	 * operations
	 * @param tableName the name of the table to count rows in
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @return the number of rows in the table that match the provided
	 * {@code WHERE} clause
	 */
	public static int countRowsInTableWhere(
			JdbcOperations jdbcTemplate, String tableName, @Nullable String whereClause) {

		return countRowsInTableWhere(JdbcClient.create(jdbcTemplate), tableName, whereClause);
	}

	/**
	 * Count the rows in the given table, using the provided {@code WHERE} clause.
	 * <p>If the provided {@code WHERE} clause contains text, it will be prefixed
	 * with {@code " WHERE "} and then appended to the generated {@code SELECT}
	 * statement. For example, if the provided table name is {@code "person"} and
	 * the provided where clause is {@code "name = 'Bob' and age > 25"}, the
	 * resulting SQL statement to execute will be
	 * {@code "SELECT COUNT(0) FROM person WHERE name = 'Bob' and age > 25"}.
	 * @param jdbcClient the {@link JdbcClient} with which to perform JDBC
	 * operations
	 * @param tableName the name of the table to count rows in
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @return the number of rows in the table that match the provided
	 * {@code WHERE} clause
	 * @since 6.1
	 */
	public static int countRowsInTableWhere(
			JdbcClient jdbcClient, String tableName, @Nullable String whereClause) {

		String sql = "SELECT COUNT(0) FROM " + tableName;
		if (StringUtils.hasText(whereClause)) {
			sql += " WHERE " + whereClause;
		}
		return jdbcClient.sql(sql).query(Integer.class).single();
	}

	/**
	 * Delete all rows from the specified tables.
	 * @param jdbcTemplate the {@link JdbcOperations} with which to perform JDBC
	 * operations
	 * @param tableNames the names of the tables to delete from
	 * @return the total number of rows deleted from all specified tables
	 */
	public static int deleteFromTables(JdbcOperations jdbcTemplate, String... tableNames) {
		return deleteFromTables(JdbcClient.create(jdbcTemplate), tableNames);
	}

	/**
	 * Delete all rows from the specified tables.
	 * @param jdbcClient the {@link JdbcClient} with which to perform JDBC
	 * operations
	 * @param tableNames the names of the tables to delete from
	 * @return the total number of rows deleted from all specified tables
	 * @since 6.1
	 */
	public static int deleteFromTables(JdbcClient jdbcClient, String... tableNames) {
		int totalRowCount = 0;
		for (String tableName : tableNames) {
			int rowCount = jdbcClient.sql("DELETE FROM " + tableName).update();
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
	 * @param jdbcTemplate the {@link JdbcOperations} with which to perform JDBC
	 * operations
	 * @param tableName the name of the table to delete rows from
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @param args arguments to bind to the query (leaving it to the PreparedStatement
	 * to guess the corresponding SQL type); may also contain {@link SqlParameterValue}
	 * objects which indicate not only the argument value but also the SQL type and
	 * optionally the scale.
	 * @return the number of rows deleted from the table
	 */
	public static int deleteFromTableWhere(
			JdbcOperations jdbcTemplate, String tableName, String whereClause, Object... args) {

		return deleteFromTableWhere(JdbcClient.create(jdbcTemplate), tableName, whereClause, args);
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
	 * @param jdbcClient the {@link JdbcClient} with which to perform JDBC
	 * operations
	 * @param tableName the name of the table to delete rows from
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @param args arguments to bind to the query (leaving it to the PreparedStatement
	 * to guess the corresponding SQL type); may also contain {@link SqlParameterValue}
	 * objects which indicate not only the argument value but also the SQL type and
	 * optionally the scale.
	 * @return the number of rows deleted from the table
	 * @since 6.1
	 */
	public static int deleteFromTableWhere(
			JdbcClient jdbcClient, String tableName, String whereClause, Object... args) {

		String sql = "DELETE FROM " + tableName;
		if (StringUtils.hasText(whereClause)) {
			sql += " WHERE " + whereClause;
		}
		int rowCount = jdbcClient.sql(sql).params(args).update();
		if (logger.isInfoEnabled()) {
			logger.info("Deleted " + rowCount + " rows from table " + tableName);
		}
		return rowCount;
	}

	/**
	 * Drop the specified tables.
	 * @param jdbcTemplate the {@link JdbcOperations} with which to perform JDBC
	 * operations
	 * @param tableNames the names of the tables to drop
	 */
	public static void dropTables(JdbcOperations jdbcTemplate, String... tableNames) {
		dropTables(JdbcClient.create(jdbcTemplate), tableNames);
	}

	/**
	 * Drop the specified tables.
	 * @param jdbcClient the {@link JdbcClient} with which to perform JDBC operations
	 * @param tableNames the names of the tables to drop
	 * @since 6.1
	 */
	public static void dropTables(JdbcClient jdbcClient, String... tableNames) {
		for (String tableName : tableNames) {
			jdbcClient.sql("DROP TABLE " + tableName).update();
			if (logger.isInfoEnabled()) {
				logger.info("Dropped table " + tableName);
			}
		}
	}

}
