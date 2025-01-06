/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.jdbc.core.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Class to manage context meta-data used for the configuration
 * and execution of operations on a database table.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 */
public class TableMetaDataContext {

	// Logger available to subclasses
	protected final Log logger = LogFactory.getLog(getClass());

	// Name of table for this context
	@Nullable
	private String tableName;

	// Name of catalog for this context
	@Nullable
	private String catalogName;

	// Name of schema for this context
	@Nullable
	private String schemaName;

	// List of columns objects to be used in this context
	private List<String> tableColumns = new ArrayList<>();

	// Should we access insert parameter meta-data info or not
	private boolean accessTableColumnMetaData = true;

	// Should we override default for including synonyms for meta-data lookups
	private boolean overrideIncludeSynonymsDefault = false;

	// The provider of table meta-data
	@Nullable
	private TableMetaDataProvider metaDataProvider;

	// Are we using generated key columns
	private boolean generatedKeyColumnsUsed = false;


	/**
	 * Set the name of the table for this context.
	 */
	public void setTableName(@Nullable String tableName) {
		this.tableName = tableName;
	}

	/**
	 * Get the name of the table for this context.
	 */
	@Nullable
	public String getTableName() {
		return this.tableName;
	}

	/**
	 * Set the name of the catalog for this context.
	 */
	public void setCatalogName(@Nullable String catalogName) {
		this.catalogName = catalogName;
	}

	/**
	 * Get the name of the catalog for this context.
	 */
	@Nullable
	public String getCatalogName() {
		return this.catalogName;
	}

	/**
	 * Set the name of the schema for this context.
	 */
	public void setSchemaName(@Nullable String schemaName) {
		this.schemaName = schemaName;
	}

	/**
	 * Get the name of the schema for this context.
	 */
	@Nullable
	public String getSchemaName() {
		return this.schemaName;
	}

	/**
	 * Specify whether we should access table column meta-data.
	 */
	public void setAccessTableColumnMetaData(boolean accessTableColumnMetaData) {
		this.accessTableColumnMetaData = accessTableColumnMetaData;
	}

	/**
	 * Are we accessing table meta-data?
	 */
	public boolean isAccessTableColumnMetaData() {
		return this.accessTableColumnMetaData;
	}


	/**
	 * Specify whether we should override default for accessing synonyms.
	 */
	public void setOverrideIncludeSynonymsDefault(boolean override) {
		this.overrideIncludeSynonymsDefault = override;
	}

	/**
	 * Are we overriding include synonyms default?
	 */
	public boolean isOverrideIncludeSynonymsDefault() {
		return this.overrideIncludeSynonymsDefault;
	}

	/**
	 * Get a List of the table column names.
	 */
	public List<String> getTableColumns() {
		return this.tableColumns;
	}


	/**
	 * Process the current meta-data with the provided configuration options.
	 * @param dataSource the DataSource being used
	 * @param declaredColumns any columns that are declared
	 * @param generatedKeyNames name of generated keys
	 */
	public void processMetaData(DataSource dataSource, List<String> declaredColumns, String[] generatedKeyNames) {
		this.metaDataProvider = TableMetaDataProviderFactory.createMetaDataProvider(dataSource, this);
		this.tableColumns = reconcileColumnsToUse(declaredColumns, generatedKeyNames);
	}

	private TableMetaDataProvider obtainMetaDataProvider() {
		Assert.state(this.metaDataProvider != null, "No TableMetaDataProvider - call processMetaData first");
		return this.metaDataProvider;
	}

	/**
	 * Compare columns created from meta-data with declared columns and return a reconciled list.
	 * @param declaredColumns declared column names
	 * @param generatedKeyNames names of generated key columns
	 */
	protected List<String> reconcileColumnsToUse(List<String> declaredColumns, String[] generatedKeyNames) {
		if (generatedKeyNames.length > 0) {
			this.generatedKeyColumnsUsed = true;
		}
		if (!declaredColumns.isEmpty()) {
			return new ArrayList<>(declaredColumns);
		}
		Set<String> keys = new LinkedHashSet<>(generatedKeyNames.length);
		for (String key : generatedKeyNames) {
			keys.add(key.toUpperCase(Locale.ROOT));
		}
		List<String> columns = new ArrayList<>();
		for (TableParameterMetaData meta : obtainMetaDataProvider().getTableParameterMetaData()) {
			if (!keys.contains(meta.getParameterName().toUpperCase(Locale.ROOT))) {
				columns.add(meta.getParameterName());
			}
		}
		return columns;
	}

