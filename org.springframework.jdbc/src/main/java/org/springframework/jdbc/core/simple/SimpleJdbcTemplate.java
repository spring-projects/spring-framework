/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jdbc.core.simple;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.util.ObjectUtils;

/**
 * Java-5-based convenience wrapper for the classic Spring
 * {@link org.springframework.jdbc.core.JdbcTemplate},
 * taking advantage of varargs and autoboxing, and exposing only the most
 * commonly required operations in order to simplify JdbcTemplate usage.
 *
 * <p>Use the {@link #getJdbcOperations()} method (or a straight JdbcTemplate)
 * if you need to invoke less commonly used template methods. This includes
 * any methods specifying SQL types, methods using less commonly used callbacks
 * such as RowCallbackHandler, updates with PreparedStatementSetters rather than
 * argument arrays, and stored procedures as well as batch operations.
 * 
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since 2.0
 * @see ParameterizedRowMapper
 * @see SimpleJdbcDaoSupport
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public class SimpleJdbcTemplate implements SimpleJdbcOperations {
	
	/** The NamedParameterJdbcTemplate that we are wrapping */
	private final NamedParameterJdbcOperations namedParameterJdbcOperations;


	/**
	 * Create a new SimpleJdbcTemplate for the given DataSource.
	 * <p>Creates a classic Spring JdbcTemplate and wraps it.
	 * @param dataSource the JDBC DataSource to access
	 */
	public SimpleJdbcTemplate(DataSource dataSource) {
		this.namedParameterJdbcOperations = new NamedParameterJdbcTemplate(dataSource);
	}

	/**
	 * Create a new SimpleJdbcTemplate for the given classic Spring JdbcTemplate.
	 * @param classicJdbcTemplate the classic Spring JdbcTemplate to wrap
	 */
	public SimpleJdbcTemplate(JdbcOperations classicJdbcTemplate) {
		this.namedParameterJdbcOperations = new NamedParameterJdbcTemplate(classicJdbcTemplate);
	}

	/**
	 * Create a new SimpleJdbcTemplate for the given Spring NamedParameterJdbcTemplate.
	 * @param namedParameterJdbcTemplate the Spring NamedParameterJdbcTemplate to wrap
	 */
	public SimpleJdbcTemplate(NamedParameterJdbcOperations namedParameterJdbcTemplate) {
		this.namedParameterJdbcOperations = namedParameterJdbcTemplate;
	}


	/**
	 * Expose the classic Spring JdbcTemplate to allow invocation of
	 * less commonly used methods.
	 */
	public JdbcOperations getJdbcOperations() {
		return this.namedParameterJdbcOperations.getJdbcOperations();
	}

	/**
	 * Expose the Spring NamedParameterJdbcTemplate to allow invocation of
	 * less commonly used methods.
	 */
	public NamedParameterJdbcOperations getNamedParameterJdbcOperations() {
		return this.namedParameterJdbcOperations;
	}


	public int queryForInt(String sql, Map args) throws DataAccessException {
		return getNamedParameterJdbcOperations().queryForInt(sql, args);
	}

	public int queryForInt(String sql, SqlParameterSource args) throws DataAccessException {
		return getNamedParameterJdbcOperations().queryForInt(sql, args);
	}

	public int queryForInt(String sql, Object... args) throws DataAccessException {
		return (ObjectUtils.isEmpty(args) ?
					getJdbcOperations().queryForInt(sql) :
					getJdbcOperations().queryForInt(sql, getArguments(args)));
	}

	public long queryForLong(String sql, Map args) throws DataAccessException {
		return getNamedParameterJdbcOperations().queryForLong(sql, args);
	}

	public long queryForLong(String sql, SqlParameterSource args) throws DataAccessException {
		return getNamedParameterJdbcOperations().queryForLong(sql, args);
	}

	public long queryForLong(String sql, Object... args) throws DataAccessException {
		return (ObjectUtils.isEmpty(args) ?
					getJdbcOperations().queryForLong(sql) :
					getJdbcOperations().queryForLong(sql, getArguments(args)));
	}

	@SuppressWarnings("unchecked")
	public <T> T queryForObject(String sql, Class<T> requiredType, Map args) throws DataAccessException {
		return (T) getNamedParameterJdbcOperations().queryForObject(sql, args, requiredType);
	}

	@SuppressWarnings("unchecked")
	public <T> T queryForObject(String sql, Class<T> requiredType, SqlParameterSource args)
			throws DataAccessException {
		return (T) getNamedParameterJdbcOperations().queryForObject(sql, args, requiredType);
	}

	@SuppressWarnings("unchecked")
	public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) throws DataAccessException {
		return (T) (ObjectUtils.isEmpty(args) ?
				getJdbcOperations().queryForObject(sql, requiredType) :
				getJdbcOperations().queryForObject(sql, getArguments(args), requiredType));
	}

	@SuppressWarnings("unchecked")
	public <T> T queryForObject(String sql, ParameterizedRowMapper<T> rm, Map args) throws DataAccessException {
		return (T) getNamedParameterJdbcOperations().queryForObject(sql, args, rm);
	}

	@SuppressWarnings("unchecked")
	public <T> T queryForObject(String sql, ParameterizedRowMapper<T> rm, SqlParameterSource args)
			throws DataAccessException {
		return (T) getNamedParameterJdbcOperations().queryForObject(sql, args, rm);
	}

	@SuppressWarnings("unchecked")
	public <T> T queryForObject(String sql, ParameterizedRowMapper<T> rm, Object... args) throws DataAccessException {
		return (T) (ObjectUtils.isEmpty(args) ?
				getJdbcOperations().queryForObject(sql, rm):
				getJdbcOperations().queryForObject(sql, getArguments(args), rm));
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> query(String sql, ParameterizedRowMapper<T> rm, Map args) throws DataAccessException {
		return (List<T>) getNamedParameterJdbcOperations().query(sql, args, rm);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> query(String sql, ParameterizedRowMapper<T> rm, SqlParameterSource args)
			throws DataAccessException {
		return (List<T>) getNamedParameterJdbcOperations().query(sql, args, rm);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> query(String sql, ParameterizedRowMapper<T> rm, Object... args) throws DataAccessException {
		return (List<T>) (ObjectUtils.isEmpty(args) ?
				getJdbcOperations().query(sql, rm) :
				getJdbcOperations().query(sql, getArguments(args), rm));
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> queryForMap(String sql, Map args) throws DataAccessException {
		return getNamedParameterJdbcOperations().queryForMap(sql, args);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> queryForMap(String sql, SqlParameterSource args)
			throws DataAccessException {
		return getNamedParameterJdbcOperations().queryForMap(sql, args);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> queryForMap(String sql, Object... args) throws DataAccessException {
		return (ObjectUtils.isEmpty(args) ?
				getJdbcOperations().queryForMap(sql) :
				getJdbcOperations().queryForMap(sql, getArguments(args)));
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> queryForList(String sql, Map args) throws DataAccessException {
		return getNamedParameterJdbcOperations().queryForList(sql, args);
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> queryForList(String sql, SqlParameterSource args)
			throws DataAccessException {
		return getNamedParameterJdbcOperations().queryForList(sql, args);
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> queryForList(String sql, Object... args) throws DataAccessException {
		return (ObjectUtils.isEmpty(args) ?
				getJdbcOperations().queryForList(sql) :
				getJdbcOperations().queryForList(sql, getArguments(args)));
	}

	public int update(String sql, Map args) throws DataAccessException {
		return getNamedParameterJdbcOperations().update(sql, args);
	}

	public int update(String sql, SqlParameterSource args) throws DataAccessException {
		return getNamedParameterJdbcOperations().update(sql, args);
	}

	public int update(String sql, Object ... args) throws DataAccessException {
		return (ObjectUtils.isEmpty(args) ?
				getJdbcOperations().update(sql) :
				getJdbcOperations().update(sql, getArguments(args)));
	}

	public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
		return doExecuteBatchUpdate(sql, batchArgs, new int[0]);
	}

	public int[] batchUpdate(String sql, List<Object[]> batchArgs, int[] argTypes) {
		return doExecuteBatchUpdate(sql, batchArgs, argTypes);
	}

	public int[] batchUpdate(String sql, Map[] batchValues) {
		SqlParameterSource[] batchArgs = new SqlParameterSource[batchValues.length];
		int i = 0;
		for (Map values : batchValues) {
			batchArgs[i] = new MapSqlParameterSource(values);
			i++;
		}
		return doExecuteBatchUpdateWithNamedParameters(sql, batchArgs);
	}

	public int[] batchUpdate(String sql, SqlParameterSource[] batchArgs) {
		return doExecuteBatchUpdateWithNamedParameters(sql, batchArgs);
	}


	private int[] doExecuteBatchUpdate(String sql, final List<Object[]> batchValues, final int[] columnTypes) {
		return getJdbcOperations().batchUpdate(
				sql,
				new BatchPreparedStatementSetter() {

					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Object[] values = batchValues.get(i);
						doSetStatementParameters(values, ps, columnTypes);
					}

					public int getBatchSize() {
						return batchValues.size();
					}
				});
	}

	private int[] doExecuteBatchUpdateWithNamedParameters(String sql, final SqlParameterSource[] batchArgs) {
		if (batchArgs.length <= 0) {
			return new int[] {0};
		}
		final ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, batchArgs[0]);
		return getJdbcOperations().batchUpdate(
				sqlToUse,
				new BatchPreparedStatementSetter() {

					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Object[] values = NamedParameterUtils.buildValueArray(parsedSql, batchArgs[i], null);
						int[] columnTypes = NamedParameterUtils.buildSqlTypeArray(parsedSql, batchArgs[i]);
						doSetStatementParameters(values, ps, columnTypes);
					}

					public int getBatchSize() {
						return batchArgs.length;
					}
				});
	}

	private void doSetStatementParameters(Object[] values, PreparedStatement ps, int[] columnTypes) throws SQLException {
		int colIndex = 0;
		for (Object value : values) {
			colIndex++;
			if (value instanceof SqlParameterValue) {
				SqlParameterValue paramValue = (SqlParameterValue) value;
				StatementCreatorUtils.setParameterValue(ps, colIndex, paramValue, paramValue.getValue());
			}
			else {
				int colType;
				if (columnTypes == null || columnTypes.length < colIndex) {
					colType = SqlTypeValue.TYPE_UNKNOWN;
				}
				else {
					colType = columnTypes[colIndex - 1];
				}
				StatementCreatorUtils.setParameterValue(ps, colIndex, colType, value);
			}
		}
	}


	/**
	 * Considers an Object array passed into a varargs parameter as
	 * collection of arguments rather than as single argument.
	 */
	private Object[] getArguments(Object[] varArgs) {
		if (varArgs.length == 1 && varArgs[0] instanceof Object[]) {
			return (Object[]) varArgs[0];
		}
		else {
			return varArgs;
		}
	}

}
