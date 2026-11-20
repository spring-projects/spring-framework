/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * DB2 specific implementation for the {@link CallMetaDataProvider} interface.
 * This class is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public class Db2CallMetaDataProvider extends GenericCallMetaDataProvider {

	public Db2CallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
		try {
			setSupportsCatalogsInProcedureCalls(databaseMetaData.supportsCatalogsInProcedureCalls());
		}
		catch (SQLException ex) {
			logger.debug("Error retrieving 'DatabaseMetaData.supportsCatalogsInProcedureCalls' - " + ex.getMessage());
		}
		try {
			setSupportsSchemasInProcedureCalls(databaseMetaData.supportsSchemasInProcedureCalls());
		}
		catch (SQLException ex) {
			logger.debug("Error retrieving 'DatabaseMetaData.supportsSchemasInProcedureCalls' - " + ex.getMessage());
		}
		try {
			setStoresUpperCaseIdentifiers(databaseMetaData.storesUpperCaseIdentifiers());
		}
		catch (SQLException ex) {
			logger.debug("Error retrieving 'DatabaseMetaData.storesUpperCaseIdentifiers' - " + ex.getMessage());
		}
		try {
			setStoresLowerCaseIdentifiers(databaseMetaData.storesLowerCaseIdentifiers());
		}
		catch (SQLException ex) {
			logger.debug("Error retrieving 'DatabaseMetaData.storesLowerCaseIdentifiers' - " + ex.getMessage());
		}
	}

	@Override
	public @Nullable String metaDataSchemaNameToUse(@Nullable String schemaName) {
		if (schemaName != null) {
			return super.metaDataSchemaNameToUse(schemaName);
		}

		// Use current user schema if no schema specified...
		String userName = getUserName();
		return (userName != null ? userName.toUpperCase(Locale.ROOT) : null);
	}

}
