/*
 * Copyright 2002-2024 the original author or authors.
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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;

/**
 * A generic implementation of the {@link TableMetaDataProvider} interface
 * which should provide enough features for all supported databases.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 */
public class GenericTableMetaDataProvider implements TableMetaDataProvider {

	/** Logger available to subclasses. */
	protected static final Log logger = LogFactory.getLog(TableMetaDataProvider.class);

	/** Database products we know not supporting the use of a String[] for generated keys. */
	private static final List<String> productsNotSupportingGeneratedKeysColumnNameArray =
			Arrays.asList("Apache Derby", "HSQL Database Engine");


	/** The name of the user currently connected. */
	@Nullable
	private final String userName;

	/** The version of the database. */
	@Nullable
	private String databaseVersion;

	/** Indicates whether column meta-data has been used. */
	private boolean tableColumnMetaDataUsed = false;

	/** Indicates whether generated keys retrieval is supported. */
	private boolean getGeneratedKeysSupported = true;

	/** Indicates whether the use of a String[] for generated keys is supported. */
	private boolean generatedKeysColumnNameArraySupported = true;

	/** Indicates whether the identifiers are uppercased. */
	private boolean storesUpperCaseIdentifiers = true;

	/** Indicates whether the identifiers are lowercased. */
	private boolean storesLowerCaseIdentifiers = false;

	/** The string used to quote SQL identifiers. */
	private String identifierQuoteString = " ";

	/** Collection of TableParameterMetaData objects. */
	private final List<TableParameterMetaData> tableParameterMetaData = new ArrayList<>();


