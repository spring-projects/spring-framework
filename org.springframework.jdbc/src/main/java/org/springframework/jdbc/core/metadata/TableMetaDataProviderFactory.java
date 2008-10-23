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

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * Factory used to create a {@link TableMetaDataProvider} implementation based on the type of databse being used.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public class TableMetaDataProviderFactory {

	/** Logger */
	private static final Log logger = LogFactory.getLog(TableMetaDataProviderFactory.class);

	/**
	 * Create a TableMetaDataProvider based on the database metedata
	 * @param dataSource used to retrieve metedata
	 * @param context the class that holds configuration and metedata
	 * @return instance of the TableMetaDataProvider implementation to be used
	 */
	static public TableMetaDataProvider createMetaDataProvider(DataSource dataSource,
															 final TableMetaDataContext context) {
		try {
			return (TableMetaDataProvider) JdbcUtils.extractDatabaseMetaData(
					dataSource, new DatabaseMetaDataCallback() {

				public Object processMetaData(DatabaseMetaData databaseMetaData)
						throws SQLException, MetaDataAccessException {
					String databaseProductName = JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
					boolean accessTableColumnMetaData = context.isAccessTableParameterMetaData();
					TableMetaDataProvider provider;
					if ("HSQL Database Engine".equals(databaseProductName)) {
						provider = new HsqlTableMetaDataProvider(databaseMetaData);
					}
					else if ("PostgreSQL".equals(databaseProductName)) {
						provider = new PostgresTableMetaDataProvider(databaseMetaData);
					}
					else {
						provider = new GenericTableMetaDataProvider(databaseMetaData);
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Using " + provider.getClass().getName());
					}
					provider.initializeWithMetaData(databaseMetaData);
					if (accessTableColumnMetaData) {
						provider.initializeWithTableColumnMetaData(databaseMetaData, context.getCatalogName(), context.getSchemaName(), context.getTableName());
					}
					return provider;
				}
			});
		} catch (MetaDataAccessException e) {
			throw new DataAccessResourceFailureException("Error retreiving database metadata", e);
		}

	}

}
