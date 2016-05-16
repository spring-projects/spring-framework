/*
 * Copyright 2016 the original author or authors.
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
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * Abstract class to provide base functionality for easy updates based on
 * configuration options and database metadata. This class provides the base SPI
 * for {@link SimpleJdbcUpdate}.
 *
 * @author Thomas Risberg
 * @author Florent Paillard
 * @author Sanghyuk Jung
 * @since 4.3.2
 */
public abstract class AbstractJdbcUpdate extends AbstractSimpleSql {

	/** The generated string used for update statement */
	private String updateString;

	/** The SQL type information for the declared columns */
	private int[] columnTypes;

	/** List of columns effectively used in 'set' clause */
	private final List<String> reconciledUpdatingColumns = new ArrayList<String>();

	/** The names of the columns to be used in 'where' clause */
	private final List<String> restrictingColumns = new ArrayList<String>();


	/**
	 * Constructor for sublasses to delegate to for setting the DataSource.
	 */
	protected AbstractJdbcUpdate(DataSource dataSource) {
		super(dataSource);
	}

	/**
	 * Constructor for sublasses to delegate to for setting the JdbcTemplate.
	 */
	protected AbstractJdbcUpdate(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}


	// -------------------------------------------------------------------------
	// Methods dealing with configuration properties
	// -------------------------------------------------------------------------

	/**
	 * Get the names of 'where' columns
	 */
	public List<String> getRestrictingColumnNames() {
		return Collections.unmodifiableList(this.restrictingColumns);
	}

	/**
	 * Set the names of any primary keys
	 */
	public void setRestrictingColumnNames(List<String> whereNames) {
		checkIfConfigurationModificationIsAllowed();
		this.restrictingColumns.clear();
		this.restrictingColumns.addAll(whereNames);
	}

	/**
	 * Get the update string to be used
	 */
	protected String getUpdateString() {
		return this.updateString;
	}

	/**
	 * Get the array of {@link java.sql.Types} to be used in 'set' clause
	 */
	protected int[] getColumnTypes() {
		return this.columnTypes;
	}

	// -------------------------------------------------------------------------
	// Methods handling compilation issues
	// -------------------------------------------------------------------------

	/**
	 * Method to perform the actual compilation. Subclasses can override this
	 * template method to perform their own compilation. Invoked after this base
	 * class's compilation is complete.
	 */
	protected void compileInternal() {

		this.tableMetaDataContext.processMetaData(
				this.getJdbcTemplate().getDataSource(), getColumnNames(), new String[]{});
		this.reconciledUpdatingColumns.addAll(this.tableMetaDataContext.getTableColumns());

		List<String> columns = new ArrayList<>();
		columns.addAll(this.reconciledUpdatingColumns);
		columns.addAll(this.restrictingColumns);
		this.columnTypes = this.tableMetaDataContext.createColumnTypes(columns);

		this.updateString = this.tableMetaDataContext.createUpdateString(this.restrictingColumns);

		if (logger.isDebugEnabled()) {
			logger.debug("Compiled JdbcUpdate. Update string is [" + getUpdateString() + "]");
		}

		onCompileInternal();
	}


	// -------------------------------------------------------------------------
	// Methods handling execution
	// -------------------------------------------------------------------------

	/**
	 * Method that provides execution of the update using the passed in
	 * {@link SqlParameterSource}
	 *
	 * @param updatingValues parameter names and values to be used in update
	 * @param restrictingValues List containing PK column values
	 * @return number of rows affected
	 */
	protected int doExecute(Map<String, ?> updatingValues, Map<String, ?> restrictingValues) {
		checkCompiled();
		List<Object> values = new ArrayList<>();
		values.addAll(matchInParameterValuesWithUpdateColumns(updatingValues, reconciledUpdatingColumns));
		values.addAll(matchInParameterValuesWithUpdateColumns(restrictingValues, restrictingColumns));
		return executeUpdateInternal(values);
	}

	/**
	 * Method that provides execution of the update using the passed in Map of
	 * parameters
	 *
	 * @param updatingValues Map with parameter names and values to be used in update
	 * @param restrictingValues List containing PK column values
	 * @return number of rows affected
	 */
	protected int doExecute(SqlParameterSource updatingValues, SqlParameterSource restrictingValues) {
		checkCompiled();
		List<Object> values = new ArrayList<>();
		values.addAll(matchInParameterValuesWithUpdateColumns(updatingValues, reconciledUpdatingColumns));
		values.addAll(matchInParameterValuesWithUpdateColumns(restrictingValues, restrictingColumns));
		return executeUpdateInternal(values);
	}

	/**
	 * Method to execute the update
	 */
	private int executeUpdateInternal(List<Object> values) {
		if (logger.isDebugEnabled()) {
			logger.debug("The following parameters are used for update " + getUpdateString() + " with: " + values);
		}
		return getJdbcTemplate().update(updateString, values.toArray(), columnTypes);
	}

	/**
	 * Match the provided in parameter values with regitered parameters and
	 * parameters defined via metedata processing.
	 *
	 * @param args
	 *            the parameter values provided in a Map
	 * @return Map with parameter names and values
	 */
	protected List<Object> matchInParameterValuesWithUpdateColumns(Map<String, ?> args, List<String> columns) {
		return this.tableMetaDataContext.matchInParameterValues(args, columns);
	}

	/**
	 * Match the provided in parameter values with regitered parameters and
	 * parameters defined via metedata processing.
	 *
	 * @param parameterSource the parameter vakues provided as a {@link SqlParameterSource}
	 * @return Map with parameter names and values
	 */
	protected List<Object> matchInParameterValuesWithUpdateColumns(SqlParameterSource parameterSource, List<String> columns) {
		return this.tableMetaDataContext.matchInParameterValues(parameterSource, columns);
	}
}
