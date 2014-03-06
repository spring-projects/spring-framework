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

package org.springframework.test.context.junit4;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract {@linkplain Transactional transactional} extension of
 * {@link AbstractJUnit4SpringContextTests} which adds convenience functionality
 * for JDBC access. Expects a {@link DataSource} bean and a
 * {@link PlatformTransactionManager} bean to be defined in the Spring
 * {@linkplain ApplicationContext application context}.
 *
 * <p>This class exposes a {@link JdbcTemplate} and provides an easy way to
 * {@linkplain #countRowsInTable count the number of rows in a table}
 * (potentially {@linkplain #countRowsInTableWhere with a WHERE clause}),
 * {@linkplain #deleteFromTables delete from tables},
 * {@linkplain #dropTables drop tables}, and
 * {@linkplain #executeSqlScript execute SQL scripts} within a transaction.
 *
 * <p>Concrete subclasses must fulfill the same requirements outlined in
 * {@link AbstractJUnit4SpringContextTests}.
 *
 * <p>Note: this class serves only as a convenience for extension. If you do not
 * wish for your test classes to be tied to a Spring-specific class hierarchy,
 * you may configure your own custom test classes by using
 * {@link SpringJUnit4ClassRunner}, {@link ContextConfiguration
 * &#064;ContextConfiguration}, {@link TestExecutionListeners
 * &#064;TestExecutionListeners}, {@link Transactional &#064;Transactional},
 * etc.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see AbstractJUnit4SpringContextTests
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.TestExecutionListeners
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 * @see org.springframework.test.context.transaction.TransactionConfiguration
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.test.annotation.Rollback
 * @see org.springframework.test.context.transaction.BeforeTransaction
 * @see org.springframework.test.context.transaction.AfterTransaction
 * @see org.springframework.test.jdbc.JdbcTestUtils
 * @see org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
 */
@TestExecutionListeners(TransactionalTestExecutionListener.class)
@Transactional
public abstract class AbstractTransactionalJUnit4SpringContextTests extends AbstractJUnit4SpringContextTests {

	/**
	 * The {@code JdbcTemplate} that this base class manages, available to subclasses.
	 * @since 3.2
	 */
	protected JdbcTemplate jdbcTemplate;

	private String sqlScriptEncoding;


	/**
	 * Set the {@code DataSource}, typically provided via Dependency Injection.
	 * <p>This method also instantiates the {@link #jdbcTemplate} instance variable.
	 */
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * Specify the encoding for SQL scripts, if different from the platform encoding.
	 * @see #executeSqlScript
	 */
	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}

	/**
	 * Convenience method for counting the rows in the given table.
	 * @param tableName table name to count rows in
	 * @return the number of rows in the table
	 * @see JdbcTestUtils#countRowsInTable
	 */
	protected int countRowsInTable(String tableName) {
		return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
	}

	/**
	 * Convenience method for counting the rows in the given table, using the
	 * provided {@code WHERE} clause.
	 * <p>See the Javadoc for {@link JdbcTestUtils#countRowsInTableWhere} for details.
	 * @param tableName the name of the table to count rows in
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @return the number of rows in the table that match the provided
	 * {@code WHERE} clause
	 * @since 3.2
	 * @see JdbcTestUtils#countRowsInTableWhere
	 */
	protected int countRowsInTableWhere(String tableName, String whereClause) {
		return JdbcTestUtils.countRowsInTableWhere(this.jdbcTemplate, tableName, whereClause);
	}

	/**
	 * Convenience method for deleting all rows from the specified tables.
	 * <p>Use with caution outside of a transaction!
	 * @param names the names of the tables from which to delete
	 * @return the total number of rows deleted from all specified tables
	 * @see JdbcTestUtils#deleteFromTables
	 */
	protected int deleteFromTables(String... names) {
		return JdbcTestUtils.deleteFromTables(this.jdbcTemplate, names);
	}

	/**
	 * Convenience method for deleting all rows from the given table, using the
	 * provided {@code WHERE} clause.
	 * <p>Use with caution outside of a transaction!
	 * <p>See the Javadoc for {@link JdbcTestUtils#deleteFromTableWhere} for details.
	 * @param tableName the name of the table to delete rows from
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @param args arguments to bind to the query (leaving it to the {@code
	 * PreparedStatement} to guess the corresponding SQL type); may also contain
	 * {@link org.springframework.jdbc.core.SqlParameterValue SqlParameterValue}
	 * objects which indicate not only the argument value but also the SQL type
	 * and optionally the scale.
	 * @return the number of rows deleted from the table
	 * @since 4.0
	 * @see JdbcTestUtils#deleteFromTableWhere
	 */
	protected int deleteFromTableWhere(String tableName, String whereClause, Object... args) {
		return JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, tableName, whereClause, args);
	}

	/**
	 * Convenience method for dropping all of the specified tables.
	 * <p>Use with caution outside of a transaction!
	 * @param names the names of the tables to drop
	 * @since 3.2
	 * @see JdbcTestUtils#dropTables
	 */
	protected void dropTables(String... names) {
		JdbcTestUtils.dropTables(this.jdbcTemplate, names);
	}

	/**
	 * Execute the given SQL script.
	 * <p>Use with caution outside of a transaction!
	 * <p>The script will normally be loaded by classpath. There should be one
	 * statement per line. Any semicolons will be removed. <b>Do not use this
	 * method to execute DDL if you expect rollback.</b>
	 * @param sqlResourcePath the Spring resource path for the SQL script
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error
	 * @throws DataAccessException if there is an error executing a statement
	 * and continueOnError was {@code false}
	 * @see JdbcTestUtils#executeSqlScript(JdbcTemplate, EncodedResource, boolean)
	 * @see #setSqlScriptEncoding
	 */
	@SuppressWarnings("deprecation")
	protected void executeSqlScript(String sqlResourcePath, boolean continueOnError) throws DataAccessException {
		Resource resource = this.applicationContext.getResource(sqlResourcePath);
		JdbcTestUtils.executeSqlScript(this.jdbcTemplate, new EncodedResource(resource, this.sqlScriptEncoding),
			continueOnError);
	}

}
