/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.core.support;

import java.sql.Connection;
import javax.sql.DataSource;

import org.springframework.dao.support.DaoSupport;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenient super class for JDBC-based data access objects.
 *
 * <p>Requires a {@link javax.sql.DataSource} to be set, providing a
 * {@link org.springframework.jdbc.core.JdbcTemplate} based on it to
 * subclasses through the {@link #getJdbcTemplate()} method.
 *
 * <p>This base class is mainly intended for JdbcTemplate usage but can
 * also be used when working with a Connection directly or when using
 * {@code org.springframework.jdbc.object} operation objects.
 *
 * @author Juergen Hoeller
 * @since 28.07.2003
 * @see #setDataSource
 * @see #getJdbcTemplate
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public abstract class JdbcDaoSupport extends DaoSupport {

	@Nullable
	private JdbcTemplate jdbcTemplate;


	/**
	 * Set the JDBC DataSource to be used by this DAO.
	 */
	public final void setDataSource(DataSource dataSource) {
		if (this.jdbcTemplate == null || dataSource != this.jdbcTemplate.getDataSource()) {
			this.jdbcTemplate = createJdbcTemplate(dataSource);
			initTemplateConfig();
		}
	}

	/**
	 * Create a JdbcTemplate for the given DataSource.
	 * Only invoked if populating the DAO with a DataSource reference!
	 * <p>Can be overridden in subclasses to provide a JdbcTemplate instance
	 * with different configuration, or a custom JdbcTemplate subclass.
	 * @param dataSource the JDBC DataSource to create a JdbcTemplate for
	 * @return the new JdbcTemplate instance
	 * @see #setDataSource
	 */
	protected JdbcTemplate createJdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	/**
	 * Return the JDBC DataSource used by this DAO.
	 */
	@Nullable
	public final DataSource getDataSource() {
		return (this.jdbcTemplate != null ? this.jdbcTemplate.getDataSource() : null);
	}

	/**
	 * Set the JdbcTemplate for this DAO explicitly,
	 * as an alternative to specifying a DataSource.
	 */
	public final void setJdbcTemplate(@Nullable JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		initTemplateConfig();
	}

	/**
	 * Return the JdbcTemplate for this DAO,
	 * pre-initialized with the DataSource or set explicitly.
	 */
	@Nullable
	public final JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * Initialize the template-based configuration of this DAO.
	 * Called after a new JdbcTemplate has been set, either directly
	 * or through a DataSource.
	 * <p>This implementation is empty. Subclasses may override this
	 * to configure further objects based on the JdbcTemplate.
	 * @see #getJdbcTemplate()
	 */
	protected void initTemplateConfig() {
	}

	@Override
	protected void checkDaoConfig() {
		if (this.jdbcTemplate == null) {
			throw new IllegalArgumentException("'dataSource' or 'jdbcTemplate' is required");
		}
	}


	/**
	 * Return the SQLExceptionTranslator of this DAO's JdbcTemplate,
	 * for translating SQLExceptions in custom JDBC access code.
	 * @see org.springframework.jdbc.core.JdbcTemplate#getExceptionTranslator()
	 */
	protected final SQLExceptionTranslator getExceptionTranslator() {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		Assert.state(jdbcTemplate != null, "No JdbcTemplate set");
		return jdbcTemplate.getExceptionTranslator();
	}

	/**
	 * Get a JDBC Connection, either from the current transaction or a new one.
	 * @return the JDBC Connection
	 * @throws CannotGetJdbcConnectionException if the attempt to get a Connection failed
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection(javax.sql.DataSource)
	 */
	protected final Connection getConnection() throws CannotGetJdbcConnectionException {
		DataSource dataSource = getDataSource();
		Assert.state(dataSource != null, "No DataSource set");
		return DataSourceUtils.getConnection(dataSource);
	}

	/**
	 * Close the given JDBC Connection, created via this DAO's DataSource,
	 * if it isn't bound to the thread.
	 * @param con the Connection to close
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
	 */
	protected final void releaseConnection(Connection con) {
		DataSourceUtils.releaseConnection(con, getDataSource());
	}

}
