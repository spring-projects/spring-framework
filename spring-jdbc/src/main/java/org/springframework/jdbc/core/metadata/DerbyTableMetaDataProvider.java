/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * The Derby specific implementation of {@link TableMetaDataProvider}.
 * Overrides the Derby meta-data info regarding retrieving generated keys.
 *
 * @author Thomas Risberg
 * @since 3.0
 * @deprecated Apache Derby is retired, switch to HSQLDB or H2
 */
@Deprecated(forRemoval = true, since = "7.1")
public class DerbyTableMetaDataProvider extends GenericTableMetaDataProvider {

	private boolean supportsGeneratedKeysOverride = false;


	public DerbyTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
		super.initializeWithMetaData(databaseMetaData);
		if (!databaseMetaData.supportsGetGeneratedKeys()) {
			if (logger.isInfoEnabled()) {
				logger.info("Overriding supportsGetGeneratedKeys from DatabaseMetaData to 'true'; it was reported as " +
						"'false' by " + databaseMetaData.getDriverName() + " " + databaseMetaData.getDriverVersion());
			}
			this.supportsGeneratedKeysOverride = true;
		}
	}

	@Override
	public boolean isGetGeneratedKeysSupported() {
		return (super.isGetGeneratedKeysSupported() || this.supportsGeneratedKeysOverride);
	}

}
