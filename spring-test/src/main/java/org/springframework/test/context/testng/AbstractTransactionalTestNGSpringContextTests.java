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

package org.springframework.test.context.testng;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * Abstract {@link Transactional transactional} extension of
 * {@link AbstractTestNGSpringContextTests} which adds convenience functionality
 * for JDBC access. Expects a {@link DataSource} bean and a
 * {@link PlatformTransactionManager} bean to be defined in the Spring
 * {@link ApplicationContext application context}.
 * </p>
 * <p>
 * This class exposes a {@link SimpleJdbcTemplate} and provides an easy way to
 * {@link #countRowsInTable(String) count the number of rows in a table} ,
 * {@link #deleteFromTables(String...) delete from the database} , and
 * {@link #executeSqlScript(String, boolean) execute SQL scripts} within a
 * transaction.
 * </p>
 * <p>
 * Concrete subclasses must fulfill the same requirements outlined in
 * {@link AbstractTestNGSpringContextTests}.
 * </p>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see AbstractTestNGSpringContextTests
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.TestExecutionListeners
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 * @see org.springframework.test.context.transaction.TransactionConfiguration
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.test.annotation.NotTransactional
 * @see org.springframework.test.annotation.Rollback
 * @see org.springframework.test.jdbc.SimpleJdbcTestUtils
 * @see org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests
 */
@SuppressWarnings("deprecation")
@TestExecutionListeners(TransactionalTestExecutionListener.class)
@Transactional
public abstract class AbstractTransactionalTestNGSpringContextTests extends AbstractTestNGSpringContextTests {

	/**
	 * The SimpleJdbcTemplate that this base class manages, available to subclasses.
	 */
	protected SimpleJdbcTemplate simpleJdbcTemplate;

	private String sqlScriptEncoding;


	/**
	 * Set the DataSource, typically provided via Dependency Injection.
	 * @param dataSource the DataSource to inject
	 */
	@Autowired
	public void setDataSource(final DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	/**
	 * Specify the encoding for SQL scripts, if different from the platform encoding.
	 * @see #executeSqlScript
	 */
	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}

	/**
	 * Count the rows in the given table.
	 * @param tableName table name to count rows in
	 * @return the number of rows in the table
	 */
	protected int countRowsInTable(String tableName) {
		return SimpleJdbcTestUtils.countRowsInTable(this.simpleJdbcTemplate, tableName);
	}

	/**
	 * Convenience method for deleting all rows from the specified tables.
	 * Use with caution outside of a transaction!
	 * @param names the names of the tables from which to delete
	 * @return the total number of rows deleted from all specified tables
	 */
	protected int deleteFromTables(String... names) {
		return SimpleJdbcTestUtils.deleteFromTables(this.simpleJdbcTemplate, names);
	}

	/**
	 * Execute the given SQL script. Use with caution outside of a transaction!
	 * <p>The script will normally be loaded by classpath. There should be one statement
	 * per line. Any semicolons will be removed. <b>Do not use this method to execute
	 * DDL if you expect rollback.</b>
	 * @param sqlResourcePath the Spring resource path for the SQL script
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error
	 * @throws DataAccessException if there is an error executing a statement
	 * and continueOnError was <code>false</code>
	 */
	protected void executeSqlScript(String sqlResourcePath, boolean continueOnError) throws DataAccessException {

		Resource resource = this.applicationContext.getResource(sqlResourcePath);
		SimpleJdbcTestUtils.executeSqlScript(this.simpleJdbcTemplate, new EncodedResource(resource,
			this.sqlScriptEncoding), continueOnError);
	}

}
