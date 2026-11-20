/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

/**
 * The PostgreSQL specific implementation of {@link TableMetaDataProvider}.
 * Supports a feature for retrieving generated keys without the JDBC 3.0
 * {@code getGeneratedKeys} support. Also, it processes PostgreSQL-returned
 * catalog and schema names from {@code DatabaseMetaData} in the given case.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public class PostgresTableMetaDataProvider extends GenericTableMetaDataProvider {

	public PostgresTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public @Nullable String metaDataCatalogNameToUse(@Nullable String catalogName) {
		return catalogName;
	}

	@Override
	public @Nullable String metaDataSchemaNameToUse(@Nullable String schemaName) {
		return (schemaName != null ? schemaName : getDefaultSchema());
	}

	@Override
	public boolean isGetGeneratedKeysSimulated() {
		return true;
	}

	@Override
	public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return "RETURNING " + keyColumnName;
	}

}
