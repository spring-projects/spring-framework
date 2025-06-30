/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.jdbc.support;

import java.sql.DatabaseMetaData;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Factory for creating {@link SQLErrorCodes} based on the
 * "databaseProductName" taken from the {@link java.sql.DatabaseMetaData}.
 *
 * <p>Returns {@code SQLErrorCodes} populated with vendor codes
 * defined in a configuration file named "sql-error-codes.xml".
 * Reads the default file in this package if not overridden by a file in
 * the root of the class path (for example in the "/WEB-INF/classes" directory).
 *
 * @author Thomas Risberg
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
 */
public class SQLErrorCodesFactory {

	/**
	 * The name of custom SQL error codes file, loading from the root
	 * of the class path (for example, from the "/WEB-INF/classes" directory).
	 */
	public static final String SQL_ERROR_CODE_OVERRIDE_PATH = "sql-error-codes.xml";

	/**
	 * The name of default SQL error code files, loading from the class path.
	 */
	public static final String SQL_ERROR_CODE_DEFAULT_PATH = "org/springframework/jdbc/support/sql-error-codes.xml";


	private static final Log logger = LogFactory.getLog(SQLErrorCodesFactory.class);

	/**
	 * Keep track of a single instance, so we can return it to classes that request it.
	 * Lazily initialized in order to avoid making {@code SQLErrorCodesFactory} constructor
	 * reachable on native images when not needed.
	 */
	private static @Nullable SQLErrorCodesFactory instance;


	/**
	 * Return the singleton instance.
	 */
	public static SQLErrorCodesFactory getInstance() {
		if (instance == null) {
			instance = new SQLErrorCodesFactory();
		}
		return instance;
	}


	/**
	 * Map to hold error codes for all databases defined in the config file.
	 * Key is the database product name, value is the SQLErrorCodes instance.
	 */
	private final Map<String, SQLErrorCodes> errorCodesMap;

	/**
	 * Map to cache the SQLErrorCodes instance per DataSource.
	 */
	private final Map<DataSource, SQLErrorCodes> dataSourceCache = new ConcurrentReferenceHashMap<>(16);


	/**
	 * Create a new instance of the {@link SQLErrorCodesFactory} class.
	 * <p>Not public to enforce Singleton design pattern. Would be private
	 * except to allow testing via overriding the
	 * {@link #loadResource(String)} method.
	 * <p><b>Do not subclass in application code.</b>
	 * @see #loadResource(String)
	 */
	protected SQLErrorCodesFactory() {

		Map<String, SQLErrorCodes> errorCodes;

		try {
			DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
			lbf.setBeanClassLoader(getClass().getClassLoader());
			XmlBeanDefinitionReader bdr = new XmlBeanDefinitionReader(lbf);

			// Load default SQL error codes.
			Resource resource = loadResource(SQL_ERROR_CODE_DEFAULT_PATH);
			if (resource != null && resource.exists()) {
				bdr.loadBeanDefinitions(resource);
			}
			else {
				logger.info("Default sql-error-codes.xml not found (should be included in spring-jdbc jar)");
			}

			// Load custom SQL error codes, overriding defaults.
			resource = loadResource(SQL_ERROR_CODE_OVERRIDE_PATH);
			if (resource != null && resource.exists()) {
				bdr.loadBeanDefinitions(resource);
				logger.debug("Found custom sql-error-codes.xml file at the root of the classpath");
			}

			// Check all beans of type SQLErrorCodes.
			errorCodes = lbf.getBeansOfType(SQLErrorCodes.class, true, false);
			if (logger.isTraceEnabled()) {
				logger.trace("SQLErrorCodes loaded: " + errorCodes.keySet());
			}
		}
		catch (BeansException ex) {
			logger.warn("Error loading SQL error codes from config file", ex);
			errorCodes = Collections.emptyMap();
		}

		this.errorCodesMap = errorCodes;
	}

	/**
	 * Load the given resource from the class path.
	 * <p><b>Not to be overridden by application developers, who should obtain
	 * instances of this class from the static {@link #getInstance()} method.</b>
	 * <p>Protected for testability.
	 * @param path resource path; either a custom path or one of either
	 * {@link #SQL_ERROR_CODE_DEFAULT_PATH} or
	 * {@link #SQL_ERROR_CODE_OVERRIDE_PATH}.
	 * @return the resource, or {@code null} if the resource wasn't found
	 * @see #getInstance
	 */
	protected @Nullable Resource loadResource(String path) {
		return new ClassPathResource(path, getClass().getClassLoader());
	}


	/**
	 * Return the {@link SQLErrorCodes} instance for the given database.
	 * <p>No need for a database meta-data lookup.
	 * @param databaseName the database name (must not be {@code null})
	 * @return the {@code SQLErrorCodes} instance for the given database
	 * (never {@code null}; potentially empty)
	 * @throws IllegalArgumentException if the supplied database name is {@code null}
	 */
	public SQLErrorCodes getErrorCodes(String databaseName) {
		Assert.notNull(databaseName, "Database product name must not be null");

		SQLErrorCodes sec = this.errorCodesMap.get(databaseName);
		if (sec == null) {
			for (SQLErrorCodes candidate : this.errorCodesMap.values()) {
				if (PatternMatchUtils.simpleMatch(candidate.getDatabaseProductNames(), databaseName)) {
					sec = candidate;
					break;
				}
			}
		}
		if (sec != null) {
			checkCustomTranslatorRegistry(databaseName, sec);
			if (logger.isDebugEnabled()) {
				logger.debug("SQL error codes for '" + databaseName + "' found");
			}
			return sec;
		}

		// Could not find the database among the defined ones.
		if (logger.isDebugEnabled()) {
			logger.debug("SQL error codes for '" + databaseName + "' not found");
		}
		return new SQLErrorCodes();
	}