	/**
	 * Match the provided column names and values with the list of columns used.
	 * @param parameterSource the parameter names and values
	 */
	public List<Object> matchInParameterValuesWithInsertColumns(SqlParameterSource parameterSource) {
		List<Object> values = new ArrayList<>();
		// For parameter source lookups we need to provide case-insensitive lookup support since the
		// database meta-data is not necessarily providing case-sensitive column names
		Map<String, String> caseInsensitiveParameterNames =
				SqlParameterSourceUtils.extractCaseInsensitiveParameterNames(parameterSource);
		for (String column : this.tableColumns) {
			if (parameterSource.hasValue(column)) {
				values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, column));
			}
			else {
				String lowerCaseName = column.toLowerCase(Locale.ROOT);
				if (parameterSource.hasValue(lowerCaseName)) {
					values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, lowerCaseName));
				}
				else {
					String propertyName = JdbcUtils.convertUnderscoreNameToPropertyName(column);
					if (parameterSource.hasValue(propertyName)) {
						values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, propertyName));
					}
					else {
						if (caseInsensitiveParameterNames.containsKey(lowerCaseName)) {
							values.add(SqlParameterSourceUtils.getTypedValue(
									parameterSource, caseInsensitiveParameterNames.get(lowerCaseName)));
						}
						else {
							values.add(null);
						}
					}
				}
			}
		}
		return values;
	}

	/**
	 * Match the provided column names and values with the list of columns used.
	 * @param inParameters the parameter names and values
	 */
	public List<Object> matchInParameterValuesWithInsertColumns(Map<String, ?> inParameters) {
		List<Object> values = new ArrayList<>(inParameters.size());
		for (String column : this.tableColumns) {
			Object value = inParameters.get(column);
			if (value == null) {
				value = inParameters.get(column.toLowerCase(Locale.ROOT));
				if (value == null) {
					for (Map.Entry<String, ?> entry : inParameters.entrySet()) {
						if (column.equalsIgnoreCase(entry.getKey())) {
							value = entry.getValue();
							break;
						}
					}
				}
			}
			values.add(value);
		}
		return values;
	}


	/**
	 * Build the insert string based on configuration and meta-data information.
	 * @return the insert string to be used
	 */
	public String createInsertString(String... generatedKeyNames) {
		Set<String> keys = new LinkedHashSet<>(generatedKeyNames.length);
		for (String key : generatedKeyNames) {
			keys.add(key.toUpperCase(Locale.ROOT));
		}
		StringBuilder insertStatement = new StringBuilder();
		insertStatement.append("INSERT INTO ");
		if (getSchemaName() != null) {
			insertStatement.append(getSchemaName());
			insertStatement.append('.');
		}
		insertStatement.append(getTableName());
		insertStatement.append(" (");
		int columnCount = 0;
		for (String columnName : getTableColumns()) {
			if (!keys.contains(columnName.toUpperCase(Locale.ROOT))) {
				columnCount++;
				if (columnCount > 1) {
					insertStatement.append(", ");
				}
				insertStatement.append(columnName);
			}
		}
		insertStatement.append(") VALUES(");
		if (columnCount < 1) {
			if (this.generatedKeyColumnsUsed) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unable to locate non-key columns for table '" +
							getTableName() + "' so an empty insert statement is generated");
				}
			}
			else {
				String message = "Unable to locate columns for table '" + getTableName()
						+ "' so an insert statement can't be generated.";
				if (isAccessTableColumnMetaData()) {
					message += " Consider specifying explicit column names -- for example, via SimpleJdbcInsert#usingColumns().";
				}
				throw new InvalidDataAccessApiUsageException(message);
			}
		}
		String params = String.join(", ", Collections.nCopies(columnCount, "?"));
		insertStatement.append(params);
		insertStatement.append(')');
		return insertStatement.toString();
	}

	/**
	 * Build the array of {@link java.sql.Types} based on configuration and meta-data information.
	 * @return the array of types to be used
	 */
	public int[] createInsertTypes() {
		int[] types = new int[getTableColumns().size()];
		List<TableParameterMetaData> parameters = obtainMetaDataProvider().getTableParameterMetaData();
		Map<String, TableParameterMetaData> parameterMap = CollectionUtils.newLinkedHashMap(parameters.size());
		for (TableParameterMetaData tpmd : parameters) {
			parameterMap.put(tpmd.getParameterName().toUpperCase(Locale.ROOT), tpmd);
		}
		int typeIndx = 0;
		for (String column : getTableColumns()) {
			if (column == null) {
				types[typeIndx] = SqlTypeValue.TYPE_UNKNOWN;
			}
			else {
				TableParameterMetaData tpmd = parameterMap.get(column.toUpperCase(Locale.ROOT));
				if (tpmd != null) {
					types[typeIndx] = tpmd.getSqlType();
				}
				else {
					types[typeIndx] = SqlTypeValue.TYPE_UNKNOWN;
				}
			}
			typeIndx++;
		}
		return types;
	}


	/**
	 * Does this database support the JDBC 3.0 feature of retrieving generated keys:
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	public boolean isGetGeneratedKeysSupported() {
		return obtainMetaDataProvider().isGetGeneratedKeysSupported();
	}

	/**
	 * Does this database support simple query to retrieve generated keys
	 * when the JDBC 3.0 feature is not supported:
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	public boolean isGetGeneratedKeysSimulated() {
		return obtainMetaDataProvider().isGetGeneratedKeysSimulated();
	}

	/**
	 * Does this database support a simple query to retrieve generated keys
	 * when the JDBC 3.0 feature is not supported:
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 * @deprecated as of 4.3.15, in favor of {@link #getSimpleQueryForGetGeneratedKey}
	 */
	@Deprecated
	@Nullable
	public String getSimulationQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return getSimpleQueryForGetGeneratedKey(tableName, keyColumnName);
	}

	/**
	 * Does this database support a simple query to retrieve generated keys
	 * when the JDBC 3.0 feature is not supported:
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	@Nullable
	public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return obtainMetaDataProvider().getSimpleQueryForGetGeneratedKey(tableName, keyColumnName);
	}

	/**
	 * Is a column name String array for retrieving generated keys supported:
	 * {@link java.sql.Connection#createStruct(String, Object[])}?
	 */
	public boolean isGeneratedKeysColumnNameArraySupported() {
		return obtainMetaDataProvider().isGeneratedKeysColumnNameArraySupported();
	}

}
