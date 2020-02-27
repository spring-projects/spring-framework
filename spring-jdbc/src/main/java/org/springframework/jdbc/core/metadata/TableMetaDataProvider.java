/*
 * Copyright 2002-2018 the original author or authors.
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
import java.sql.SQLException;
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Interface specifying the API to be implemented by a class providing table meta-data.
 * This is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public interface TableMetaDataProvider {

	/**
	 * Initialize using the database meta-data provided.
	 * @param databaseMetaData used to retrieve database specific information
	 * @throws SQLException in case of initialization failure
	 */
	void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException;

	/**
	 * Initialize using provided database meta-data, table and column information.
	 * This initialization can be turned off by specifying that column meta-data should not be used.
	 * @param databaseMetaData used to retrieve database specific information
	 * @param catalogName name of catalog to use (or {@code null} if none)
	 * @param schemaName name of schema name to use (or {@code null} if none)
	 * @param tableName name of the table
	 * @throws SQLException in case of initialization failure
	 */
	void initializeWithTableColumnMetaData(DatabaseMetaData databaseMetaData, @Nullable String catalogName,
			@Nullable String schemaName, @Nullable String tableName) throws SQLException;

	/**
	 * Get the table name formatted based on meta-data information.
	 * This could include altering the case.
	 */
	@Nullable
	String tableNameToUse(@Nullable String tableName);

	/**
	 * Get the catalog name formatted based on meta-data information.
	 * This could include altering the case.
	 */
	@Nullable
	String catalogNameToUse(@Nullable String catalogName);

	/**
	 * Get the schema name formatted based on meta-data information.
	 * This could include altering the case.
	 */
	@Nullable
	String schemaNameToUse(@Nullable String schemaName);

	/**
	 * Provide any modification of the catalog name passed in to match the meta-data currently used.
	 * The returned value will be used for meta-data lookups.
	 * This could include altering the case used or providing a base catalog if none is provided.
	 */
	@Nullable
	String metaDataCatalogNameToUse(@Nullable String catalogName) ;

	/**
	 * Provide any modification of the schema name passed in to match the meta-data currently used.
	 * The returned value will be used for meta-data lookups.
	 * This could include altering the case used or providing a base schema if none is provided.
	 */
	@Nullable
	String metaDataSchemaNameToUse(@Nullable String schemaName) ;

	/**
	 * Are we using the meta-data for the table columns?
	 */
	boolean isTableColumnMetaDataUsed();

	/**
	 * Does this database support the JDBC 3.0 feature of retrieving generated keys:
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	boolean isGetGeneratedKeysSupported();

	/**
	 * Does this database support a simple query to retrieve the generated key when
	 * the JDBC 3.0 feature of retrieving generated keys is not supported?
	 * @see #isGetGeneratedKeysSupported()
	 */
	boolean isGetGeneratedKeysSimulated();

	/**
	 * Get the simple query to retrieve a generated key.
	 */
	@Nullable
	String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName);

	/**
	 * Does this database support a column name String array for retrieving generated keys:
	 * {@link java.sql.Connection#createStruct(String, Object[])}?
	 */
	boolean isGeneratedKeysColumnNameArraySupported();

	/**
	 * Get the table parameter meta-data that is currently used.
	 * @return a List of {@link TableParameterMetaData}
	 */
	List<TableParameterMetaData> getTableParameterMetaData();

}
