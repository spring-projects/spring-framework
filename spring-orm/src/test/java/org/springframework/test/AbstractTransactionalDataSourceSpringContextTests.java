/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test;

import javax.sql.DataSource;

import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * This class is only used within tests in the spring-orm module.
 *
 * <p>Subclass of AbstractTransactionalSpringContextTests that adds some convenience
 * functionality for JDBC access. Expects a {@link javax.sql.DataSource} bean
 * to be defined in the Spring application context.
 *
 * <p>This class exposes a {@link org.springframework.jdbc.core.JdbcTemplate}
 * and provides an easy way to delete from the database in a new transaction.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since 1.1.1
 * @see #setDataSource(javax.sql.DataSource)
 * @see #getJdbcTemplate()
 * @deprecated as of Spring 3.0, in favor of using the listener-based test context framework
 * ({@link org.springframework.test.context.junit38.AbstractJUnit38SpringContextTests})
 */
@Deprecated
public abstract class AbstractTransactionalDataSourceSpringContextTests extends AbstractTransactionalSpringContextTests {

	protected JdbcTemplate jdbcTemplate;

	private String sqlScriptEncoding;

	/**
	 * Did this test delete any tables? If so, we forbid transaction completion,
	 * and only allow rollback.
	 */
	private boolean zappedTables;


	/**
	 * Default constructor for AbstractTransactionalDataSourceSpringContextTests.
	 */
	public AbstractTransactionalDataSourceSpringContextTests() {
	}

	/**
	 * Constructor for AbstractTransactionalDataSourceSpringContextTests with a JUnit name.
	 */
	public AbstractTransactionalDataSourceSpringContextTests(String name) {
		super(name);
	}


	/**
	 * Setter: DataSource is provided by Dependency Injection.
	 */
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * Return the JdbcTemplate that this base class manages.
	 */
	public final JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * Specify the encoding for SQL scripts, if different from the platform encoding.
	 * @see #executeSqlScript
	 */
	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}


	/**
	 * Convenient method to delete all rows from these tables.
	 * Calling this method will make avoidance of rollback by calling
	 * {@code setComplete()} impossible.
	 * @see #setComplete
	 */
	protected void deleteFromTables(String... names) {
		for (String name : names) {
			int rowCount = this.jdbcTemplate.update("DELETE FROM " + name);
			if (logger.isInfoEnabled()) {
				logger.info("Deleted " + rowCount + " rows from table " + name);
			}
		}
		this.zappedTables = true;
	}

	/**
	 * Overridden to prevent the transaction committing if a number of tables have been
	 * cleared, as a defensive measure against accidental <i>permanent</i> wiping of a database.
	 * @see org.springframework.test.AbstractTransactionalSpringContextTests#setComplete()
	 */
	@Override
	protected final void setComplete() {
		if (this.zappedTables) {
			throw new IllegalStateException("Cannot set complete after deleting tables");
		}
		super.setComplete();
	}

	/**
	 * Count the rows in the given table
	 * @param tableName table name to count rows in
	 * @return the number of rows in the table
	 */
	protected int countRowsInTable(String tableName) {
		return this.jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + tableName, Integer.class);
	}


	/**
	 * Execute the given SQL script.
	 * <p>Use with caution outside of a transaction!
	 * <p>The script will normally be loaded by classpath.
	 * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
	 * @param sqlResourcePath the Spring resource path for the SQL script
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error
	 * @throws DataAccessException if there is an error executing a statement
	 * @see ResourceDatabasePopulator
	 * @see #setSqlScriptEncoding
	 */
	protected void executeSqlScript(String sqlResourcePath, boolean continueOnError) throws DataAccessException {
		Resource resource = this.applicationContext.getResource(sqlResourcePath);
		new ResourceDatabasePopulator(continueOnError, false, this.sqlScriptEncoding, resource).execute(jdbcTemplate.getDataSource());
	}

}
