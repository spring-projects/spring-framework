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

package org.springframework.jdbc.core.namedparam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.SqlRowSetResultSetExtractor;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.Assert;

/**
 * Template class with a basic set of JDBC operations, allowing the use
 * of named parameters rather than traditional '?' placeholders.
 *
 * <p>This class delegates to a wrapped {@link #getJdbcOperations() JdbcTemplate}
 * once the substitution from named parameters to JDBC style '?' placeholders is
 * done at execution time. It also allows for expanding a {@link java.util.List}
 * of values to the appropriate number of placeholders.
 *
 * <p>The underlying {@link org.springframework.jdbc.core.JdbcTemplate} is
 * exposed to allow for convenient access to the traditional
 * {@link org.springframework.jdbc.core.JdbcTemplate} methods.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see NamedParameterJdbcOperations
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public class NamedParameterJdbcTemplate implements NamedParameterJdbcOperations {

	/** The JdbcTemplate we are wrapping */
	private final JdbcOperations classicJdbcTemplate;

	/** Map of original SQL String to ParsedSql representation */
	private final Map parsedSqlCache = new HashMap();


	/**
	 * Create a new NamedParameterJdbcTemplate for the given {@link DataSource}.
	 * <p>Creates a classic Spring {@link org.springframework.jdbc.core.JdbcTemplate} and wraps it.
	 * @param dataSource the JDBC DataSource to access
	 */
	public NamedParameterJdbcTemplate(DataSource dataSource) {
		Assert.notNull(dataSource, "The [dataSource] argument cannot be null.");
		this.classicJdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * Create a new NamedParameterJdbcTemplate for the given classic
	 * Spring {@link org.springframework.jdbc.core.JdbcTemplate}.
	 * @param classicJdbcTemplate the classic Spring JdbcTemplate to wrap
	 */
	public NamedParameterJdbcTemplate(JdbcOperations classicJdbcTemplate) {
		Assert.notNull(classicJdbcTemplate, "JdbcTemplate must not be null");
		this.classicJdbcTemplate = classicJdbcTemplate;
	}


	/**
	 * Expose the classic Spring JdbcTemplate to allow invocation of
	 * less commonly used methods.
	 */
	public JdbcOperations getJdbcOperations() {
		return this.classicJdbcTemplate;
	}


	public Object execute(String sql, SqlParameterSource paramSource, PreparedStatementCallback action)
			throws DataAccessException {

		return getJdbcOperations().execute(getPreparedStatementCreator(sql, paramSource), action);
	}

	public Object execute(String sql, Map paramMap, PreparedStatementCallback action) throws DataAccessException {
		return execute(sql, new MapSqlParameterSource(paramMap), action);
	}

	public Object query(String sql, SqlParameterSource paramSource, ResultSetExtractor rse)
			throws DataAccessException {

		return getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rse);
	}

	public Object query(String sql, Map paramMap, ResultSetExtractor rse) throws DataAccessException {
		return query(sql, new MapSqlParameterSource(paramMap), rse);
	}

	public void query(String sql, SqlParameterSource paramSource, RowCallbackHandler rch)
			throws DataAccessException {

		getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rch);
	}

	public void query(String sql, Map paramMap, RowCallbackHandler rch) throws DataAccessException {
		query(sql, new MapSqlParameterSource(paramMap), rch);
	}

	public List query(String sql, SqlParameterSource paramSource, RowMapper rowMapper)
			throws DataAccessException {

		return getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rowMapper);
	}

	public List query(String sql, Map paramMap, RowMapper rowMapper) throws DataAccessException {
		return query(sql, new MapSqlParameterSource(paramMap), rowMapper);
	}

	public Object queryForObject(String sql, SqlParameterSource paramSource, RowMapper rowMapper)
			throws DataAccessException {

		List results = getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rowMapper);
		return DataAccessUtils.requiredSingleResult(results);
	}

	public Object queryForObject(String sql, Map paramMap, RowMapper rowMapper) throws DataAccessException {
		return queryForObject(sql, new MapSqlParameterSource(paramMap), rowMapper);
	}

	public Object queryForObject(String sql, SqlParameterSource paramSource, Class requiredType)
			throws DataAccessException {

		return queryForObject(sql, paramSource, new SingleColumnRowMapper(requiredType));
	}

	public Object queryForObject(String sql, Map paramMap, Class requiredType) throws DataAccessException {
		return queryForObject(sql, paramMap, new SingleColumnRowMapper(requiredType));
	}

	public Map queryForMap(String sql, SqlParameterSource paramSource) throws DataAccessException {
		return (Map) queryForObject(sql, paramSource, new ColumnMapRowMapper());
	}

	public Map queryForMap(String sql, Map paramMap) throws DataAccessException {
		return (Map) queryForObject(sql, paramMap, new ColumnMapRowMapper());
	}

	public long queryForLong(String sql, SqlParameterSource paramSource) throws DataAccessException {
		Number number = (Number) queryForObject(sql, paramSource, Number.class);
		return (number != null ? number.longValue() : 0);
	}

	public long queryForLong(String sql, Map paramMap) throws DataAccessException {
		return queryForLong(sql, new MapSqlParameterSource(paramMap));
	}

	public int queryForInt(String sql, SqlParameterSource paramSource) throws DataAccessException {
		Number number = (Number) queryForObject(sql, paramSource, Number.class);
		return (number != null ? number.intValue() : 0);
	}

	public int queryForInt(String sql, Map paramMap) throws DataAccessException {
		return queryForInt(sql, new MapSqlParameterSource(paramMap));
	}

	public List queryForList(String sql, SqlParameterSource paramSource, Class elementType)
			throws DataAccessException {
		return query(sql, paramSource, new SingleColumnRowMapper(elementType));
	}

	public List queryForList(String sql, Map paramMap, Class elementType) throws DataAccessException {
		return queryForList(sql, new MapSqlParameterSource(paramMap), elementType);
	}

	public List queryForList(String sql, SqlParameterSource paramSource) throws DataAccessException {
		return query(sql, paramSource, new ColumnMapRowMapper());
	}

	public List queryForList(String sql, Map paramMap) throws DataAccessException {
		return queryForList(sql, new MapSqlParameterSource(paramMap));
	}

	public SqlRowSet queryForRowSet(String sql, SqlParameterSource paramSource) throws DataAccessException {
		return (SqlRowSet) getJdbcOperations().query(
				getPreparedStatementCreator(sql, paramSource), new SqlRowSetResultSetExtractor());
	}

	public SqlRowSet queryForRowSet(String sql, Map paramMap) throws DataAccessException {
		return queryForRowSet(sql, new MapSqlParameterSource(paramMap));
	}

	public int update(String sql, SqlParameterSource paramSource) throws DataAccessException {
		return getJdbcOperations().update(getPreparedStatementCreator(sql, paramSource));
	}

	public int update(String sql, Map paramMap) throws DataAccessException {
		return update(sql, new MapSqlParameterSource(paramMap));
	}

	public int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder)
			throws DataAccessException {

		return update(sql, paramSource, generatedKeyHolder, null);
	}

	public int update(
			String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder, String[] keyColumnNames)
			throws DataAccessException {

		ParsedSql parsedSql = getParsedSql(sql);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
		int[] paramTypes = NamedParameterUtils.buildSqlTypeArray(parsedSql, paramSource);
		PreparedStatementCreatorFactory pscf = new PreparedStatementCreatorFactory(sqlToUse, paramTypes);
		if (keyColumnNames != null) {
			pscf.setGeneratedKeysColumnNames(keyColumnNames);
		}
		else {
			pscf.setReturnGeneratedKeys(true);
		}
		return getJdbcOperations().update(pscf.newPreparedStatementCreator(params), generatedKeyHolder);
	}


	/**
	 * Build a PreparedStatementCreator based on the given SQL and named parameters.
	 * <p>Note: Not used for the <code>update</code> variant with generated key handling.
	 * @param sql SQL to execute
	 * @param paramSource container of arguments to bind
	 * @return the corresponding PreparedStatementCreator
	 */
	protected PreparedStatementCreator getPreparedStatementCreator(String sql, SqlParameterSource paramSource) {
		ParsedSql parsedSql = getParsedSql(sql);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
		int[] paramTypes = NamedParameterUtils.buildSqlTypeArray(parsedSql, paramSource);
		PreparedStatementCreatorFactory pscf = new PreparedStatementCreatorFactory(sqlToUse, paramTypes);
		return pscf.newPreparedStatementCreator(params);
	}

	/**
	 * Obtain a parsed representation of the given SQL statement.
	 * @param sql the original SQL
	 * @return a representation of the parsed SQL statement
	 */
	protected ParsedSql getParsedSql(String sql) {
		synchronized (this.parsedSqlCache) {
			ParsedSql parsedSql = (ParsedSql) this.parsedSqlCache.get(sql);
			if (parsedSql == null) {
				parsedSql = NamedParameterUtils.parseSqlStatement(sql);
				this.parsedSqlCache.put(sql, parsedSql);
			}
			return parsedSql;
		}
	}

}
