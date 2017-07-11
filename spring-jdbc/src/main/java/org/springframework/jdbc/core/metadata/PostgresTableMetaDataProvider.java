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

/**
 * The PostgreSQL specific implementation of {@link TableMetaDataProvider}.
 * Supports a feature for retrieving generated keys without the JDBC 3.0
 * {@code getGeneratedKeys} support.
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
		String version = getDatabaseVersion();
		if (version != null && version.compareTo("8.2.0") >= 0) {
			return true;
		}
		else {
			if (logger.isWarnEnabled()) {
				logger.warn("PostgreSQL does not support getGeneratedKeys or INSERT ... RETURNING in version " +
						version);
			}
			return false;
		}
	}

	@Override
	public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return "RETURNING " + keyColumnName;
	}

}
