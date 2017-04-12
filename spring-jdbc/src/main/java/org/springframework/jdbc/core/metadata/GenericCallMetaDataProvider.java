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
import org.springframework.util.StringUtils;

/**
 * Generic implementation for the {@link CallMetaDataProvider} interface.
 * This class can be extended to provide database specific behavior.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public class GenericCallMetaDataProvider implements CallMetaDataProvider {

	/** Logger available to subclasses */
	protected static final Log logger = LogFactory.getLog(CallMetaDataProvider.class);

	private boolean procedureColumnMetaDataUsed = false;

	private String userName;

	private boolean supportsCatalogsInProcedureCalls = true;

	private boolean supportsSchemasInProcedureCalls = true;

	private boolean storesUpperCaseIdentifiers = true;

	private boolean storesLowerCaseIdentifiers = false;

	private List<CallParameterMetaData> callParameterMetaData = new ArrayList<>();


	/**
	 * Constructor used to initialize with provided database meta data.
	 * @param databaseMetaData meta data to be used
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
	public void initializeWithProcedureColumnMetaData(DatabaseMetaData databaseMetaData, String catalogName,
			String schemaName, String procedureName) throws SQLException {

		this.procedureColumnMetaDataUsed = true;
		processProcedureColumns(databaseMetaData, catalogName, schemaName,  procedureName);
	}

	@Override
	public List<CallParameterMetaData> getCallParameterMetaData() {
		return this.callParameterMetaData;
	}

	@Override
	public String procedureNameToUse(String procedureName) {
		if (procedureName == null) {
			return null;
		}
		else if (isStoresUpperCaseIdentifiers()) {
			return procedureName.toUpperCase();
		}
		else if (isStoresLowerCaseIdentifiers()) {
			return procedureName.toLowerCase();
		}
		else {
			return procedureName;
		}
	}

	@Override
	public String catalogNameToUse(String catalogName) {
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
	public String schemaNameToUse(String schemaName) {
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
	public String metaDataCatalogNameToUse(String catalogName) {
		if (isSupportsCatalogsInProcedureCalls()) {
			return catalogNameToUse(catalogName);
		}
		else {
			return null;
		}
	}

	@Override
	public String metaDataSchemaNameToUse(String schemaName) {
		if (isSupportsSchemasInProcedureCalls()) {
			return schemaNameToUse(schemaName);
		}
		else {
			return null;
		}
	}

	@Override
	public String parameterNameToUse(String parameterName) {
		if (parameterName == null) {
			return null;
		}
		else if (isStoresUpperCaseIdentifiers()) {
			return parameterName.toUpperCase();
		}
		else if (isStoresLowerCaseIdentifiers()) {
			return parameterName.toLowerCase();
		}
		else {
			return parameterName;
		}
	}

	@Override
	public boolean byPassReturnParameter(String parameterName) {
		return false;
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
	public boolean isProcedureColumnMetaDataUsed() {
		return this.procedureColumnMetaDataUsed;
	}


	/**
	 * Specify whether the database supports the use of catalog name in procedure calls
	 */
	protected void setSupportsCatalogsInProcedureCalls(boolean supportsCatalogsInProcedureCalls) {
		this.supportsCatalogsInProcedureCalls = supportsCatalogsInProcedureCalls;
	}

	/**
	 * Does the database support the use of catalog name in procedure calls
	 */
	@Override
	public boolean isSupportsCatalogsInProcedureCalls() {
		return this.supportsCatalogsInProcedureCalls;
	}

	/**
	 * Specify whether the database supports the use of schema name in procedure calls
	 */
	protected void setSupportsSchemasInProcedureCalls(boolean supportsSchemasInProcedureCalls) {
		this.supportsSchemasInProcedureCalls = supportsSchemasInProcedureCalls;
	}

	/**
	 * Does the database support the use of schema name in procedure calls
	 */
	@Override
	public boolean isSupportsSchemasInProcedureCalls() {
		return this.supportsSchemasInProcedureCalls;
	}

	/**
	 * Specify whether the database uses upper case for identifiers
	 */
	protected void setStoresUpperCaseIdentifiers(boolean storesUpperCaseIdentifiers) {
		this.storesUpperCaseIdentifiers = storesUpperCaseIdentifiers;
	}

	/**
	 * Does the database use upper case for identifiers
	 */
	protected boolean isStoresUpperCaseIdentifiers() {
		return this.storesUpperCaseIdentifiers;
	}

	/**
	 * Specify whether the database uses lower case for identifiers
	 */
	protected void setStoresLowerCaseIdentifiers(boolean storesLowerCaseIdentifiers) {
		this.storesLowerCaseIdentifiers = storesLowerCaseIdentifiers;
	}

	/**
	 * Does the database use lower case for identifiers
	 */
	protected boolean isStoresLowerCaseIdentifiers() {
		return this.storesLowerCaseIdentifiers;
	}


	/**
	 * Process the procedure column metadata
	 */
	private void processProcedureColumns(
			DatabaseMetaData databaseMetaData, String catalogName, String schemaName, String procedureName) {

		String metaDataCatalogName = metaDataCatalogNameToUse(catalogName);
		String metaDataSchemaName = metaDataSchemaNameToUse(schemaName);
		String metaDataProcedureName = procedureNameToUse(procedureName);
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieving metadata for " + metaDataCatalogName + '/' +
					metaDataSchemaName + '/' + metaDataProcedureName);
		}

		ResultSet procs = null;
		try {
			procs = databaseMetaData.getProcedures(metaDataCatalogName, metaDataSchemaName, metaDataProcedureName);
			List<String> found = new ArrayList<>();
			while (procs.next()) {
				found.add(procs.getString("PROCEDURE_CAT") + '.' + procs.getString("PROCEDURE_SCHEM") +
						'.' + procs.getString("PROCEDURE_NAME"));
			}
			procs.close();

			if (found.size() > 1) {
				throw new InvalidDataAccessApiUsageException(
						"Unable to determine the correct call signature - multiple " +
						"procedures/functions/signatures for '" + metaDataProcedureName + "': found " + found);
			}
			else if (found.isEmpty()) {
				if (metaDataProcedureName.contains(".") && !StringUtils.hasText(metaDataCatalogName)) {
					String packageName = metaDataProcedureName.substring(0, metaDataProcedureName.indexOf("."));
					throw new InvalidDataAccessApiUsageException(
							"Unable to determine the correct call signature for '" + metaDataProcedureName +
							"' - package name should be specified separately using '.withCatalogName(\"" +
							packageName + "\")'");
				}
				else {
					throw new InvalidDataAccessApiUsageException(
							"Unable to determine the correct call signature - no " +
							"procedure/function/signature for '" + metaDataProcedureName + "'");
				}
			}

			procs = databaseMetaData.getProcedureColumns(
					metaDataCatalogName, metaDataSchemaName, metaDataProcedureName, null);
			while (procs.next()) {
				String columnName = procs.getString("COLUMN_NAME");
				int columnType = procs.getInt("COLUMN_TYPE");
				if (columnName == null && (
						columnType == DatabaseMetaData.procedureColumnIn  ||
						columnType == DatabaseMetaData.procedureColumnInOut ||
						columnType == DatabaseMetaData.procedureColumnOut)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping metadata for: " + columnType + " " + procs.getInt("DATA_TYPE") +
							" " + procs.getString("TYPE_NAME") + " " + procs.getInt("NULLABLE") +
							" (probably a member of a collection)");
					}
				}
				else {
					CallParameterMetaData meta = new CallParameterMetaData(columnName, columnType,
							procs.getInt("DATA_TYPE"), procs.getString("TYPE_NAME"),
							procs.getInt("NULLABLE") == DatabaseMetaData.procedureNullable);
					this.callParameterMetaData.add(meta);
					if (logger.isDebugEnabled()) {
						logger.debug("Retrieved metadata: " + meta.getParameterName() + " " +
								meta.getParameterType() + " " + meta.getSqlType() + " " +
								meta.getTypeName() + " " + meta.isNullable());
					}
				}
			}
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error while retrieving metadata for procedure columns: " + ex);
			}
		}
		finally {
			try {
				if (procs != null) {
					procs.close();
				}
			}
			catch (SQLException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Problem closing ResultSet for procedure column metadata: " + ex);
				}
			}
		}
	}

}
