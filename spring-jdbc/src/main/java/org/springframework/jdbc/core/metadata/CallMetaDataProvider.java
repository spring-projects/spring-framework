/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.lang.Nullable;

/**
 * Interface specifying the API to be implemented by a class providing call meta-data.
 *
 * <p>This is intended for internal use by Spring's
 * {@link org.springframework.jdbc.core.simple.SimpleJdbcCall}.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public interface CallMetaDataProvider {

	/**
	 * Initialize using the provided DatabaseMetData.
	 * @param databaseMetaData used to retrieve database specific information
	 * @throws SQLException in case of initialization failure
	 */
	void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException;

	/**
	 * Initialize the database specific management of procedure column meta-data.
	 * This is only called for databases that are supported. This initialization
	 * can be turned off by specifying that column meta-data should not be used.
	 * @param databaseMetaData used to retrieve database specific information
	 * @param catalogName name of catalog to use (or {@code null} if none)
	 * @param schemaName name of schema name to use (or {@code null} if none)
	 * @param procedureName name of the stored procedure
	 * @throws SQLException in case of initialization failure
	 * @see	org.springframework.jdbc.core.simple.SimpleJdbcCall#withoutProcedureColumnMetaDataAccess()
	 */
	void initializeWithProcedureColumnMetaData(DatabaseMetaData databaseMetaData, @Nullable String catalogName,
			@Nullable String schemaName, @Nullable String procedureName) throws SQLException;

	/**
	 * Provide any modification of the procedure name passed in to match the meta-data currently used.
	 * This could include altering the case.
	 */
	@Nullable
	String procedureNameToUse(@Nullable String procedureName);

	/**
	 * Provide any modification of the catalog name passed in to match the meta-data currently used.
	 * This could include altering the case.
	 */
	@Nullable
	String catalogNameToUse(@Nullable String catalogName);

	/**
	 * Provide any modification of the schema name passed in to match the meta-data currently used.
	 * This could include altering the case.
	 */
	@Nullable
	String schemaNameToUse(@Nullable String schemaName);

	/**
	 * Provide any modification of the catalog name passed in to match the meta-data currently used.
	 * The returned value will be used for meta-data lookups. This could include altering the case
	 * used or providing a base catalog if none is provided.
	 */
	@Nullable
	String metaDataCatalogNameToUse(@Nullable String catalogName) ;

	/**
	 * Provide any modification of the schema name passed in to match the meta-data currently used.
	 * The returned value will be used for meta-data lookups. This could include altering the case
	 * used or providing a base schema if none is provided.
	 */
	@Nullable
	String metaDataSchemaNameToUse(@Nullable String schemaName);

	/**
	 * Provide any modification of the column name passed in to match the meta-data currently used.
	 * This could include altering the case.
	 * @param parameterName name of the parameter of column
	 */
	@Nullable
	String parameterNameToUse(@Nullable String parameterName);

	/**
	 * Create a default out parameter based on the provided meta-data.
	 * This is used when no explicit parameter declaration has been made.
	 * @param parameterName the name of the parameter
	 * @param meta meta-data used for this call
	 * @return the configured SqlOutParameter
	 */
	SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta);

	/**
	 * Create a default inout parameter based on the provided meta-data.
	 * This is used when no explicit parameter declaration has been made.
	 * @param parameterName the name of the parameter
	 * @param meta meta-data used for this call
	 * @return the configured SqlInOutParameter
	 */
	SqlParameter createDefaultInOutParameter(String parameterName, CallParameterMetaData meta);

	/**
	 * Create a default in parameter based on the provided meta-data.
	 * This is used when no explicit parameter declaration has been made.
	 * @param parameterName the name of the parameter
	 * @param meta meta-data used for this call
	 * @return the configured SqlParameter
	 */
	SqlParameter createDefaultInParameter(String parameterName, CallParameterMetaData meta);

	/**
	 * Get the name of the current user. Useful for meta-data lookups etc.
	 * @return current user name from database connection
	 */
	@Nullable
	String getUserName();

	/**
	 * Does this database support returning ResultSets that should be retrieved with the JDBC call:
	 * {@link java.sql.Statement#getResultSet()}?
	 */
	boolean isReturnResultSetSupported();

	/**
	 * Does this database support returning ResultSets as ref cursors to be retrieved with
	 * {@link java.sql.CallableStatement#getObject(int)} for the specified column.
	 */
	boolean isRefCursorSupported();

	/**
	 * Get the {@link java.sql.Types} type for columns that return ResultSets as ref cursors
	 * if this feature is supported.
	 */
	int getRefCursorSqlType();

	/**
	 * Are we using the meta-data for the procedure columns?
	 */
	boolean isProcedureColumnMetaDataUsed();

	/**
	 * Should we bypass the return parameter with the specified name.
	 * This allows the database specific implementation to skip the processing
	 * for specific results returned by the database call.
	 */
	boolean byPassReturnParameter(String parameterName);

	/**
	 * Get the call parameter meta-data that is currently used.
	 * @return List of {@link CallParameterMetaData}
	 */
	List<CallParameterMetaData> getCallParameterMetaData();

	/**
	 * Does the database support the use of catalog name in procedure calls?
	 */
	boolean isSupportsCatalogsInProcedureCalls();

	/**
	 * Does the database support the use of schema name in procedure calls?
	 */
	boolean isSupportsSchemasInProcedureCalls();

}
