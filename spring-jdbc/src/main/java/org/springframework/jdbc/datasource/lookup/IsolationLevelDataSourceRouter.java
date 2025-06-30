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

package org.springframework.jdbc.datasource.lookup;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * DataSource that routes to one of various target DataSources based on the
 * current transaction isolation level. The target DataSources need to be
 * configured with the isolation level name as key, as defined on the
 * {@link org.springframework.transaction.TransactionDefinition TransactionDefinition}
 * interface.
 *
 * <p>This is particularly useful in combination with JTA transaction management
 * (typically through Spring's {@link org.springframework.transaction.jta.JtaTransactionManager}).
 * Standard JTA does not support transaction-specific isolation levels. Some JTA
 * providers support isolation levels as a vendor-specific extension (for example, WebLogic),
 * which is the preferred way of addressing this. As an alternative (for example, on WebSphere),
 * the target database can be represented through multiple JNDI DataSources, each
 * configured with a different isolation level (for the entire DataSource).
 * {@code IsolationLevelDataSourceRouter} allows to transparently switch to the
 * appropriate DataSource based on the current transaction's isolation level.
 *
 * <p>For example, the configuration can look like the following, assuming that
 * the target DataSources are defined as individual Spring beans with names
 * "myRepeatableReadDataSource", "mySerializableDataSource", and "myDefaultDataSource":
 *
 * <pre class="code">
 * &lt;bean id="dataSourceRouter" class="org.springframework.jdbc.datasource.lookup.IsolationLevelDataSourceRouter"&gt;
 *   &lt;property name="targetDataSources"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="ISOLATION_REPEATABLE_READ" value-ref="myRepeatableReadDataSource"/&gt;
 *       &lt;entry key="ISOLATION_SERIALIZABLE" value-ref="mySerializableDataSource"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 *   &lt;property name="defaultTargetDataSource" ref="myDefaultDataSource"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * Alternatively, the keyed values can also be data source names, to be resolved
 * through a {@link #setDataSourceLookup DataSourceLookup}: by default, JNDI
 * names for a standard JNDI lookup. This allows for a single concise definition
 * without the need for separate DataSource bean definitions.
 *
 * <pre class="code">
 * &lt;bean id="dataSourceRouter" class="org.springframework.jdbc.datasource.lookup.IsolationLevelDataSourceRouter"&gt;
 *   &lt;property name="targetDataSources"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="ISOLATION_REPEATABLE_READ" value="java:comp/env/jdbc/myrrds"/&gt;
 *       &lt;entry key="ISOLATION_SERIALIZABLE" value="java:comp/env/jdbc/myserds"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 *   &lt;property name="defaultTargetDataSource" value="java:comp/env/jdbc/mydefds"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * Note: If you are using this router in combination with Spring's
 * {@link org.springframework.transaction.jta.JtaTransactionManager},
 * don't forget to switch the "allowCustomIsolationLevels" flag to "true".
 * (By default, JtaTransactionManager will only accept a default isolation level
 * because of the lack of isolation level support in standard JTA itself.)
 *
 * <pre class="code">
 * &lt;bean id="transactionManager" class="org.springframework.transaction.jta.JtaTransactionManager"&gt;
 *   &lt;property name="allowCustomIsolationLevels" value="true"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0.1
 * @see #setTargetDataSources
 * @see #setDefaultTargetDataSource
 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
 * @see org.springframework.transaction.jta.JtaTransactionManager
 */
public class IsolationLevelDataSourceRouter extends AbstractRoutingDataSource {

	/**
	 * Map of constant names to constant values for the isolation constants
	 * defined in {@link TransactionDefinition}.
	 */
	static final Map<String, Integer> constants = Map.of(
			"ISOLATION_DEFAULT", TransactionDefinition.ISOLATION_DEFAULT,
			"ISOLATION_READ_UNCOMMITTED", TransactionDefinition.ISOLATION_READ_UNCOMMITTED,
			"ISOLATION_READ_COMMITTED", TransactionDefinition.ISOLATION_READ_COMMITTED,
			"ISOLATION_REPEATABLE_READ", TransactionDefinition.ISOLATION_REPEATABLE_READ,
			"ISOLATION_SERIALIZABLE", TransactionDefinition.ISOLATION_SERIALIZABLE
		);


	/**
	 * Supports Integer values for the isolation level constants
	 * as well as isolation level names as defined on the
	 * {@link org.springframework.transaction.TransactionDefinition TransactionDefinition interface}.
	 */
	@Override
	protected Object resolveSpecifiedLookupKey(Object lookupKey) {
		if (lookupKey instanceof Integer isolationLevel) {
			Assert.isTrue(constants.containsValue(isolationLevel),
					"Only values of isolation constants allowed");
			return isolationLevel;
		}
		else if (lookupKey instanceof String constantName) {
			Assert.hasText(constantName, "'lookupKey' must not be null or blank");
			Integer isolationLevel = constants.get(constantName);
			Assert.notNull(isolationLevel, "Only isolation constants allowed");
			return isolationLevel;
		}
		else {
			throw new IllegalArgumentException(
					"Invalid lookup key - needs to be isolation level Integer or isolation level name String: " + lookupKey);
		}
	}

	@Override
	protected @Nullable Object determineCurrentLookupKey() {
		return TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
	}

}
