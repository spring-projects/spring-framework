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

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;

/**
 * Interface specifying the API to be implemented by a class providing table metedata.  This is intended for internal use
 * by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public interface TableMetaDataProvider {

	/**
	 * Initialize using the database metedata provided
	 * @param databaseMetaData
	 * @throws SQLException
	 */
	void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException;

	/**
	 * Initialize using provided database metadata, table and column information. This initalization can be
	 * turned off by specifying that column meta data should not be used.
	 * @param databaseMetaData used to retrieve database specific information
	 * @param catalogName name of catalog to use or null
	 * @param schemaName name of schema name to use or null
	 * @param tableName name of the table
	 * @throws SQLException
	 */
	void initializeWithTableColumnMetaData(DatabaseMetaData databaseMetaData, String catalogName, String schemaName, String tableName)
			throws SQLException;

	/**
	 * Get the table name formatted based on metadata information. This could include altering the case.
	 *
	 * @param tableName
	 * @return table name formatted
	 */
	String tableNameToUse(String tableName);

	/**
	 * Get the catalog name formatted based on metadata information. This could include altering the case.
	 *
	 * @param catalogName
	 * @return catalog name formatted
	 */
	String catalogNameToUse(String catalogName);

	/**
	 * Get the schema name formatted based on metadata information. This could include altering the case.
	 *
	 * @param schemaName
	 * @return schema name formatted
	 */
	String schemaNameToUse(String schemaName);

	/**
	 * Provide any modification of the catalog name passed in to match the meta data currently used.
	 * The reyurned value will be used for meta data lookups.  This could include alterig the case used or
	 * providing a base catalog if mone provided.
	 *
	 * @param catalogName
	 * @return catalog name to use
	 */
	String metaDataCatalogNameToUse(String catalogName) ;

	/**
	 * Provide any modification of the schema name passed in to match the meta data currently used.
	 * The reyurned value will be used for meta data lookups.  This could include alterig the case used or
	 * providing a base schema if mone provided.
	 *
	 * @param schemaName
	 * @return schema name to use
	 */
	String metaDataSchemaNameToUse(String schemaName) ;

	/**
	 * Are we using the meta data for the table columns?
	 */
 	boolean isTableColumnMetaDataUsed();

	/**
	 * Does this database support the JDBC 3.0 feature of retreiving generated keys
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}
	 */
 	boolean isGetGeneratedKeysSupported();

	/**
	 * Does this database support a simple quey to retrieve the generated key whe the JDBC 3.0 feature
	 * of retreiving generated keys is not supported
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}
	 */
 	boolean isGetGeneratedKeysSimulated();

	/**
	 * Get the simple query to retrieve a generated key
	 */
	String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName);

	/**
	 * Does this database support a column name String array for retreiving generated keys
	 * {@link java.sql.Connection#createStruct(String, Object[])}
	 */
 	boolean isGeneratedKeysColumnNameArraySupported();

	/**
	 * Get the table parameter metadata that is currently used.
	 * @return List of {@link TableParameterMetaData}
	 */
	List<TableParameterMetaData> getTableParameterMetaData();

	/**
	 * Set the {@link NativeJdbcExtractor} to use to retrieve the native connection if necessary
	 */
	void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor);
}