	/**
	 * Return {@link SQLErrorCodes} for the given {@link DataSource},
	 * evaluating "databaseProductName" from the
	 * {@link java.sql.DatabaseMetaData}, or an empty error codes
	 * instance if no {@code SQLErrorCodes} were found.
	 * @param dataSource the {@code DataSource} identifying the database
	 * @return the corresponding {@code SQLErrorCodes} object
	 * (never {@code null}; potentially empty)
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 */
	public SQLErrorCodes getErrorCodes(DataSource dataSource) {
		SQLErrorCodes sec = resolveErrorCodes(dataSource);
		return (sec != null ? sec : new SQLErrorCodes());
	}

	/**
	 * Return {@link SQLErrorCodes} for the given {@link DataSource},
	 * evaluating "databaseProductName" from the
	 * {@link java.sql.DatabaseMetaData}, or {@code null} if case
	 * of a JDBC meta-data access problem.
	 * @param dataSource the {@code DataSource} identifying the database
	 * @return the corresponding {@code SQLErrorCodes} object,
	 * or {@code null} in case of a JDBC meta-data access problem
	 * @since 5.2.9
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 */
	public @Nullable SQLErrorCodes resolveErrorCodes(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up default SQLErrorCodes for DataSource [" + identify(dataSource) + "]");
		}

		// Try efficient lock-free access for existing cache entry
		SQLErrorCodes sec = this.dataSourceCache.get(dataSource);
		if (sec == null) {
			synchronized (this.dataSourceCache) {
				// Double-check within full dataSourceCache lock
				sec = this.dataSourceCache.get(dataSource);
				if (sec == null) {
					// We could not find it - got to look it up.
					try {
						String name = JdbcUtils.extractDatabaseMetaData(dataSource,
								DatabaseMetaData::getDatabaseProductName);
						if (StringUtils.hasLength(name)) {
							return registerDatabase(dataSource, name);
						}
					}
					catch (MetaDataAccessException ex) {
						logger.warn("Error while extracting database name", ex);
					}
					return null;
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("SQLErrorCodes found in cache for DataSource [" + identify(dataSource) + "]");
		}

		return sec;
	}

	/**
	 * Associate the specified database name with the given {@link DataSource}.
	 * @param dataSource the {@code DataSource} identifying the database
	 * @param databaseName the corresponding database name as stated in the error codes
	 * definition file (must not be {@code null})
	 * @return the corresponding {@code SQLErrorCodes} object (never {@code null})
	 * @see #unregisterDatabase(DataSource)
	 */
	public SQLErrorCodes registerDatabase(DataSource dataSource, String databaseName) {
		SQLErrorCodes sec = getErrorCodes(databaseName);
		if (logger.isDebugEnabled()) {
			logger.debug("Caching SQL error codes for DataSource [" + identify(dataSource) +
					"]: database product name is '" + databaseName + "'");
		}
		this.dataSourceCache.put(dataSource, sec);
		return sec;
	}

	/**
	 * Clear the cache for the specified {@link DataSource}, if registered.
	 * @param dataSource the {@code DataSource} identifying the database
	 * @return the corresponding {@code SQLErrorCodes} object that got removed,
	 * or {@code null} if not registered
	 * @since 4.3.5
	 * @see #registerDatabase(DataSource, String)
	 */
	public @Nullable SQLErrorCodes unregisterDatabase(DataSource dataSource) {
		return this.dataSourceCache.remove(dataSource);
	}

	/**
	 * Build an identification String for the given {@link DataSource},
	 * primarily for logging purposes.
	 * @param dataSource the {@code DataSource} to introspect
	 * @return the identification String
	 */
	private String identify(DataSource dataSource) {
		return dataSource.getClass().getName() + '@' + Integer.toHexString(dataSource.hashCode());
	}

	/**
	 * Check the {@link CustomSQLExceptionTranslatorRegistry} for any entries.
	 */
	private void checkCustomTranslatorRegistry(String databaseName, SQLErrorCodes errorCodes) {
		SQLExceptionTranslator customTranslator =
				CustomSQLExceptionTranslatorRegistry.getInstance().findTranslatorForDatabase(databaseName);
		if (customTranslator != null) {
			if (errorCodes.getCustomSqlExceptionTranslator() != null && logger.isDebugEnabled()) {
				logger.debug("Overriding already defined custom translator '" +
						errorCodes.getCustomSqlExceptionTranslator().getClass().getSimpleName() +
						" with '" + customTranslator.getClass().getSimpleName() +
						"' found in the CustomSQLExceptionTranslatorRegistry for database '" + databaseName + "'");
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Using custom translator '" + customTranslator.getClass().getSimpleName() +
						"' found in the CustomSQLExceptionTranslatorRegistry for database '" + databaseName + "'");
			}
			errorCodes.setCustomSqlExceptionTranslator(customTranslator);
		}
	}

}