	/**
	 * Constructor used to initialize with provided database meta-data.
	 * @param databaseMetaData meta-data to be used
	 */
	protected GenericTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		this.userName = databaseMetaData.getUserName();
	}


	@Override
	public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
		try {
			if (databaseMetaData.supportsGetGeneratedKeys()) {
				logger.debug("GetGeneratedKeys is supported");
				setGetGeneratedKeysSupported(true);
			}
			else {
				logger.debug("GetGeneratedKeys is not supported");
				setGetGeneratedKeysSupported(false);
			}
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error retrieving 'DatabaseMetaData.supportsGetGeneratedKeys': " + ex.getMessage());
			}
		}
		try {
			String databaseProductName = databaseMetaData.getDatabaseProductName();
			if (productsNotSupportingGeneratedKeysColumnNameArray.contains(databaseProductName)) {
				if (logger.isDebugEnabled()) {
					logger.debug("GeneratedKeysColumnNameArray is not supported for " + databaseProductName);
				}
				setGeneratedKeysColumnNameArraySupported(false);
			}
			else {
				if (isGetGeneratedKeysSupported()) {
					if (logger.isDebugEnabled()) {
						logger.debug("GeneratedKeysColumnNameArray is supported for " + databaseProductName);
					}
					setGeneratedKeysColumnNameArraySupported(true);
				}
				else {
					setGeneratedKeysColumnNameArraySupported(false);
				}
			}
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error retrieving 'DatabaseMetaData.getDatabaseProductName': " + ex.getMessage());
			}
		}

		try {
			this.databaseVersion = databaseMetaData.getDatabaseProductVersion();
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error retrieving 'DatabaseMetaData.getDatabaseProductVersion': " + ex.getMessage());
			}
		}

		try {
			setStoresUpperCaseIdentifiers(databaseMetaData.storesUpperCaseIdentifiers());
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error retrieving 'DatabaseMetaData.storesUpperCaseIdentifiers': " + ex.getMessage());
			}
		}

		try {
			setStoresLowerCaseIdentifiers(databaseMetaData.storesLowerCaseIdentifiers());
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error retrieving 'DatabaseMetaData.storesLowerCaseIdentifiers': " + ex.getMessage());
			}
		}

		try {
			this.identifierQuoteString = databaseMetaData.getIdentifierQuoteString();
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error retrieving 'DatabaseMetaData.getIdentifierQuoteString': " + ex.getMessage());
			}
		}
	}

	@Override
	public void initializeWithTableColumnMetaData(DatabaseMetaData databaseMetaData, @Nullable String catalogName,
			@Nullable String schemaName, @Nullable String tableName) throws SQLException {

		this.tableColumnMetaDataUsed = true;
		locateTableAndProcessMetaData(databaseMetaData, catalogName, schemaName, tableName);
	}

	@Override
	public List<TableParameterMetaData> getTableParameterMetaData() {
		return this.tableParameterMetaData;
	}

	@Override
	@Nullable
	public String tableNameToUse(@Nullable String tableName) {
		return identifierNameToUse(tableName);
	}

	@Override
	@Nullable
	public String columnNameToUse(@Nullable String columnName) {
		return identifierNameToUse(columnName);
	}

	@Override
	@Nullable
	public String catalogNameToUse(@Nullable String catalogName) {
		return identifierNameToUse(catalogName);
	}

	@Override
	@Nullable
	public String schemaNameToUse(@Nullable String schemaName) {
		return identifierNameToUse(schemaName);
	}

	@Nullable
	private String identifierNameToUse(@Nullable String identifierName) {
		if (identifierName == null) {
			return null;
		}
		else if (isStoresUpperCaseIdentifiers()) {
			return identifierName.toUpperCase();
		}
		else if (isStoresLowerCaseIdentifiers()) {
			return identifierName.toLowerCase();
		}
		else {
			return identifierName;
		}
	}

	@Override
	@Nullable
	public String metaDataCatalogNameToUse(@Nullable String catalogName) {
		return catalogNameToUse(catalogName);
	}

	@Override
	@Nullable
	public String metaDataSchemaNameToUse(@Nullable String schemaName) {
		if (schemaName == null) {
			return schemaNameToUse(getDefaultSchema());
		}
		return schemaNameToUse(schemaName);
	}

	/**
	 * Provide access to the default schema for subclasses.
	 */
	@Nullable
	protected String getDefaultSchema() {
		return this.userName;
	}

	/**
	 * Provide access to the version info for subclasses.
	 */
	@Nullable
	protected String getDatabaseVersion() {
		return this.databaseVersion;
	}

	@Override
	public boolean isTableColumnMetaDataUsed() {
		return this.tableColumnMetaDataUsed;
	}

	public void setGetGeneratedKeysSupported(boolean getGeneratedKeysSupported) {
		this.getGeneratedKeysSupported = getGeneratedKeysSupported;
	}

	@Override
	public boolean isGetGeneratedKeysSupported() {
		return this.getGeneratedKeysSupported;
	}

	@Override
	public boolean isGetGeneratedKeysSimulated(){
		return false;
	}

	@Override
	@Nullable
	public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return null;
	}

	public void setGeneratedKeysColumnNameArraySupported(boolean generatedKeysColumnNameArraySupported) {
		this.generatedKeysColumnNameArraySupported = generatedKeysColumnNameArraySupported;
	}

	@Override
	public boolean isGeneratedKeysColumnNameArraySupported() {
		return this.generatedKeysColumnNameArraySupported;
	}

	public void setStoresUpperCaseIdentifiers(boolean storesUpperCaseIdentifiers) {
		this.storesUpperCaseIdentifiers = storesUpperCaseIdentifiers;
	}

	public boolean isStoresUpperCaseIdentifiers() {
		return this.storesUpperCaseIdentifiers;
	}

	public void setStoresLowerCaseIdentifiers(boolean storesLowerCaseIdentifiers) {
		this.storesLowerCaseIdentifiers = storesLowerCaseIdentifiers;
	}

	public boolean isStoresLowerCaseIdentifiers() {
		return this.storesLowerCaseIdentifiers;
	}

	@Override
	public String getIdentifierQuoteString() {
		return this.identifierQuoteString;
	}


	/**
	 * Method supporting the meta-data processing for a table.
	 */
	private void locateTableAndProcessMetaData(DatabaseMetaData databaseMetaData,
			@Nullable String catalogName, @Nullable String schemaName, @Nullable String tableName) {

		Map<String, TableMetaData> tableMeta = new HashMap<>();
		ResultSet tables = null;
		try {
			tables = databaseMetaData.getTables(
					catalogNameToUse(catalogName), schemaNameToUse(schemaName), tableNameToUse(tableName), null);
			while (tables != null && tables.next()) {
				TableMetaData tmd = new TableMetaData(tables.getString("TABLE_CAT"),
						tables.getString("TABLE_SCHEM"), tables.getString("TABLE_NAME"));
				if (tmd.schemaName() == null) {
					tableMeta.put(this.userName != null ? this.userName.toUpperCase() : "", tmd);
				}
				else {
					tableMeta.put(tmd.schemaName().toUpperCase(), tmd);
				}
			}
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error while accessing table meta-data results: " + ex.getMessage());
			}
		}
		finally {
			JdbcUtils.closeResultSet(tables);
		}

		if (tableMeta.isEmpty()) {
			if (logger.isInfoEnabled()) {
				logger.info("Unable to locate table meta-data for '" + tableName + "': column names must be provided");
			}
		}
		else {
			processTableColumns(databaseMetaData, findTableMetaData(schemaName, tableName, tableMeta));
		}
	}

	private TableMetaData findTableMetaData(@Nullable String schemaName, @Nullable String tableName,
			Map<String, TableMetaData> tableMeta) {

		if (schemaName != null) {
			TableMetaData tmd = tableMeta.get(schemaName.toUpperCase());
			if (tmd == null) {
				throw new DataAccessResourceFailureException("Unable to locate table meta-data for '" +
						tableName + "' in the '" + schemaName + "' schema");
			}
			return tmd;
		}
		else if (tableMeta.size() == 1) {
			return tableMeta.values().iterator().next();
		}
		else {
			TableMetaData tmd = tableMeta.get(getDefaultSchema());
			if (tmd == null) {
				tmd = tableMeta.get(this.userName != null ? this.userName.toUpperCase() : "");
			}
			if (tmd == null) {
				tmd = tableMeta.get("PUBLIC");
			}
			if (tmd == null) {
				tmd = tableMeta.get("DBO");
			}
			if (tmd == null) {
				throw new DataAccessResourceFailureException(
						"Unable to locate table meta-data for '" + tableName + "' in the default schema");
			}
			return tmd;
		}
	}

	/**
	 * Method supporting the meta-data processing for a table's columns.
	 */
	private void processTableColumns(DatabaseMetaData databaseMetaData, TableMetaData tmd) {
		ResultSet tableColumns = null;
		String metaDataCatalogName = metaDataCatalogNameToUse(tmd.catalogName());
		String metaDataSchemaName = metaDataSchemaNameToUse(tmd.schemaName());
		String metaDataTableName = tableNameToUse(tmd.tableName());
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieving meta-data for " + metaDataCatalogName + '/' +
					metaDataSchemaName + '/' + metaDataTableName);
		}
		try {
			tableColumns = databaseMetaData.getColumns(
					metaDataCatalogName, metaDataSchemaName, metaDataTableName, null);
			while (tableColumns.next()) {
				String columnName = tableColumns.getString("COLUMN_NAME");
				int dataType = tableColumns.getInt("DATA_TYPE");
				if (dataType == Types.DECIMAL) {
					String typeName = tableColumns.getString("TYPE_NAME");
					int decimalDigits = tableColumns.getInt("DECIMAL_DIGITS");
					// Override a DECIMAL data type for no-decimal numerics
					// (this is for better Oracle support where there have been issues
					// using DECIMAL for certain inserts (see SPR-6912))
					if ("NUMBER".equals(typeName) && decimalDigits == 0) {
						dataType = Types.NUMERIC;
						if (logger.isDebugEnabled()) {
							logger.debug("Overriding meta-data: " + columnName + " now NUMERIC instead of DECIMAL");
						}
					}
				}
				boolean nullable = tableColumns.getBoolean("NULLABLE");
				TableParameterMetaData meta = new TableParameterMetaData(columnName, dataType, nullable);
				this.tableParameterMetaData.add(meta);
				if (logger.isDebugEnabled()) {
					logger.debug("Retrieved meta-data: '" + meta.getParameterName() + "', sqlType=" +
							meta.getSqlType() + ", nullable=" + meta.isNullable());
				}
			}
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error while retrieving meta-data for table columns. " +
						"Consider specifying explicit column names -- for example, via SimpleJdbcInsert#usingColumns().",
						ex);
			}
			// Clear the metadata so that we don't retain a partial list of column names
			this.tableParameterMetaData.clear();
		}
		finally {
			JdbcUtils.closeResultSet(tableColumns);
		}
	}


	/**
	 * Record representing table meta-data.
	 */
	private record TableMetaData(@Nullable String catalogName, @Nullable String schemaName,
			@Nullable String tableName) {
	}

}
