/*
 * Copyright 2002-2011 the original author or authors.
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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.util.ReflectionUtils;

/**
 * Oracle-specific implementation of the {@link org.springframework.jdbc.core.metadata.TableMetaDataProvider}.
 * Supports a feature for including synonyms in the metadata lookup. Also supports lookup of current schema using
 * the sys_context.
 *
 * <p>Thanks to Mike Youngstrom and Bruce Campbell for submitting the original suggestion for the Oracle
 * current schema lookup implementation.
 * 
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 3.0
 */
public class OracleTableMetaDataProvider extends GenericTableMetaDataProvider {

	private final boolean includeSynonyms;
	
	private String defaultSchema;


	public OracleTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		this(databaseMetaData, false);
	}

	public OracleTableMetaDataProvider(DatabaseMetaData databaseMetaData, boolean includeSynonyms) throws SQLException {
		super(databaseMetaData);
		this.includeSynonyms = includeSynonyms;
		lookupDefaultSchema(databaseMetaData);
	}

	@Override
	protected String getDefaultSchema() {
		if (defaultSchema != null) {
			return defaultSchema;
		}
		return super.getDefaultSchema();
	}

	@Override
	public void initializeWithTableColumnMetaData(DatabaseMetaData databaseMetaData,
			String catalogName, String schemaName, String tableName) throws SQLException {

		if (!this.includeSynonyms) {
			logger.debug("Defaulting to no synonyms in table metadata lookup");
			super.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName, tableName);
			return;
		}

		Connection con = databaseMetaData.getConnection();
		NativeJdbcExtractor nativeJdbcExtractor = getNativeJdbcExtractor();
		if (nativeJdbcExtractor != null) {
			con = nativeJdbcExtractor.getNativeConnection(con);
		}
		boolean isOracleCon;
		try {
			Class<?> oracleConClass = getClass().getClassLoader().loadClass("oracle.jdbc.OracleConnection");
			isOracleCon = oracleConClass.isInstance(con);
		}
		catch (ClassNotFoundException ex) {
			if (logger.isInfoEnabled()) {
				logger.info("Couldn't find Oracle JDBC API: " + ex);
			}
			isOracleCon = false;
		}

		if (!isOracleCon) {
			logger.warn("Unable to include synonyms in table metadata lookup. Connection used for " +
					"DatabaseMetaData is not recognized as an Oracle connection: " + con);
			super.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName, tableName);
			return;
		}

		logger.debug("Including synonyms in table metadata lookup");
		Method setIncludeSynonyms;
		Boolean originalValueForIncludeSynonyms;

		try {
			Method getIncludeSynonyms = con.getClass().getMethod("getIncludeSynonyms", (Class[]) null);
			ReflectionUtils.makeAccessible(getIncludeSynonyms);
			originalValueForIncludeSynonyms = (Boolean) getIncludeSynonyms.invoke(con);

			setIncludeSynonyms = con.getClass().getMethod("setIncludeSynonyms", new Class[] {boolean.class});
			ReflectionUtils.makeAccessible(setIncludeSynonyms);
			setIncludeSynonyms.invoke(con, Boolean.TRUE);
		}
		catch (Exception ex) {
			throw new InvalidDataAccessApiUsageException("Couldn't prepare Oracle Connection", ex);
		}

		super.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName, tableName);

		try {
			setIncludeSynonyms.invoke(con, originalValueForIncludeSynonyms);
		}
		catch (Exception ex) {
			throw new InvalidDataAccessApiUsageException("Couldn't reset Oracle Connection", ex);
		}
	}
	
	/*
	 * Oracle implementation for detecting current schema
	 * 
	 * @param databaseMetaData
	 */
	private void lookupDefaultSchema(DatabaseMetaData databaseMetaData) {
		try {
			CallableStatement cstmt = null;
			try {
				cstmt = databaseMetaData.getConnection().prepareCall("{? = call sys_context('USERENV', 'CURRENT_SCHEMA')}");
				cstmt.registerOutParameter(1, Types.VARCHAR);
				cstmt.execute();
				this.defaultSchema = cstmt.getString(1);
			}
			finally {
				if (cstmt != null) {
					cstmt.close();
				}
			}
		} catch (Exception ignore) {}
	}

}
