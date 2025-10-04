/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.r2dbc;

import java.util.Objects;

import io.r2dbc.spi.ConnectionFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.util.StringUtils;

/**
 * {@code R2dbcTestUtils} is a collection of R2DBC related utility functions
 * intended to simplify standard database testing scenarios.
 *
 * @author jonghoon park
 * @since 7.0
 * @see org.springframework.r2dbc.core.DatabaseClient
 */
public abstract class R2dbcTestUtils {

	/**
	 * Count the rows in the given table.
	 * @param connectionFactory the {@link ConnectionFactory} with which to perform R2DBC
	 * operations
	 * @param tableName name of the table to count rows in
	 * @return the number of rows in the table
	 */
	public static Mono<Integer> countRowsInTable(ConnectionFactory connectionFactory, String tableName) {
		return countRowsInTable(DatabaseClient.create(connectionFactory), tableName);
	}

	/**
	 * Count the rows in the given table.
	 * @param databaseClient the {@link DatabaseClient} with which to perform R2DBC
	 * operations
	 * @param tableName name of the table to count rows in
	 * @return the number of rows in the table
	 */
	public static Mono<Integer> countRowsInTable(DatabaseClient databaseClient, String tableName) {
		return countRowsInTableWhere(databaseClient, tableName, null);
	}

	/**
	 * Count the rows in the given table, using the provided {@code WHERE} clause.
	 * <p>If the provided {@code WHERE} clause contains text, it will be prefixed
	 * with {@code " WHERE "} and then appended to the generated {@code SELECT}
	 * statement. For example, if the provided table name is {@code "person"} and
	 * the provided where clause is {@code "name = 'Bob' and age > 25"}, the
	 * resulting SQL statement to execute will be
	 * {@code "SELECT COUNT(0) FROM person WHERE name = 'Bob' and age > 25"}.
	 * @param databaseClient the {@link DatabaseClient} with which to perform JDBC
	 * operations
	 * @param tableName the name of the table to count rows in
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @return the number of rows in the table that match the provided
	 * {@code WHERE} clause
	 */
	public static Mono<Integer> countRowsInTableWhere(
			DatabaseClient databaseClient, String tableName, @Nullable String whereClause) {

		String sql = "SELECT COUNT(0) FROM " + tableName;
		if (StringUtils.hasText(whereClause)) {
			sql += " WHERE " + whereClause;
		}
		return databaseClient.sql(sql)
				.map(row -> Objects.requireNonNull(row.get(0, Long.class)).intValue())
				.one();
	}
}
