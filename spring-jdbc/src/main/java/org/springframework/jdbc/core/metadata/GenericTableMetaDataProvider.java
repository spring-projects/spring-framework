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

	/** indicator whether column meta-data should be used. */
	private boolean tableColumnMetaDataUsed = false;

	/** the version of the database. */
	@Nullable
	private String databaseVersion;

	/** the name of the user currently connected. */
	@Nullable
	private final String userName;

	/** indicates whether the identifiers are uppercased. */
	private boolean storesUpperCaseIdentifiers = true;

	/** indicates whether the identifiers are lowercased. */
	private boolean storesLowerCaseIdentifiers = false;

	/** indicates whether generated keys retrieval is supported. */
	private boolean getGeneratedKeysSupported = true;

	/** indicates whether the use of a String[] for generated keys is supported. */
	private boolean generatedKeysColumnNameArraySupported = true;

	/** database products we know not supporting the use of a String[] for generated keys. */
	private final List<String> productsNotSupportingGeneratedKeysColumnNameArray =
			Arrays.asList("Apache Derby", "HSQL Database Engine");

	/** Collection of TableParameterMetaData objects. */
	private final List<TableParameterMetaData> tableParameterMetaData = new ArrayList<>();


	/**
	 * Constructor used to initialize with provided database meta-data.
	 * @param databaseMetaData meta-data to be used
	 */
	protected GenericTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		this.userName = databaseMetaData.getUserName();
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
	public boolean isTableColumnMetaDataUsed() {
		return this.tableColumnMetaDataUsed;
	}

	@Override
	public List<TableParameterMetaData> getTableParameterMetaData() {
		return this.tableParameterMetaData;
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

	public void setGetGeneratedKeysSupported(boolean getGeneratedKeysSupported) {
		this.getGeneratedKeysSupported = getGeneratedKeysSupported;
	}

	public void setGeneratedKeysColumnNameArraySupported(boolean generatedKeysColumnNameArraySupported) {
		this.generatedKeysColumnNameArraySupported = generatedKeysColumnNameArraySupported;
	}

	@Override
	public boolean isGeneratedKeysColumnNameArraySupported() {
		return this.generatedKeysColumnNameArraySupported;
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
				logger.warn("Error retrieving 'DatabaseMetaData.getGeneratedKeys': " + ex.getMessage());
			}
		}
		try {
			String databaseProductName = databaseMetaData.getDatabaseProductName();
			if (this.productsNotSupportingGeneratedKeysColumnNameArray.contains(databaseProductName)) {
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
	}

	@Override
	public void initializeWithTableColumnMetaData(DatabaseMetaData databaseMetaData, @Nullable String catalogName,
			@Nullable String schemaName, @Nullable String tableName) throws SQLException {

		this.tableColumnMetaDataUsed = true;
		locateTableAndProcessMetaData(databaseMetaData, catalogName, schemaName, tableName);
	}

	@Override
	@Nullable
	public String tableNameToUse(@Nullable String tableName) {
		if (tableName == null) {
			return null;
		}
		else if (isStoresUpperCaseIdentifiers()) {
			return tableName.toUpperCase();
		}
		else if (isStoresLowerCaseIdentifiers()) {
			return tableName.toLowerCase();
		}
		else {
			return tableName;
		}
	}

	@Override
	@Nullable
	public String catalogNameToUse(@Nullable String catalogName) {
		if (catalogName == null) {
			return null;
		}
		else if (isStoresUpperCaseIdentifiers()) {
			return catalogName.toUpperCase();
		}
		else if (isStoresLowerCaseIdentifiers()) {
			return catalogName.toLowerCase();
		}
		else {
			return catalogName;
		}
	}

	@Override
	@Nullable
	public String schemaNameToUse(@Nullable String schemaName) {
		if (schemaName == null) {
			return null;
		}
		else if (isStoresUpperCaseIdentifiers()) {
			return schemaName.toUpperCase();
		}
		else if (isStoresLowerCaseIdentifiers()) {
			return schemaName.toLowerCase();
		}
		else {
			return schemaName;
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
	 * Provide access to default schema for subclasses.
	 */
	@Nullable
	protected String getDefaultSchema() {
		return this.userName;
	}

	/**
	 * Provide access to version info for subclasses.
	 */
	@Nullable
	protected String getDatabaseVersion() {
		return this.databaseVersion;
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
				TableMetaData tmd = new TableMetaData();
				tmd.setCatalogName(tables.getString("TABLE_CAT"));
				tmd.setSchemaName(tables.getString("TABLE_SCHEM"));
				tmd.setTableName(tables.getString("TABLE_NAME"));
				if (tmd.getSchemaName() == null) {
					tableMeta.put(this.userName != null ? this.userName.toUpperCase() : "", tmd);
				}
				else {
					tableMeta.put(tmd.getSchemaName().toUpperCase(), tmd);
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
		String metaDataCatalogName = metaDataCatalogNameToUse(tmd.getCatalogName());
		String metaDataSchemaName = metaDataSchemaNameToUse(tmd.getSchemaName());
		String metaDataTableName = tableNameToUse(tmd.getTableName());
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
	 * Class representing table meta-data.
	 */
	private static class TableMetaData {

		@Nullable
		private String catalogName;

		@Nullable
		private String schemaName;

		@Nullable
		private String tableName;

		public void setCatalogName(String catalogName) {
			this.catalogName = catalogName;
		}

		@Nullable
		public String getCatalogName() {
			return this.catalogName;
		}

		public void setSchemaName(String schemaName) {
			this.schemaName = schemaName;
		}

		@Nullable
		public String getSchemaName() {
			return this.schemaName;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		@Nullable
		public String getTableName() {
			return this.tableName;
		}
	}

}
