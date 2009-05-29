package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * The PostgreSQL specific implementation of the {@link org.springframework.jdbc.core.metadata.TableMetaDataProvider}.
 * Suports a feature for retreiving generated keys without the JDBC 3.0 getGeneratedKeys support.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public class PostgresTableMetaDataProvider extends GenericTableMetaDataProvider {

	public PostgresTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public boolean isGetGeneratedKeysSimulated() {
		if (getDatabaseVersion().compareTo("8.2.0") >= 0) {
			return true;
		}
		else {
			logger.warn("PostgreSQL does not support getGeneratedKeys or INSERT ... RETURNING in version " + getDatabaseVersion());
			return false;
		}
	}


	@Override
	public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return "RETURNING " + keyColumnName;
	}
}
