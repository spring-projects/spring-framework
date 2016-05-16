/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jdbc.core.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.metadata.TableMetaDataContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract class to provide base functionality for compiling SQL
 * based on configuration options and database metadata.
 *
 * @author Thomas Risberg
 * @author Sanghyuk Jung
 * @since 4.3.2
 */
public abstract class AbstractSimpleSql {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Context used to retrieve and manage database metadata */
	protected final TableMetaDataContext tableMetaDataContext = new TableMetaDataContext();

	/** Lower-level class used to execute SQL */
	private final JdbcTemplate jdbcTemplate;

	/** List of columns objects to be used in SQL statement */
	private final List<String> declaredColumns = new ArrayList<String>();

	private volatile boolean compiled = false;

	/**
	 * Constructor for sublasses to delegate to for setting the DataSource.
	 */
	protected AbstractSimpleSql(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * Constructor for sublasses to delegate to for setting the JdbcTemplate.
	 */
	protected AbstractSimpleSql(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.jdbcTemplate = jdbcTemplate;
	}
	//-------------------------------------------------------------------------
	// Methods dealing with configuration properties
	//-------------------------------------------------------------------------

	/**
	 * Get the configured {@link JdbcTemplate}.
	 */
	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * Set the name of the table for this insert.
	 */
	public void setTableName(@Nullable String tableName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setTableName(tableName);
	}

	/**
	 * Get the name of the table for this insert.
	 */
	@Nullable
	public String getTableName() {
		return this.tableMetaDataContext.getTableName();
	}

	/**
	 * Set the name of the schema for this insert.
	 */
	public void setSchemaName(@Nullable String schemaName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setSchemaName(schemaName);
	}

	/**
	 * Get the name of the schema for this insert.
	 */
	@Nullable
	public String getSchemaName() {
		return this.tableMetaDataContext.getSchemaName();
	}

	/**
	 * Set the name of the catalog for this insert.
	 */
	public void setCatalogName(@Nullable String catalogName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setCatalogName(catalogName);
	}

	/**
	 * Get the name of the catalog for this insert.
	 */
	@Nullable
	public String getCatalogName() {
		return this.tableMetaDataContext.getCatalogName();
	}


	/**
	 * Set the names of the columns to be used.
	 */
	public void setColumnNames(List<String> columnNames) {
		checkIfConfigurationModificationIsAllowed();
		this.declaredColumns.clear();
		this.declaredColumns.addAll(columnNames);
	}

	/**
	 * Get the names of the columns used.
	 */
	public List<String> getColumnNames() {
		return Collections.unmodifiableList(this.declaredColumns);
	}

	/**
	 * Specify whether the parameter metadata for the call should be used.
	 * The default is {@code true}.
	 */
	public void setAccessTableColumnMetaData(boolean accessTableColumnMetaData) {
		this.tableMetaDataContext.setAccessTableColumnMetaData(accessTableColumnMetaData);
	}

	/**
	 * Specify whether the default for including synonyms should be changed.
	 * The default is {@code false}.
	 */
	public void setOverrideIncludeSynonymsDefault(boolean override) {
		this.tableMetaDataContext.setOverrideIncludeSynonymsDefault(override);
	}

	//-------------------------------------------------------------------------
	// Methods handling compilation issues
	//-------------------------------------------------------------------------

	/**
	 * Compile this JdbcInsert using provided parameters and meta data plus other settings.
	 * This finalizes the configuration for this object and subsequent attempts to compile are
	 * ignored. This will be implicitly called the first time an un-compiled insert is executed.
	 * @throws InvalidDataAccessApiUsageException if the object hasn't been correctly initialized,
	 * for example if no DataSource has been provided
	 */
	public synchronized final void compile() throws InvalidDataAccessApiUsageException {
		if (!isCompiled()) {
			if (getTableName() == null) {
				throw new InvalidDataAccessApiUsageException("Table name is required");
			}
			try {
				this.jdbcTemplate.afterPropertiesSet();
			}
			catch (IllegalArgumentException ex) {
				throw new InvalidDataAccessApiUsageException(ex.getMessage());
			}
			compileInternal();
			this.compiled = true;
			if (logger.isDebugEnabled()) {
				logger.debug("SQL for table [" + getTableName() + "] compiled");
			}
		}
	}

	/**
	 * Delegate method to perform the actual compilation.
	 * <p>Subclasses can override this template method to perform  their own compilation.
	 * Invoked after this base class's compilation is complete.
	 */
	abstract protected void compileInternal();

	/**
	 * Hook method that subclasses may override to react to compilation.
	 * <p>This implementation is empty.
	 */
	protected void onCompileInternal() {
		// No code by default, should be overridden only by demand at subclasses
	}

	/**
	 * Is this operation "compiled"?
	 * @return whether this operation is compiled and ready to use
	 */
	public boolean isCompiled() {
		return this.compiled;
	}

	/**
	 * Check whether this operation has been compiled already;
	 * lazily compile it if not already compiled.
	 * <p>Automatically called by {@code validateParameters}.
	 */
	protected void checkCompiled() {
		if (!isCompiled()) {
			if (logger.isDebugEnabled()) {
				logger.debug("SQL not compiled before execution - invoking compile");
			}
			compile();
		}
	}

	/**
	 * Method to check whether we are allowed to make any configuration changes at this time.
	 * If the class has been compiled, then no further changes to the configuration are allowed.
	 */
	protected void checkIfConfigurationModificationIsAllowed() {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
				"Configuration can't be altered once the class has been compiled or used");
		}
	}
}
