/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.metadata;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * Factory used to create a {@link TableMetaDataProvider} implementation
 * based on the type of database being used.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public final class TableMetaDataProviderFactory {

	private static final Log logger = LogFactory.getLog(TableMetaDataProviderFactory.class);


	private TableMetaDataProviderFactory() {
	}


	/**
	 * Create a {@link TableMetaDataProvider} based on the database meta-data.
	 * @param dataSource used to retrieve meta-data
	 * @param context the class that holds configuration and meta-data
	 * @return instance of the TableMetaDataProvider implementation to be used
	 */
	public static TableMetaDataProvider createMetaDataProvider(DataSource dataSource, TableMetaDataContext context) {
		try {
			return JdbcUtils.extractDatabaseMetaData(dataSource, databaseMetaData -> {
				String databaseProductName = JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
				TableMetaDataProvider provider;

				if ("Oracle".equals(databaseProductName)) {
					provider = new OracleTableMetaDataProvider(
							databaseMetaData, context.isOverrideIncludeSynonymsDefault());
				}
				else if ("PostgreSQL".equals(databaseProductName)) {
					provider = new PostgresTableMetaDataProvider(databaseMetaData);
				}
				else if ("Apache Derby".equals(databaseProductName)) {
					provider = new DerbyTableMetaDataProvider(databaseMetaData);
				}
				else if ("HSQL Database Engine".equals(databaseProductName)) {
					provider = new HsqlTableMetaDataProvider(databaseMetaData);
				}
				else if ("MySQL".equals(databaseProductName) || "MariaDB".equals(databaseProductName)) {
					provider = new MySQLTableMetaDataProvider(databaseMetaData);
				}
				else {
					provider = new GenericTableMetaDataProvider(databaseMetaData);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Using " + provider.getClass().getSimpleName());
				}

				provider.initializeWithMetaData(databaseMetaData);

				if (context.isAccessTableColumnMetaData()) {
					provider.initializeWithTableColumnMetaData(databaseMetaData,
							context.getCatalogName(), context.getSchemaName(), context.getTableName());
				}

				return provider;
			});
		}
		catch (MetaDataAccessException ex) {
			throw new DataAccessResourceFailureException("Error retrieving database meta-data", ex);
		}
	}

}
