/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * The HSQL specific implementation of {@link TableMetaDataProvider}.
 * Supports a feature for retrieving generated keys without the JDBC 3.0
 * {@code getGeneratedKeys} support.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public class HsqlTableMetaDataProvider extends GenericTableMetaDataProvider {

	public HsqlTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public boolean isGetGeneratedKeysSimulated() {
		return true;
	}

	@Override
	public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return "select max(identity()) from " + tableName;
	}

}
