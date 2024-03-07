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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * A generic implementation of the {@link CallMetaDataProvider} interface.
 *
 * <p>This class can be extended to provide database specific behavior.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 2.5
 */
public class GenericCallMetaDataProvider implements CallMetaDataProvider {

	/** Logger available to subclasses. */
	protected static final Log logger = LogFactory.getLog(CallMetaDataProvider.class);


	private final String userName;

	private boolean procedureColumnMetaDataUsed = false;

	private boolean supportsCatalogsInProcedureCalls = true;

	private boolean supportsSchemasInProcedureCalls = true;

	private boolean storesUpperCaseIdentifiers = true;

	private boolean storesLowerCaseIdentifiers = false;

	private final List<CallParameterMetaData> callParameterMetaData = new ArrayList<>();


	/**
	 * Constructor used to initialize with provided database meta-data.
	 * @param databaseMetaData meta-data to be used
	 */
	protected GenericCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		this.userName = databaseMetaData.getUserName();
	}


	@Override
	public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
		try {
			setSupportsCatalogsInProcedureCalls(databaseMetaData.supportsCatalogsInProcedureCalls());
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error retrieving 'DatabaseMetaData.supportsCatalogsInProcedureCalls': " + ex.getMessage());
			}
		}
		try {
			setSupportsSchemasInProcedureCalls(databaseMetaData.supportsSchemasInProcedureCalls());
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error retrieving 'DatabaseMetaData.supportsSchemasInProcedureCalls': " + ex.getMessage());
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
	public void initializeWithProcedureColumnMetaData(DatabaseMetaData databaseMetaData, @Nullable String catalogName,
			@Nullable String schemaName, @Nullable String procedureName) throws SQLException {

		this.procedureColumnMetaDataUsed = true;
		processProcedureColumns(databaseMetaData, catalogName, schemaName, procedureName);
	}

	@Override
	public List<CallParameterMetaData> getCallParameterMetaData() {
		return this.callParameterMetaData;
	}

	@Override
	@Nullable
	public String procedureNameToUse(@Nullable String procedureName) {
		return identifierNameToUse(procedureName);
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

	@Override
	@Nullable
	public String metaDataCatalogNameToUse(@Nullable String catalogName) {
		if (isSupportsCatalogsInProcedureCalls()) {
			return catalogNameToUse(catalogName);
		}
		else {
			return null;
		}
	}

	@Override
	@Nullable
	public String metaDataSchemaNameToUse(@Nullable String schemaName) {
		if (isSupportsSchemasInProcedureCalls()) {
			return schemaNameToUse(schemaName);
		}
		else {
			return null;
		}
	}

	@Override
	@Nullable
	public String parameterNameToUse(@Nullable String parameterName) {
		return identifierNameToUse(parameterName);
	}

	@Override
	public String namedParameterBindingToUse(@Nullable String parameterName) {
		return parameterName + " => ?";
	}

	@Override
	public SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta) {
		return new SqlOutParameter(parameterName, meta.getSqlType());
	}

	@Override
	public SqlParameter createDefaultInOutParameter(String parameterName, CallParameterMetaData meta) {
		return new SqlInOutParameter(parameterName, meta.getSqlType());
	}

	@Override
	public SqlParameter createDefaultInParameter(String parameterName, CallParameterMetaData meta) {
		return new SqlParameter(parameterName, meta.getSqlType());
	}

	@Override
	public String getUserName() {
		return this.userName;
	}

	@Override
	public boolean isProcedureColumnMetaDataUsed() {
		return this.procedureColumnMetaDataUsed;
	}

	@Override
	public boolean isReturnResultSetSupported() {
		return true;
	}

	@Override
	public boolean isRefCursorSupported() {
		return false;
	}

	@Override
	public int getRefCursorSqlType() {
		return Types.OTHER;
	}

	@Override
	public boolean byPassReturnParameter(String parameterName) {
		return false;
	}

	/**
	 * Specify whether the database supports the use of catalog name in procedure calls.
	 */
	protected void setSupportsCatalogsInProcedureCalls(boolean supportsCatalogsInProcedureCalls) {
		this.supportsCatalogsInProcedureCalls = supportsCatalogsInProcedureCalls;
	}

	/**
	 * Does the database support the use of catalog name in procedure calls?
	 */
	@Override
	public boolean isSupportsCatalogsInProcedureCalls() {
		return this.supportsCatalogsInProcedureCalls;
	}

	/**
	 * Specify whether the database supports the use of schema name in procedure calls.
	 */
	protected void setSupportsSchemasInProcedureCalls(boolean supportsSchemasInProcedureCalls) {
		this.supportsSchemasInProcedureCalls = supportsSchemasInProcedureCalls;
	}

	/**
	 * Does the database support the use of schema name in procedure calls?
	 */
	@Override
	public boolean isSupportsSchemasInProcedureCalls() {
		return this.supportsSchemasInProcedureCalls;
	}

	/**
	 * Specify whether the database uses upper case for identifiers.
	 */
	protected void setStoresUpperCaseIdentifiers(boolean storesUpperCaseIdentifiers) {
		this.storesUpperCaseIdentifiers = storesUpperCaseIdentifiers;
	}

	/**
	 * Does the database use upper case for identifiers?
	 */
	protected boolean isStoresUpperCaseIdentifiers() {
		return this.storesUpperCaseIdentifiers;
	}

	/**
	 * Specify whether the database uses lower case for identifiers.
	 */
	protected void setStoresLowerCaseIdentifiers(boolean storesLowerCaseIdentifiers) {
		this.storesLowerCaseIdentifiers = storesLowerCaseIdentifiers;
	}

	/**
	 * Does the database use lower case for identifiers?
	 */
	protected boolean isStoresLowerCaseIdentifiers() {
		return this.storesLowerCaseIdentifiers;
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

	/**
	 * Process the procedure column meta-data.
	 */
	private void processProcedureColumns(DatabaseMetaData databaseMetaData,
			@Nullable String catalogName, @Nullable String schemaName, @Nullable String procedureName) {

		String metaDataCatalogName = metaDataCatalogNameToUse(catalogName);
		String metaDataSchemaName = metaDataSchemaNameToUse(schemaName);
		String metaDataProcedureName = procedureNameToUse(procedureName);
		try {
			ProcedureMetadata procedureMetadata = getProcedureMetadata(databaseMetaData,
					metaDataCatalogName, metaDataSchemaName, metaDataProcedureName);
			if (procedureMetadata.hits() > 1) {
				// Try again with exact match in case of placeholders
				String searchStringEscape = databaseMetaData.getSearchStringEscape();
				if (searchStringEscape != null) {
					procedureMetadata = getProcedureMetadata(databaseMetaData, metaDataCatalogName,
							escapeNamePattern(metaDataSchemaName, searchStringEscape),
							escapeNamePattern(metaDataProcedureName, searchStringEscape));
				}
			}
			if (procedureMetadata.hits() == 0) {
				// Functions not exposed as procedures anymore on PostgreSQL driver 42.2.11
				procedureMetadata = getProcedureMetadataAsFunction(databaseMetaData,
						metaDataCatalogName, metaDataSchemaName, metaDataProcedureName);
				if (procedureMetadata.hits() > 1) {
					// Try again with exact match in case of placeholders
					String searchStringEscape = databaseMetaData.getSearchStringEscape();
					if (searchStringEscape != null) {
						procedureMetadata = getProcedureMetadataAsFunction(
								databaseMetaData, metaDataCatalogName,
								escapeNamePattern(metaDataSchemaName, searchStringEscape),
								escapeNamePattern(metaDataProcedureName, searchStringEscape));
					}
				}
			}
			// Handling matches

			boolean isFunction = procedureMetadata.function();
			List<String> matches = procedureMetadata.matches;
			if (matches.size() > 1) {
				throw new InvalidDataAccessApiUsageException(
						"Unable to determine the correct call signature - multiple signatures for '" +
						metaDataProcedureName + "': found " + matches + " " + (isFunction ? "functions" : "procedures"));
			}
			else if (matches.isEmpty()) {
				if (metaDataProcedureName != null && metaDataProcedureName.contains(".") &&
						!StringUtils.hasText(metaDataCatalogName)) {
					String packageName = metaDataProcedureName.substring(0, metaDataProcedureName.indexOf('.'));
					throw new InvalidDataAccessApiUsageException(
							"Unable to determine the correct call signature for '" + metaDataProcedureName +
							"' - package name should be specified separately using '.withCatalogName(\"" +
							packageName + "\")'");
				}
				else if ("Oracle".equals(databaseMetaData.getDatabaseProductName())) {
					if (logger.isDebugEnabled()) {
						logger.debug("Oracle JDBC driver did not return procedure/function/signature for '" +
								metaDataProcedureName + "' - assuming a non-exposed synonym");
					}
				}
				else {
					throw new InvalidDataAccessApiUsageException(
							"Unable to determine the correct call signature - no " +
							"procedure/function/signature for '" + metaDataProcedureName + "'");
				}
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Retrieving column meta-data for " + (isFunction ? "function" : "procedure") + ' ' +
						metaDataCatalogName + '/' + procedureMetadata.schemaName + '/' + procedureMetadata.procedureName);
			}
			try (ResultSet columns = isFunction ?
					databaseMetaData.getFunctionColumns(metaDataCatalogName, procedureMetadata.schemaName, procedureMetadata.procedureName, null) :
					databaseMetaData.getProcedureColumns(metaDataCatalogName, procedureMetadata.schemaName, procedureMetadata.procedureName, null)) {
				while (columns.next()) {
					String columnName = columns.getString("COLUMN_NAME");
					int columnType = columns.getInt("COLUMN_TYPE");
					if (columnName == null && isInOrOutColumn(columnType, isFunction)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Skipping meta-data for: " + columnType + " " + columns.getInt("DATA_TYPE") +
									" " + columns.getString("TYPE_NAME") + " " + columns.getInt("NULLABLE") +
									" (probably a member of a collection)");
						}
					}
					else {
						int nullable = (isFunction ? DatabaseMetaData.functionNullable : DatabaseMetaData.procedureNullable);
						CallParameterMetaData meta = new CallParameterMetaData(isFunction, columnName, columnType,
								columns.getInt("DATA_TYPE"), columns.getString("TYPE_NAME"),
								columns.getInt("NULLABLE") == nullable);
						this.callParameterMetaData.add(meta);
						if (logger.isDebugEnabled()) {
							logger.debug("Retrieved meta-data: " + meta.getParameterName() + " " +
									meta.getParameterType() + " " + meta.getSqlType() + " " +
									meta.getTypeName() + " " + meta.isNullable());
						}
					}
				}
			}
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error while retrieving meta-data for procedure columns. " +
						"Consider declaring explicit parameters -- for example, via SimpleJdbcCall#addDeclaredParameter().",
						ex);
			}
			// Although we could invoke `this.callParameterMetaData.clear()` so that
			// we don't retain a partial list of column names (like we do in
			// GenericTableMetaDataProvider.processTableColumns(...)), we choose
			// not to do that here, since invocation of the stored procedure will
			// likely fail anyway with an incorrect argument list.
		}
	}

	private ProcedureMetadata getProcedureMetadata(DatabaseMetaData databaseMetaData,
			@Nullable String catalogName, @Nullable String schemaName, @Nullable String procedureName) throws SQLException {
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieving meta-data for " + catalogName + '/' + schemaName + '/' + procedureName);
		}
		List<String> matches = new ArrayList<>();
		try (ResultSet procedures = databaseMetaData.getProcedures(catalogName, schemaName, procedureName)) {
			while (procedures.next()) {
				matches.add(procedures.getString("PROCEDURE_CAT") + '.' + procedures.getString("PROCEDURE_SCHEM") +
						'.' + procedures.getString("PROCEDURE_NAME"));
			}
		}
		return new ProcedureMetadata(schemaName, procedureName, matches, false);
	}

	private ProcedureMetadata getProcedureMetadataAsFunction(DatabaseMetaData databaseMetaData,
			@Nullable String catalogName, @Nullable String schemaName, @Nullable String procedureName) throws SQLException {
		if (logger.isDebugEnabled()) {
			logger.debug("Fallback on retrieving function meta-data for " + catalogName + '/' + schemaName + '/' + procedureName);
		}
		List<String> matches = new ArrayList<>();
		try (ResultSet functions = databaseMetaData.getFunctions(catalogName, schemaName, procedureName)) {
			while (functions.next()) {
				matches.add(functions.getString("FUNCTION_CAT") + '.' + functions.getString("FUNCTION_SCHEM") +
						'.' + functions.getString("FUNCTION_NAME"));
			}
		}
		return new ProcedureMetadata(schemaName, procedureName, matches, true);
	}

	@Nullable
	private static String escapeNamePattern(@Nullable String name, @Nullable String escape) {
		if (name == null || escape == null) {
			return name;
		}
		return name.replace(escape, escape + escape)
					.replace("_", escape + "_")
					.replace("%", escape + "%");
	}

	private static boolean isInOrOutColumn(int columnType, boolean function) {
		if (function) {
			return (columnType == DatabaseMetaData.functionColumnIn ||
					columnType == DatabaseMetaData.functionColumnInOut ||
					columnType == DatabaseMetaData.functionColumnOut);
		}
		else {
			return (columnType == DatabaseMetaData.procedureColumnIn ||
					columnType == DatabaseMetaData.procedureColumnInOut ||
					columnType == DatabaseMetaData.procedureColumnOut);
		}
	}

	private record ProcedureMetadata(@Nullable String schemaName, @Nullable String procedureName,
			List<String> matches, boolean function) {

		int hits() {
			return this.matches.size();
		}
	}

}
