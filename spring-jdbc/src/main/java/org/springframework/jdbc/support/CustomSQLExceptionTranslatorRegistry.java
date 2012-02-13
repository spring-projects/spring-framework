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

package org.springframework.jdbc.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * Registry for custom {@link org.springframework.jdbc.support.SQLExceptionTranslator} instances associated with
 * specific databases allowing for overriding translation based on values contained in the configuration file
 * named "sql-error-codes.xml".
 *
 * @author Thomas Risberg
 * @since 3.1
 * @see SQLErrorCodesFactory
 */
public class CustomSQLExceptionTranslatorRegistry {

	private static final Log logger = LogFactory.getLog(CustomSQLExceptionTranslatorRegistry.class);

	/**
	 * Map registry to hold custom translators specific databases.
	 * Key is the database product name as defined in the
	 * {@link org.springframework.jdbc.support.SQLErrorCodesFactory}.
	 */
	private final Map<String, SQLExceptionTranslator> sqlExceptionTranslatorRegistry =
			new HashMap<String, SQLExceptionTranslator>();


	/**
	 * Keep track of a single instance so we can return it to classes that request it.
	 */
	private static final CustomSQLExceptionTranslatorRegistry instance = new CustomSQLExceptionTranslatorRegistry();


	/**
	 * Return the singleton instance.
	 */
	public static CustomSQLExceptionTranslatorRegistry getInstance() {
		return instance;
	}


	/**
	 * Create a new instance of the {@link org.springframework.jdbc.support.CustomSQLExceptionTranslatorRegistry} class.
	 * <p>Not public to enforce Singleton design pattern.
	 */
	private CustomSQLExceptionTranslatorRegistry() {
	}

	/**
	 * Register a new custom translator for the specified database name.
	 *
	 * @param dbName the database name
	 * @param sqlExceptionTranslator the custom translator
	 */
	public void registerSqlExceptionTranslator(String dbName, SQLExceptionTranslator sqlExceptionTranslator) {
		SQLExceptionTranslator replaced = sqlExceptionTranslatorRegistry.put(dbName, sqlExceptionTranslator);
		if (replaced != null) {
			logger.warn("Replacing custom translator '" + replaced +
					"' for database " + dbName +
					" with '" + sqlExceptionTranslator + "'");
		}
		else {
			logger.info("Adding custom translator '" + sqlExceptionTranslator.getClass().getSimpleName() +
					"' for database " + dbName);
		}
	}

	public SQLExceptionTranslator findSqlExceptionTranslatorForDatabase(String dbName) {
		return sqlExceptionTranslatorRegistry.get(dbName);
	}

}
