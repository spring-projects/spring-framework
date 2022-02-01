/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Oracle-specific implementation for the {@link CallMetaDataProvider} interface.
 * This class is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @author Loïc Lefèvre
 * @since 2.5
 */
public class OracleCallMetaDataProvider extends GenericCallMetaDataProvider {

	private static final String REF_CURSOR_NAME = "REF CURSOR";

	public OracleCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public boolean isReturnResultSetSupported() {
		return false;
	}

	@Override
	public boolean isRefCursorSupported() {
		return true;
	}

	@Override
	public int getRefCursorSqlType() {
		return -10;
	}

	@Override
	@Nullable
	public String metaDataCatalogNameToUse(@Nullable String catalogName) {
		// Oracle uses catalog name for package name or an empty string if no package
		return (catalogName == null ? "" : catalogNameToUse(catalogName));
	}

	@Override
	@Nullable
	public String metaDataSchemaNameToUse(@Nullable String schemaName) {
		// Use current user schema if no schema specified
		return (schemaName == null ? getUserName() : super.metaDataSchemaNameToUse(schemaName));
	}

	@Override
	public SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta) {
		if (meta.getSqlType() == Types.OTHER && REF_CURSOR_NAME.equals(meta.getTypeName())) {
			return new SqlOutParameter(parameterName, getRefCursorSqlType(), new ColumnMapRowMapper());
		}
		else {
			return super.createDefaultOutParameter(parameterName, meta);
		}
	}

	@Override
	protected void processProcedureColumns(DatabaseMetaData databaseMetaData,
										final CallMetaDataContext context, final List<SqlParameter> parameters) {
		String metaDataCatalogName = metaDataCatalogNameToUse(context.getCatalogName());
		String metaDataSchemaName = metaDataSchemaNameToUse(context.getSchemaName());
		String metaDataProcedureName = procedureNameToUse(context.getProcedureName());
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieving column meta-data for " + (context.isFunction() ? "function" : "procedure") + ' ' +
					metaDataCatalogName + '/' + metaDataSchemaName + '/' + metaDataProcedureName);
		}

		try {
			int objectsFound = 0;

			if (context.isFunction()) {
				try (ResultSet functions = databaseMetaData.getFunctions(
						metaDataCatalogName, metaDataSchemaName, metaDataProcedureName)) {
					while (functions.next()) {
						objectsFound++;
					}
				}
			}
			else {
				try (ResultSet procedures = databaseMetaData.getProcedures(
						metaDataCatalogName, metaDataSchemaName, metaDataProcedureName)) {
					while (procedures.next()) {
						objectsFound++;
					}
				}
			}

			if (objectsFound == 0) {
				if (metaDataProcedureName != null && metaDataProcedureName.contains(".") &&
						!StringUtils.hasText(metaDataCatalogName)) {
					String packageName = metaDataProcedureName.substring(0, metaDataProcedureName.indexOf('.'));
					throw new InvalidDataAccessApiUsageException(
							"Unable to determine the correct call signature for '" + metaDataProcedureName +
									"' - package name should be specified separately using '.withCatalogName(\"" +
									packageName + "\")'");
				}
				else if (!"SYS".equalsIgnoreCase(metaDataSchemaName) &&
						metaDataCatalogName != null && metaDataCatalogName.toLowerCase().startsWith("dbms_")) {
					if (logger.isDebugEnabled()) {
						logger.debug("Chances are that the package name '" + metaDataCatalogName +
								"' is owned by internal Oracle 'SYS' user - try using '.withSchemaName(\"SYS\")'");
					}
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Oracle JDBC driver did not return procedure/function/signature for '" +
								metaDataProcedureName + "' - assuming a non-exposed synonym");
					}
				}
			}

			try (ResultSet columns = context.isFunction() ?
					databaseMetaData.getFunctionColumns(metaDataCatalogName, metaDataSchemaName, metaDataProcedureName, null) :
					databaseMetaData.getProcedureColumns(metaDataCatalogName, metaDataSchemaName, metaDataProcedureName, null)) {

				boolean overloadColumnPresent = false;
				for (int i = 1; i <= columns.getMetaData().getColumnCount(); i++) {
					if ("OVERLOAD".equals(columns.getMetaData().getColumnName(i))) {
						overloadColumnPresent = true;
						break;
					}
				}

				if (objectsFound > 1 && !overloadColumnPresent) {
					throw new InvalidDataAccessApiUsageException(
							"Unable to determine the correct call signature - multiple signatures for '" +
							metaDataProcedureName + "': found " + objectsFound + " " +
							(context.isFunction() ? "functions" : "procedures"));
				}

				while (columns.next()) {
					String columnName = columns.getString("COLUMN_NAME");
					int columnType = columns.getInt("COLUMN_TYPE");
					if (columnName == null && isInOrOutColumn(columnType, context.isFunction())) {
						if (logger.isDebugEnabled()) {
							logger.debug("Skipping meta-data for: " + columnType + " " + columns.getInt("DATA_TYPE") +
									" " + columns.getString("TYPE_NAME") + " " + columns.getInt("NULLABLE") +
									" (probably a member of a collection)");
						}
					}
					else {
						int nullable = (context.isFunction() ? DatabaseMetaData.functionNullable : DatabaseMetaData.procedureNullable);
						CallParameterMetaData meta = overloadColumnPresent ?
								new OracleCallParameterMetaData(context.isFunction(), columnName, columnType,
								columns.getInt("DATA_TYPE"), columns.getString("TYPE_NAME"),
								columns.getInt("NULLABLE") == nullable, columns.getInt("OVERLOAD")) :
								new CallParameterMetaData(context.isFunction(), columnName, columnType,
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

				if (objectsFound > 1) {
					if (parameters.isEmpty()) {
						throw new InvalidDataAccessApiUsageException(
								"Unable to determine the correct call signature - multiple signatures for '" +
								metaDataProcedureName + "': found " + objectsFound + " " +
								(context.isFunction() ? "functions" : "procedures"));
					}

					// now work on the different overloads and keep only the one matching the requested parameters
					// this ensures that no issue happens at runtime (in production)
					// although this check can bring a performance penalty if used intensively
					int currentOverload;
					int overloadStartIndex = 0;
					List<CallParameterMetaData> newCallParameterMetaData = new ArrayList<>();
					boolean signatureFound = false;
					for (int i = 0; i < this.callParameterMetaData.size(); i++) {
						OracleCallParameterMetaData meta = (OracleCallParameterMetaData) callParameterMetaData.get(i);
						currentOverload = meta.overload;

						boolean allMatched = true;
						for (SqlParameter sqlParameter : parameters) {
							if (sqlParameter.getSqlType() == meta.getSqlType() && (meta.getParameterName() == null ||
								meta.getParameterName().equalsIgnoreCase(sqlParameter.getName()))) {
								if (i < this.callParameterMetaData.size() - 1) {
									i++;
									meta = (OracleCallParameterMetaData) callParameterMetaData.get(i);
								}
								else {
									break;
								}
							}
							else {
								allMatched = false;
								break;
							}
						}

						if (allMatched) {
							// Matching overload starts at index overloadStartIndex
							for (int j = 0; j < parameters.size(); j++) {
								newCallParameterMetaData.add(this.callParameterMetaData.get(j+overloadStartIndex));
							}
							signatureFound = true;
							break;
						}
						else {
							// get to the next overload
							while (i < this.callParameterMetaData.size()) {
								i++;
								meta = (OracleCallParameterMetaData) callParameterMetaData.get(i);
								if (currentOverload < meta.overload) {
									overloadStartIndex = i;
									i--;
									break;
								}
							}
						}
					}

					if (signatureFound) {
						// replace all the parameters meta-data with the right overload ones
						this.callParameterMetaData.clear();
						this.callParameterMetaData.addAll(newCallParameterMetaData);
					}
					else {
						throw new InvalidDataAccessApiUsageException(
								"Unable to determine the correct call signature - multiple signatures for '" +
								metaDataProcedureName + "': found " + objectsFound + " " +
								(context.isFunction() ? "functions" : "procedures"));
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
}
