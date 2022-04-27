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

package org.springframework.jdbc.core.namedparam;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Generic utility methods for working with JDBC batch statements using named parameters.
 * Mainly for internal use within the framework.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 3.0
 * @deprecated as of 5.1.3, not used by {@link NamedParameterJdbcTemplate} anymore
 */
@Deprecated
public abstract class NamedParameterBatchUpdateUtils extends org.springframework.jdbc.core.BatchUpdateUtils {

	public static int[] executeBatchUpdateWithNamedParameters(
			final ParsedSql parsedSql, final SqlParameterSource[] batchArgs, JdbcOperations jdbcOperations) {

		if (batchArgs.length == 0) {
			return new int[0];
		}

		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, batchArgs[0]);
		return jdbcOperations.batchUpdate(
				sqlToUse,
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Object[] values = NamedParameterUtils.buildValueArray(parsedSql, batchArgs[i], null);
						int[] columnTypes = NamedParameterUtils.buildSqlTypeArray(parsedSql, batchArgs[i]);
						setStatementParameters(values, ps, columnTypes);
					}
					@Override
					public int getBatchSize() {
						return batchArgs.length;
					}
				});
	}

}
