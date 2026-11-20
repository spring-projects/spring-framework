/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * The MySQL/MariaDB specific implementation of {@link TableMetaDataProvider}.
 * Sets {@link #setGeneratedKeysColumnNameArraySupported} to {@code false}.
 *
 * @author Juergen Hoeller
 * @since 6.2.12
 */
public class MySQLTableMetaDataProvider extends GenericTableMetaDataProvider {

	public MySQLTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
		setGeneratedKeysColumnNameArraySupported(false);
	}

}
