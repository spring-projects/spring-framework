/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Registry for custom {@link org.springframework.jdbc.support.SQLExceptionTranslator} instances associated with
 * specific databases allowing for overriding translation based on values contained in the configuration file
 * named "sql-error-codes.xml".
 *
 * @author Thomas Risberg
 * @since 3.1.1
 * @see SQLErrorCodesFactory
 */
public class CustomSQLExceptionTranslatorRegistry {

	private static final Log logger = LogFactory.getLog(CustomSQLExceptionTranslatorRegistry.class);

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
	 * Map registry to hold custom translators specific databases.
	 * Key is the database product name as defined in the
	 * {@link org.springframework.jdbc.support.SQLErrorCodesFactory}.
	 */
	private final Map<String, SQLExceptionTranslator> translatorMap = new HashMap<>();


	/**
	 * Create a new instance of the {@link CustomSQLExceptionTranslatorRegistry} class.
	 * <p>Not public to enforce Singleton design pattern.
	 */
	private CustomSQLExceptionTranslatorRegistry() {
	}

	/**
	 * Register a new custom translator for the specified database name.
	 * @param dbName the database name
	 * @param translator the custom translator
	 */
	public void registerTranslator(String dbName, SQLExceptionTranslator translator) {
		SQLExceptionTranslator replaced = translatorMap.put(dbName, translator);
		if (replaced != null) {
			logger.warn("Replacing custom translator [" + replaced + "] for database '" + dbName +
					"' with [" + translator + "]");
		}
		else {
			logger.info("Adding custom translator of type [" + translator.getClass().getName() +
					"] for database '" + dbName + "'");
		}
	}

	/**
	 * Find a custom translator for the specified database.
	 * @param dbName the database name
	 * @return the custom translator, or {@code null} if none found
	 */
	public SQLExceptionTranslator findTranslatorForDatabase(String dbName) {
		return this.translatorMap.get(dbName);
	}

}
