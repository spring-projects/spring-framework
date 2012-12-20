/*
 * Copyright 2002-2007 the original author or authors.
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
 * The Derby specific implementation of the {@link org.springframework.jdbc.core.metadata.TableMetaDataProvider}.
 * Overrides the Derby metadata info regarding retreiving generated keys. It seems to work OK so not sure why they
 * claim it's not supported.
 *
 * @author Thomas Risberg
 * @since 3.0
 */
public class DerbyTableMetaDataProvider extends GenericTableMetaDataProvider {

	private boolean supportsGeneratedKeysOverride = false;

	public DerbyTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}

	@Override
	public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
		super.initializeWithMetaData(databaseMetaData);
		if (!databaseMetaData.supportsGetGeneratedKeys()) {
			logger.warn("Overriding supportsGetGeneratedKeys from DatabaseMetaData to 'true'; it was reported as " +
					"'false' by " + databaseMetaData.getDriverName() + " " + databaseMetaData.getDriverVersion());
			supportsGeneratedKeysOverride = true;
		}
	}

	@Override
	public boolean isGetGeneratedKeysSupported() {
		boolean derbysAnswer = super.isGetGeneratedKeysSupported();
		if (!derbysAnswer) {
			return supportsGeneratedKeysOverride;
		}
		return derbysAnswer;
	}
}
