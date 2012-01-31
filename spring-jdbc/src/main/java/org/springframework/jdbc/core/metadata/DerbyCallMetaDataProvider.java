/*
 * Copyright 2002-2010 the original author or authors.
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
 * Derby specific implementation for the {@link CallMetaDataProvider} interface.
 * This class is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public class DerbyCallMetaDataProvider extends GenericCallMetaDataProvider {

	public DerbyCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}

	@Override
	public String metaDataSchemaNameToUse(String schemaName) {
		if (schemaName != null) {
			return super.metaDataSchemaNameToUse(schemaName);
		}
		// Use current user schema if no schema specified...
		String userName = getUserName();
		return (userName != null ? userName.toUpperCase() : null);
	}

}
