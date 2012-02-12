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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;

/**
 * Registry for registering custom {@link org.springframework.jdbc.support.SQLExceptionTranslator}.
 *
 * @author Thomas Risberg
 * @since 3.1
 */
public class CustomSQLExceptionTranslatorRegistrar implements InitializingBean {

	private static final Log logger = LogFactory.getLog(CustomSQLExceptionTranslatorRegistrar.class);

	/**
	 * Map registry to hold custom translators specific databases.
	 * Key is the database product name as defined in the
	 * {@link org.springframework.jdbc.support.SQLErrorCodesFactory}.
	 */
	private final Map<String, SQLExceptionTranslator> sqlExceptionTranslators =
			new HashMap<String, SQLExceptionTranslator>();

	/**
	 * Setter for a Map of translators where the key must be the database name as defined in the
	 * sql-error-codes.xml file. This method is used when this registry is used in an application context.
	 * <p>Note that any existing translators will remain unless there is a match in the database name at which
	 * point the new translator will replace the existing one.
	 *
	 * @param sqlExceptionTranslators
	 */
	public void setSqlExceptionTranslators(Map<String, SQLExceptionTranslator> sqlExceptionTranslators) {
		this.sqlExceptionTranslators.putAll(sqlExceptionTranslators);
	}

	public void afterPropertiesSet() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Registering custom SQL exception translators for database(s): " +
					sqlExceptionTranslators.keySet());
		}
		for (String dbName : sqlExceptionTranslators.keySet()) {
			CustomSQLExceptionTranslatorRegistry.getInstance()
					.registerSqlExceptionTranslator(dbName, sqlExceptionTranslators.get(dbName));
		}
	}
}
