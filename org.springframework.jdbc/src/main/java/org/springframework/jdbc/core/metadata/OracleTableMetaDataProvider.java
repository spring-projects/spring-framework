/*
 * Copyright 2002-2009 the original author or authors.
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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * The Oracle specific implementation of the {@link org.springframework.jdbc.core.metadata.TableMetaDataProvider}.
 * Supports a feature for including synonyms in the metadata lookup.
 *
 * @author Thomas Risberg
 * @since 3.0
 */
public class OracleTableMetaDataProvider extends GenericTableMetaDataProvider {

	private boolean includeSynonyms;


	public OracleTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		this(databaseMetaData, false);
	}

	public OracleTableMetaDataProvider(DatabaseMetaData databaseMetaData, boolean includeSynonyms) throws SQLException {
		super(databaseMetaData);
		this.includeSynonyms = includeSynonyms;
	}


	@Override
	public void initializeWithTableColumnMetaData(DatabaseMetaData databaseMetaData,
			String catalogName, String schemaName, String tableName) throws SQLException {

		Connection con = databaseMetaData.getConnection();
		Method methodToInvoke = null;
		Boolean origValueForIncludeSynonyms = null;

		if (includeSynonyms) {
			if (con.getClass().getName().startsWith("oracle")) {
				if (logger.isDebugEnabled()) {
					logger.debug("Including synonyms in table metadata lookup.");
				}
			}
			else {
				logger.warn("Unable to include synonyms in table metadata lookup. Connection used for " +
						"DatabaseMetaData is not recognized as an Oracle connection; " +
						"class is " + con.getClass().getName());
			}

		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Defaulting to no synonyms in table metadata lookup.");
			}
		}

		if (includeSynonyms && con.getClass().getName().startsWith("oracle")) {
			try {
				methodToInvoke = con.getClass().getMethod("getIncludeSynonyms", (Class[]) null);
				methodToInvoke.setAccessible(true);
				origValueForIncludeSynonyms = (Boolean)methodToInvoke.invoke(con);
				methodToInvoke = con.getClass().getMethod("setIncludeSynonyms", new Class[] {boolean.class});
				methodToInvoke.setAccessible(true);
				methodToInvoke.invoke(con, Boolean.TRUE);
			}
			catch (Exception ex) {
				throw new InvalidDataAccessApiUsageException(
						"Couldn't initialize Oracle Connection.", ex);
			}

		}
		super.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName, tableName);
		if (includeSynonyms && con.getClass().getName().startsWith("oracle")) {
			try {
				methodToInvoke = con.getClass().getMethod("setIncludeSynonyms", new Class[] {boolean.class});
				methodToInvoke.setAccessible(true);
				methodToInvoke.invoke(con, origValueForIncludeSynonyms);
			}
			catch (Exception ex) {
				throw new InvalidDataAccessApiUsageException(
						"Couldn't restore Oracle Connection.", ex);
			}
		}
	}
}