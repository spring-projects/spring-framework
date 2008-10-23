/*
 * Copyright 2002-2007 the original author or authors.
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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * A generic implementation of the {@link TableMetaDataProvider} that should provide enough features for all supported
 * databases.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public class GenericTableMetaDataProvider implements TableMetaDataProvider {

	/** Logger available to subclasses */
	protected static final Log logger = LogFactory.getLog(TableMetaDataProvider.class);

	/** indicator whether column metadata should be used */
	private boolean tableColumnMetaDataUsed = false;

	/** the version of the database */
	private String databaseVersion;

	/** the name of the user currently connected */
	private String userName;

	/** indicates whether the identifiers are uppercased */
	private boolean storesUpperCaseIdentifiers = true;

	/** indicates whether the identifiers are lowercased */
	private boolean storesLowerCaseIdentifiers = false;

	/** indicates whether generated keys retrieval is supported */
	private boolean getGeneratedKeysSupported = true;

	/** indicates whether the use of a String[] for generated keys is supported */
	private boolean generatedKeysColumnNameArraySupported = true;

	/** database product we know that don't support the use of a String[] for generated keys */
	private List productsNotSupportingGeneratedKeysColumnNameArray = Arrays.asList(new String[] {"Apache Derby"});

	/** Collection of TableParameterMetaData objects */
	private List<TableParameterMetaData> insertParameterMetaData = new ArrayList<TableParameterMetaData>();


	/**
	 * Constructor used to initialize with provided database meta data.
	 * @param databaseMetaData meta data to be used
	 * @throws SQLException
	 */
	protected GenericTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		userName = databaseMetaData.getUserName();
	}

	/**
	 * Get whether identifiers use upper case
	 */
	public boolean isStoresUpperCaseIdentifiers() {
		return storesUpperCaseIdentifiers;
	}

	/**
	 * Specify whether identifiers use upper case
	 */
	public void setStoresUpperCaseIdentifiers(boolean storesUpperCaseIdentifiers) {
		this.storesUpperCaseIdentifiers = storesUpperCaseIdentifiers;
	}

	/**
	 * Get whether identifiers use lower case
	 */
	public boolean isStoresLowerCaseIdentifiers() {
		return storesLowerCaseIdentifiers;
	}

	/**
	 * Specify whether identifiers use lower case
	 */
	public void setStoresLowerCaseIdentifiers(boolean storesLowerCaseIdentifiers) {
		this.storesLowerCaseIdentifiers = storesLowerCaseIdentifiers;
	}

	public boolean isTableColumnMetaDataUsed() {
		return tableColumnMetaDataUsed;
	}

	public List<TableParameterMetaData> getTableParameterMetaData() {
		return insertParameterMetaData;
	}

	public boolean isGetGeneratedKeysSupported() {
		return getGeneratedKeysSupported;
	}

	public boolean isGetGeneratedKeysSimulated(){
		return false;
	}

	public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return null;
	}

	/**
	 * Specify whether a column name array is supported for generated keys
	 */
	public void setGetGeneratedKeysSupported(boolean getGeneratedKeysSupported) {
		this.getGeneratedKeysSupported = getGeneratedKeysSupported;
	}

	public boolean isGeneratedKeysColumnNameArraySupported() {
		return generatedKeysColumnNameArraySupported;
	}

	/**
	 * Specify whether a column name array is supported for generated keys
	 */
	public void setGeneratedKeysColumnNameArraySupported(boolean generatedKeysColumnNameArraySupported) {
		this.generatedKeysColumnNameArraySupported = generatedKeysColumnNameArraySupported;
	}


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
		catch (SQLException se) {
			logger.warn("Error retrieving 'DatabaseMetaData.getGeneratedKeys' - " + se.getMessage());
		}
		try {
			String databaseProductName = databaseMetaData.getDatabaseProductName();
			if (productsNotSupportingGeneratedKeysColumnNameArray.contains(databaseProductName)) {
				logger.debug("GeneratedKeysColumnNameArray is not supported for " + databaseProductName);
				setGeneratedKeysColumnNameArraySupported(false);
			}
			else {
				logger.debug("GeneratedKeysColumnNameArray is supported for " + databaseProductName);
				setGeneratedKeysColumnNameArraySupported(true);
			}
		}
		catch (SQLException se) {
			logger.warn("Error retrieving 'DatabaseMetaData.getDatabaseProductName' - " + se.getMessage());
		}
		try {
			databaseVersion = databaseMetaData.getDatabaseProductVersion();
		}
		catch (SQLException se) {
			logger.warn("Error retrieving 'DatabaseMetaData.getDatabaseProductVersion' - " + se.getMessage());
		}
		try {
			setStoresUpperCaseIdentifiers(databaseMetaData.storesUpperCaseIdentifiers());
		}
		catch (SQLException se) {
			logger.warn("Error retrieving 'DatabaseMetaData.storesUpperCaseIdentifiers' - " + se.getMessage());
		}
		try {
			setStoresLowerCaseIdentifiers(databaseMetaData.storesLowerCaseIdentifiers());
		}
		catch (SQLException se) {
			logger.warn("Error retrieving 'DatabaseMetaData.storesLowerCaseIdentifiers' - " + se.getMessage());
		}

	}

	public void initializeWithTableColumnMetaData(DatabaseMetaData databaseMetaData, String catalogName, String schemaName, String tableName)
			throws SQLException {

		tableColumnMetaDataUsed = true;

		locateTableAndProcessMetaData(databaseMetaData, catalogName, schemaName, tableName);


	}

	public String tableNameToUse(String tableName) {
		if (tableName == null)
			return null;
		else if (isStoresUpperCaseIdentifiers())
			return tableName.toUpperCase();
		else if(isStoresLowerCaseIdentifiers())
			return tableName.toLowerCase();
		else
			return tableName;
	}

	public String catalogNameToUse(String catalogName) {
		if (catalogName == null)
			return null;
		else if (isStoresUpperCaseIdentifiers())
			return catalogName.toUpperCase();
		else if(isStoresLowerCaseIdentifiers())
			return catalogName.toLowerCase();
		else
		return catalogName;
	}

	public String schemaNameToUse(String schemaName) {
		if (schemaName == null)
			return null;
		else if (isStoresUpperCaseIdentifiers())
			return schemaName.toUpperCase();
		else if(isStoresLowerCaseIdentifiers())
			return schemaName.toLowerCase();
		else
		return schemaName;
	}

	public String metaDataCatalogNameToUse(String catalogName) {
		return catalogNameToUse(catalogName);
	}

	public String metaDataSchemaNameToUse(String schemaName) {
		if (schemaName == null) {
			return schemaNameToUse(userName);
		}
		return schemaNameToUse(schemaName);
	}


	/**
	 * Provide access to version info for subclasses 
	 */
	protected String getDatabaseVersion() {
		return databaseVersion;
	}

	/**
	 * Method supporting the metedata processing for a table
	 */
	private void locateTableAndProcessMetaData(DatabaseMetaData databaseMetaData, String catalogName, String schemaName, String tableName) {
		Map<String, TableMetaData> tableMeta = new HashMap<String, TableMetaData>();
		ResultSet tables = null;

		try {
			tables = databaseMetaData.getTables(
				catalogNameToUse(catalogName),
				schemaNameToUse(schemaName),
				tableNameToUse(tableName),
				null);
			while (tables != null && tables.next()) {
				TableMetaData tmd = new TableMetaData();
				tmd.setCatalogName(tables.getString("TABLE_CAT"));
				tmd.setSchemaName(tables.getString("TABLE_SCHEM"));
				tmd.setTableName(tables.getString("TABLE_NAME"));
				tmd.setType(tables.getString("TABLE_TYPE"));
				if (tmd.getSchemaName() == null) {
					tableMeta.put(userName.toUpperCase(), tmd);
				}
				else {
					tableMeta.put(tmd.getSchemaName().toUpperCase(), tmd);
				}
			}
		}
		catch (SQLException se) {
			logger.warn("Error while accessing table meta data results" + se.getMessage());
		}
		finally {
			if (tables != null) {
				try {
					tables.close();
				} catch (SQLException e) {
					logger.warn("Error while closing table meta data reults" + e.getMessage());
				}
			}
		}

		if (tableMeta.size() < 1) {
			logger.warn("Unable to locate table meta data for '" + tableName +"' -- column names must be provided");
		}
		else {
			TableMetaData tmd = null;
			if (schemaName == null) {
				tmd = tableMeta.get(userName.toUpperCase());
				if (tmd == null) {
					tmd = tableMeta.get("PUBLIC");
					if (tmd == null) {
						tmd = tableMeta.get("DBO");
					}
					if (tmd == null) {
						throw new DataAccessResourceFailureException("Unable to locate table meta data for '" + tableName + "' in the default schema");
					}
				}
			}
			else {
				tmd = tableMeta.get(schemaName.toUpperCase());
				if (tmd == null) {
					throw new DataAccessResourceFailureException("Unable to locate table meta data for '" + tableName + "' in the '" + schemaName + "' schema");
				}
			}

			processTableColumns(databaseMetaData, tmd);
		}
	}

	/**
	 * Method supporting the metedata processing for a table's columns
	 */
	private void processTableColumns(DatabaseMetaData databaseMetaData, TableMetaData tmd) {
		ResultSet tableColumns = null;
		String metaDataCatalogName = metaDataCatalogNameToUse(tmd.getCatalogName());
		String metaDataSchemaName = metaDataSchemaNameToUse(tmd.getSchemaName());
		String metaDataTableName = tableNameToUse(tmd.getTableName());
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieving metadata for " + metaDataCatalogName + "/" +
					metaDataSchemaName + "/" + metaDataTableName);
		}
		try {
			tableColumns = databaseMetaData.getColumns(
					metaDataCatalogName,
					metaDataSchemaName,
					metaDataTableName,
					null);
			while (tableColumns.next()) {
				TableParameterMetaData meta = new TableParameterMetaData(
						tableColumns.getString("COLUMN_NAME"),
						tableColumns.getInt("DATA_TYPE"),
						tableColumns.getBoolean("NULLABLE")
				);
				insertParameterMetaData.add(meta);
				if (logger.isDebugEnabled()) {
					logger.debug("Retrieved metadata: "
						+ meta.getParameterName() +
						" " + meta.getSqlType() +
						" " + meta.isNullable()
					);
				}
			}
		}
		catch (SQLException se) {
			logger.warn("Error while retreiving metadata for procedure columns: " + se.getMessage());
		}
		finally {
			try {
				if (tableColumns != null)
					tableColumns.close();
			}
			catch (SQLException se) {
				logger.warn("Problem closing resultset for procedure column metadata " + se.getMessage());
			}
		}

	}


	/**
	 * Class representing table meta data
	 */
	private class TableMetaData {
		private String catalogName;
		private String schemaName;
		private String tableName;
		private String type;


		public String getCatalogName() {
			return catalogName;
		}

		public void setCatalogName(String catalogName) {
			this.catalogName = catalogName;
		}

		public String getSchemaName() {
			return schemaName;
		}

		public void setSchemaName(String schemaName) {
			this.schemaName = schemaName;
		}

		public String getTableName() {
			return tableName;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}
}
