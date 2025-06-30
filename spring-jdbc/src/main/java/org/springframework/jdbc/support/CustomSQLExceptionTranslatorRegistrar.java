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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;

/**
 * Registry for custom {@link SQLExceptionTranslator} instances for specific databases.
 *
 * @author Thomas Risberg
 * @since 3.1.1
 */
public class CustomSQLExceptionTranslatorRegistrar implements InitializingBean {

	/**
	 * Map registry to hold custom translators specific databases.
	 * Key is the database product name as defined in the
	 * {@link org.springframework.jdbc.support.SQLErrorCodesFactory}.
	 */
	private final Map<String, SQLExceptionTranslator> translators = new HashMap<>();


	/**
	 * Setter for a Map of {@link SQLExceptionTranslator} references where the key must
	 * be the database name as defined in the {@code sql-error-codes.xml} file.
	 * <p>Note that any existing translators will remain unless there is a match in the
	 * database name, at which point the new translator will replace the existing one.
	 */
	public void setTranslators(Map<String, SQLExceptionTranslator> translators) {
		this.translators.putAll(translators);
	}

	@Override
	public void afterPropertiesSet() {
		this.translators.forEach((dbName, translator) ->
				CustomSQLExceptionTranslatorRegistry.getInstance().registerTranslator(dbName, translator));
	}

}
