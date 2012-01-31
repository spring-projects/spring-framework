/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.jdbc.core.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;

/**
 * Class to manage context metadata used for the configuration
 * and execution of operations on a database table.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public class TableMetaDataContext {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** name of procedure to call **/
	private String tableName;

	/** name of catalog for call **/
	private String catalogName;

	/** name of schema for call **/
	private String schemaName;

	/** List of columns objects to be used in this context */
	private List<String> tableColumns = new ArrayList<String>();

	/** should we access insert parameter meta data info or not */
	private boolean accessTableColumnMetaData = true;

	/** should we override default for including synonyms for meta data lookups */
	private boolean overrideIncludeSynonymsDefault = false;

	/** the provider of table meta data */
	private TableMetaDataProvider metaDataProvider;

	/** are we using generated key columns */
	private boolean generatedKeyColumnsUsed = false;

	/** NativeJdbcExtractor to be used to retrieve the native connection */
	NativeJdbcExtractor nativeJdbcExtractor;


	/**
	 * Set the name of the table for this context.
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * Get the name of the table for this context.
	 */
	public String getTableName() {
		return this.tableName;
	}

	/**
	 * Set the name of the catalog for this context.
	 */
	public void setCatalogName(String catalogName) {
		this.catalogName = catalogName;
	}

	/**
	 * Get the name of the catalog for this context.
	 */
	public String getCatalogName() {
		return this.catalogName;
	}

	/**
	 * Set the name of the schema for this context.
	 */
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	/**
	 * Get the name of the schema for this context.
	 */
	public String getSchemaName() {
		return this.schemaName;
	}

	/**
	 * Specify whether we should access table column meta data.
	 */
	public void setAccessTableColumnMetaData(boolean accessTableColumnMetaData) {
		this.accessTableColumnMetaData = accessTableColumnMetaData;
	}

	/**
	 * Are we accessing table meta data?
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
	 * Does this database support the JDBC 3.0 feature of retrieving generated keys
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	public boolean isGetGeneratedKeysSupported() {
		return this.metaDataProvider.isGetGeneratedKeysSupported();
	}

	/**
	 * Does this database support simple query to retrieve generated keys
	 * when the JDBC 3.0 feature is not supported.
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	public boolean isGetGeneratedKeysSimulated() {
		return this.metaDataProvider.isGetGeneratedKeysSimulated();
	}

	/**
	 * Does this database support simple query to retrieve generated keys
	 * when the JDBC 3.0 feature is not supported.
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	public String getSimulationQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return this.metaDataProvider.getSimpleQueryForGetGeneratedKey(tableName, keyColumnName);
	}

	/**
	 * Is a column name String array for retrieving generated keys supported?
	 * {@link java.sql.Connection#createStruct(String, Object[])}?
	 */
	public boolean isGeneratedKeysColumnNameArraySupported() {
		return this.metaDataProvider.isGeneratedKeysColumnNameArraySupported();
	}

	/**
	 * Set {@link NativeJdbcExtractor} to be used to retrieve the native connection.
	 */
	public void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor) {
		this.nativeJdbcExtractor = nativeJdbcExtractor;
	}


	/**
	 * Process the current meta data with the provided configuration options.
	 * @param dataSource the DataSource being used
	 * @param declaredColumns any columns that are declared
	 * @param generatedKeyNames name of generated keys
	 */
	public void processMetaData(DataSource dataSource, List<String> declaredColumns, String[] generatedKeyNames) {
		this.metaDataProvider = TableMetaDataProviderFactory.createMetaDataProvider(dataSource, this, this.nativeJdbcExtractor);
		this.tableColumns = reconcileColumnsToUse(declaredColumns, generatedKeyNames);
	}

	/**
	 * Compare columns created from metadata with declared columns and return a reconciled list.
	 * @param declaredColumns declared column names
	 * @param generatedKeyNames names of generated key columns
	 */
	protected List<String> reconcileColumnsToUse(List<String> declaredColumns, String[] generatedKeyNames) {
		if (generatedKeyNames.length > 0) {
			this.generatedKeyColumnsUsed = true;
		}
		if (declaredColumns.size() > 0) {
			return new ArrayList<String>(declaredColumns);
		}
		Set<String> keys = new HashSet<String>(generatedKeyNames.length);
		for (String key : generatedKeyNames) {
			keys.add(key.toUpperCase());
		}
		List<String> columns = new ArrayList<String>();
		for (TableParameterMetaData meta : metaDataProvider.getTableParameterMetaData()) {
			if (!keys.contains(meta.getParameterName().toUpperCase())) {
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
		List<Object> values = new ArrayList<Object>();
		// for parameter source lookups we need to provide caseinsensitive lookup support since the
		// database metadata is not necessarily providing case sensitive column names
		Map caseInsensitiveParameterNames =
				SqlParameterSourceUtils.extractCaseInsensitiveParameterNames(parameterSource);
		for (String column : this.tableColumns) {
			if (parameterSource.hasValue(column)) {
				values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, column));
			}
			else {
				String lowerCaseName = column.toLowerCase();
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
							values.add(
									SqlParameterSourceUtils.getTypedValue(parameterSource,
											(String) caseInsensitiveParameterNames.get(lowerCaseName)));
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
	public List<Object> matchInParameterValuesWithInsertColumns(Map<String, Object> inParameters) {
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> source = new HashMap<String, Object>();
		for (String key : inParameters.keySet()) {
			source.put(key.toLowerCase(), inParameters.get(key));
		}
		for (String column : this.tableColumns) {
			values.add(source.get(column.toLowerCase()));
		}
		return values;
	}


	/**
	 * Build the insert string based on configuration and metadata information
	 * @return the insert string to be used
	 */
	public String createInsertString(String[] generatedKeyNames) {
		HashSet<String> keys = new HashSet<String>(generatedKeyNames.length);
		for (String key : generatedKeyNames) {
			keys.add(key.toUpperCase());
		}
		StringBuilder insertStatement = new StringBuilder();
		insertStatement.append("INSERT INTO ");
		if (this.getSchemaName() != null) {
			insertStatement.append(this.getSchemaName());
			insertStatement.append(".");
		}
		insertStatement.append(this.getTableName());
		insertStatement.append(" (");
		int columnCount = 0;
		for (String columnName : this.getTableColumns()) {
			if (!keys.contains(columnName.toUpperCase())) {
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
				logger.info("Unable to locate non-key columns for table '" +
						this.getTableName() + "' so an empty insert statement is generated");
			}
			else {
				throw new InvalidDataAccessApiUsageException("Unable to locate columns for table '" +
						this.getTableName() + "' so an insert statement can't be generated");
			}
		}
		for (int i = 0; i < columnCount; i++) {
			if (i > 0) {
				insertStatement.append(", ");
			}
			insertStatement.append("?");
		}
		insertStatement.append(")");
		return insertStatement.toString();
	}

	/**
	 * Build the array of {@link java.sql.Types} based on configuration and metadata information
	 * @return the array of types to be used
	 */
	public int[] createInsertTypes() {
		int[] types = new int[this.getTableColumns().size()];
		List<TableParameterMetaData> parameters = this.metaDataProvider.getTableParameterMetaData();
		Map<String, TableParameterMetaData> parameterMap = new HashMap<String, TableParameterMetaData>(parameters.size());
		for (TableParameterMetaData tpmd : parameters) {
			parameterMap.put(tpmd.getParameterName().toUpperCase(), tpmd);
		}
		int typeIndx = 0;
		for (String column : this.getTableColumns()) {
			if (column == null) {
				types[typeIndx] = SqlTypeValue.TYPE_UNKNOWN;
			}
			else {
				TableParameterMetaData tpmd = parameterMap.get(column.toUpperCase());
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

}
