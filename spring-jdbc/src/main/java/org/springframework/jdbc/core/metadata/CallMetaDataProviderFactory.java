/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.core.metadata;

import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * Factory used to create a {@link CallMetaDataProvider} implementation
 * based on the type of database being used.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public final class CallMetaDataProviderFactory {

	/** List of supported database products for procedure calls. */
	public static final List<String> supportedDatabaseProductsForProcedures = Arrays.asList(
			"Apache Derby",
			"DB2",
			"MySQL",
			"Microsoft SQL Server",
			"Oracle",
			"PostgreSQL",
			"Sybase"
		);

	/** List of supported database products for function calls. */
	public static final List<String> supportedDatabaseProductsForFunctions = Arrays.asList(
			"MySQL",
			"Microsoft SQL Server",
			"Oracle",
			"PostgreSQL"
		);

	private static final Log logger = LogFactory.getLog(CallMetaDataProviderFactory.class);


	private CallMetaDataProviderFactory() {
	}


	/**
	 * Create a {@link CallMetaDataProvider} based on the database meta-data.
	 * @param dataSource the JDBC DataSource to use for retrieving meta-data
	 * @param context the class that holds configuration and meta-data
	 * @return instance of the CallMetaDataProvider implementation to be used
	 */
	public static CallMetaDataProvider createMetaDataProvider(DataSource dataSource, final CallMetaDataContext context) {
		try {
			return (CallMetaDataProvider) JdbcUtils.extractDatabaseMetaData(dataSource, databaseMetaData -> {
				String databaseProductName = JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
				boolean accessProcedureColumnMetaData = context.isAccessCallParameterMetaData();
				if (context.isFunction()) {
					if (!supportedDatabaseProductsForFunctions.contains(databaseProductName)) {
						if (logger.isInfoEnabled()) {
							logger.info(databaseProductName + " is not one of the databases fully supported for function calls " +
									"-- supported are: " + supportedDatabaseProductsForFunctions);
						}
						if (accessProcedureColumnMetaData) {
							logger.info("Metadata processing disabled - you must specify all parameters explicitly");
							accessProcedureColumnMetaData = false;
						}
					}
				}
				else {
					if (!supportedDatabaseProductsForProcedures.contains(databaseProductName)) {
						if (logger.isInfoEnabled()) {
							logger.info(databaseProductName + " is not one of the databases fully supported for procedure calls " +
									"-- supported are: " + supportedDatabaseProductsForProcedures);
						}
						if (accessProcedureColumnMetaData) {
							logger.info("Metadata processing disabled - you must specify all parameters explicitly");
							accessProcedureColumnMetaData = false;
						}
					}
				}

				CallMetaDataProvider provider;
				if ("Oracle".equals(databaseProductName)) {
					provider = new OracleCallMetaDataProvider(databaseMetaData);
				}
				else if ("PostgreSQL".equals(databaseProductName)) {
					provider = new PostgresCallMetaDataProvider((databaseMetaData));
				}
				else if ("Apache Derby".equals(databaseProductName)) {
					provider = new DerbyCallMetaDataProvider((databaseMetaData));
				}
				else if ("DB2".equals(databaseProductName)) {
					provider = new Db2CallMetaDataProvider((databaseMetaData));
				}
				else if ("HDB".equals(databaseProductName)) {
					provider = new HanaCallMetaDataProvider((databaseMetaData));
				}
				else if ("Microsoft SQL Server".equals(databaseProductName)) {
					provider = new SqlServerCallMetaDataProvider((databaseMetaData));
				}
				else if ("Sybase".equals(databaseProductName)) {
					provider = new SybaseCallMetaDataProvider((databaseMetaData));
				}
				else {
					provider = new GenericCallMetaDataProvider(databaseMetaData);
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Using " + provider.getClass().getName());
				}
				provider.initializeWithMetaData(databaseMetaData);
				if (accessProcedureColumnMetaData) {
					provider.initializeWithProcedureColumnMetaData(databaseMetaData,
							context.getCatalogName(), context.getSchemaName(), context.getProcedureName());
				}
				return provider;
			});
		}
		catch (MetaDataAccessException ex) {
			throw new DataAccessResourceFailureException("Error retrieving database meta-data", ex);
		}
	}

}
