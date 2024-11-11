/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * {@link DataFieldMaxValueIncrementer} that increments the maximum counter value of an
 * auto-increment column of a given MySQL table.
 *
 * <p>The sequence is kept in a table. The storage engine used by the sequence table must be
 * InnoDB in MySQL 8.0 or later since the current maximum auto-increment counter is required to be
 * persisted across restarts of the database server.
 *
 * <p>Example:
 *
 * <pre class="code">
 * create table tab_sequence (`id` bigint unsigned primary key auto_increment);</pre>
 *
 * <p>If {@code cacheSize} is set, the intermediate values are served without querying the
 * database. If the server or your application is stopped or crashes or a transaction
 * is rolled back, the unused values will never be served. The maximum hole size in
 * numbering is consequently the value of {@code cacheSize}.
 *
 * @author Henning Pöttker
 * @since 6.1.2
 */
public class MySQLIdentityColumnMaxValueIncrementer extends AbstractIdentityColumnMaxValueIncrementer {

	/**
	 * Default constructor for bean property style usage.
	 * @see #setDataSource
	 * @see #setIncrementerName
	 * @see #setColumnName
	 */
	public MySQLIdentityColumnMaxValueIncrementer() {
	}

	/**
	 * Convenience constructor.
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence table to use
	 * @param columnName the name of the column in the sequence table to use
	 */
	public MySQLIdentityColumnMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
	}

	@Override
	protected String getIncrementStatement() {
		return "insert into " + getIncrementerName() + " () values ()";
	}

	@Override
	protected String getIdentityStatement() {
		return "select last_insert_id()";
	}

}
