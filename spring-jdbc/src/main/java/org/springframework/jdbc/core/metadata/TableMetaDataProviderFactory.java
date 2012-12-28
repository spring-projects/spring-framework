/*
 * Copyright 2002-2012 the original author or authors.
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
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;

/**
 * Factory used to create a {@link TableMetaDataProvider} implementation based on the type of databse being used.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public class TableMetaDataProviderFactory {

	private static final Log logger = LogFactory.getLog(TableMetaDataProviderFactory.class);


	/**
	 * Create a TableMetaDataProvider based on the database metedata
	 * @param dataSource used to retrieve metedata
	 * @param context the class that holds configuration and metedata
	 * @return instance of the TableMetaDataProvider implementation to be used
	 */
	public static TableMetaDataProvider createMetaDataProvider(DataSource dataSource, TableMetaDataContext context) {
		return createMetaDataProvider(dataSource, context, null);
	}

	/**
	 * Create a TableMetaDataProvider based on the database metedata
	 * @param dataSource used to retrieve metedata
	 * @param context the class that holds configuration and metedata
	 * @param nativeJdbcExtractor the NativeJdbcExtractor to be used
	 * @return instance of the TableMetaDataProvider implementation to be used
	 */
	public static TableMetaDataProvider createMetaDataProvider(DataSource dataSource,
				final TableMetaDataContext context, final NativeJdbcExtractor nativeJdbcExtractor) {
		try {
			return (TableMetaDataProvider) JdbcUtils.extractDatabaseMetaData(dataSource,
					new DatabaseMetaDataCallback() {
						@Override
						public Object processMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
							String databaseProductName =
									JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
							boolean accessTableColumnMetaData = context.isAccessTableColumnMetaData();
							TableMetaDataProvider provider;
							if ("Oracle".equals(databaseProductName)) {
								provider = new OracleTableMetaDataProvider(databaseMetaData,
										context.isOverrideIncludeSynonymsDefault());
							}
							else if ("HSQL Database Engine".equals(databaseProductName)) {
								provider = new HsqlTableMetaDataProvider(databaseMetaData);
							}
							else if ("PostgreSQL".equals(databaseProductName)) {
								provider = new PostgresTableMetaDataProvider(databaseMetaData);
							}
							else if ("Apache Derby".equals(databaseProductName)) {
								provider = new DerbyTableMetaDataProvider(databaseMetaData);
							}
							else {
								provider = new GenericTableMetaDataProvider(databaseMetaData);
							}
							if (nativeJdbcExtractor != null) {
								provider.setNativeJdbcExtractor(nativeJdbcExtractor);
							}
							if (logger.isDebugEnabled()) {
								logger.debug("Using " + provider.getClass().getSimpleName());
							}
							provider.initializeWithMetaData(databaseMetaData);
							if (accessTableColumnMetaData) {
								provider.initializeWithTableColumnMetaData(databaseMetaData, context.getCatalogName(),
										context.getSchemaName(), context.getTableName());
							}
							return provider;
						}
					});
		}
		catch (MetaDataAccessException ex) {
			throw new DataAccessResourceFailureException("Error retrieving database metadata", ex);
		}
	}

}
