package org.springframework.jdbc.core.namedparam;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.core.BatchUpdateUtils;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

/**
 * Generic utility methods for working with JDBC batch statements. Mainly for internal use
 * within the framework.
 *
 * @author Thomas Risberg
 */
public class NamedParameterBatchUpdateUtils extends BatchUpdateUtils {

	public static int[] executeBatchUpdateWithNamedParameters(String sql, final SqlParameterSource[] batchArgs, JdbcOperations jdbcOperations) {
		if (batchArgs.length <= 0) {
			return new int[] {0};
		}
		final ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, batchArgs[0]);
		return jdbcOperations.batchUpdate(
				sqlToUse,
				new BatchPreparedStatementSetter() {

					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Object[] values = NamedParameterUtils.buildValueArray(parsedSql, batchArgs[i], null);
						int[] columnTypes = NamedParameterUtils.buildSqlTypeArray(parsedSql, batchArgs[i]);
						setStatementParameters(values, ps, columnTypes);
					}

					public int getBatchSize() {
						return batchArgs.length;
					}
				});
	}

}
