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

package org.springframework.jdbc.core.namedparam;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
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
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlRowSetResultSetExtractor;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentLruCache;

/**
 * Template class with a basic set of JDBC operations, allowing the use
 * of named parameters rather than traditional '?' placeholders.
 *
 * <p>This class delegates to a wrapped {@link #getJdbcOperations() JdbcTemplate}
 * once the substitution from named parameters to JDBC style '?' placeholders is
 * done at execution time. It also allows for expanding a {@link java.util.List}
 * of values to the appropriate number of placeholders.
 *
 * <p>An instance of this template class is thread-safe once configured.
 * The underlying {@link org.springframework.jdbc.core.JdbcTemplate} is
 * exposed to allow for convenient access to the traditional
 * {@link org.springframework.jdbc.core.JdbcTemplate} methods.
 *
 * <p><b>NOTE: As of 6.1, there is a unified JDBC access facade available in
 * the form of {@link org.springframework.jdbc.core.simple.JdbcClient}.</b>
 * {@code JdbcClient} provides a fluent API style for common JDBC queries/updates
 * with flexible use of indexed or named parameters. It delegates to a
 * {@code JdbcTemplate}/{@code NamedParameterJdbcTemplate} for actual execution.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see NamedParameterJdbcOperations
 * @see SqlParameterSource
 * @see ResultSetExtractor
 * @see RowCallbackHandler
 * @see RowMapper
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public class NamedParameterJdbcTemplate implements NamedParameterJdbcOperations {

	/** Default maximum number of entries for this template's SQL cache: 256. */
	public static final int DEFAULT_CACHE_LIMIT = 256;


	/** The JdbcTemplate we are wrapping. */
	private final JdbcOperations classicJdbcTemplate;

	/** Cache of original SQL String to ParsedSql representation. */
	private volatile ConcurrentLruCache<String, ParsedSql> parsedSqlCache =
			new ConcurrentLruCache<>(DEFAULT_CACHE_LIMIT, NamedParameterUtils::parseSqlStatement);


	/**
	 * Create a new NamedParameterJdbcTemplate for the given {@link DataSource}.
	 * <p>Creates a classic Spring {@link org.springframework.jdbc.core.JdbcTemplate} and wraps it.
	 * @param dataSource the JDBC DataSource to access
	 */
	public NamedParameterJdbcTemplate(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
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
	 * Expose the classic Spring JdbcTemplate operations to allow invocation
	 * of less commonly used methods.
	 */
	@Override
	public JdbcOperations getJdbcOperations() {
		return this.classicJdbcTemplate;
	}

	/**
	 * Expose the classic Spring {@link JdbcTemplate} itself, if available,
	 * in particular for passing it on to other {@code JdbcTemplate} consumers.
	 * <p>If sufficient for the purposes at hand, {@link #getJdbcOperations()}
	 * is recommended over this variant.
	 * @since 5.0.3
	 */
	public JdbcTemplate getJdbcTemplate() {
		Assert.state(this.classicJdbcTemplate instanceof JdbcTemplate, "No JdbcTemplate available");
		return (JdbcTemplate) this.classicJdbcTemplate;
	}

	/**
	 * Specify the maximum number of entries for this template's SQL cache.
	 * Default is 256. 0 indicates no caching, always parsing each statement.
	 */
	public void setCacheLimit(int cacheLimit) {
		this.parsedSqlCache = new ConcurrentLruCache<>(cacheLimit, NamedParameterUtils::parseSqlStatement);
	}

	/**
	 * Return the maximum number of entries for this template's SQL cache.
	 */
	public int getCacheLimit() {
		return this.parsedSqlCache.capacity();
	}


	@Override
	public <T> @Nullable T execute(String sql, SqlParameterSource paramSource, PreparedStatementCallback<T> action)
			throws DataAccessException {

		return getJdbcOperations().execute(getPreparedStatementCreator(sql, paramSource), action);
	}

	@Override
	public <T> @Nullable T execute(String sql, Map<String, ?> paramMap, PreparedStatementCallback<T> action)
			throws DataAccessException {

		return execute(sql, new MapSqlParameterSource(paramMap), action);
	}

	@Override
	public <T> @Nullable T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
		return execute(sql, EmptySqlParameterSource.INSTANCE, action);
	}

	@Override
	public <T> @Nullable T query(String sql, SqlParameterSource paramSource, ResultSetExtractor<T> rse)
			throws DataAccessException {

		return getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rse);
	}

	@Override
	public <T> @Nullable T query(String sql, Map<String, ?> paramMap, ResultSetExtractor<T> rse)
			throws DataAccessException {

		return query(sql, new MapSqlParameterSource(paramMap), rse);
	}

	@Override
	public <T> @Nullable T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(sql, EmptySqlParameterSource.INSTANCE, rse);
	}

	@Override
	public void query(String sql, SqlParameterSource paramSource, RowCallbackHandler rch)
			throws DataAccessException {

		getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rch);
	}

	@Override
	public void query(String sql, Map<String, ?> paramMap, RowCallbackHandler rch)
			throws DataAccessException {

		query(sql, new MapSqlParameterSource(paramMap), rch);
	}

	@Override
	public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
		query(sql, EmptySqlParameterSource.INSTANCE, rch);
	}

	@Override
	public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException {

		return getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rowMapper);
	}

	@Override
	public <T> List<T> query(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException {

		return query(sql, new MapSqlParameterSource(paramMap), rowMapper);
	}

	@Override
	public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		return query(sql, EmptySqlParameterSource.INSTANCE, rowMapper);
	}

	@Override
	public <T> Stream<T> queryForStream(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException {

		return getJdbcOperations().queryForStream(getPreparedStatementCreator(sql, paramSource), rowMapper);
	}

	@Override
	public <T> Stream<T> queryForStream(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException {

		return queryForStream(sql, new MapSqlParameterSource(paramMap), rowMapper);
	}

	@Override
	public <T> @Nullable T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException {

		List<T> results = getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rowMapper);
		return DataAccessUtils.nullableSingleResult(results);
	}

	@Override
	public <T> @Nullable T queryForObject(String sql, Map<String, ?> paramMap, RowMapper<T>rowMapper)
			throws DataAccessException {

		return queryForObject(sql, new MapSqlParameterSource(paramMap), rowMapper);
	}

	@Override
	public <T> @Nullable T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType)
			throws DataAccessException {

		return queryForObject(sql, paramSource, new SingleColumnRowMapper<>(requiredType));
	}

	@Override
	public <T> @Nullable T queryForObject(String sql, Map<String, ?> paramMap, Class<T> requiredType)
			throws DataAccessException {

		return queryForObject(sql, paramMap, new SingleColumnRowMapper<>(requiredType));
	}

	@Override
	public Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) throws DataAccessException {
		Map<String, Object> result = queryForObject(sql, paramSource, new ColumnMapRowMapper());
		Assert.state(result != null, "No result map");
		return result;
	}

	@Override
	public Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap) throws DataAccessException {
		Map<String, Object> result = queryForObject(sql, paramMap, new ColumnMapRowMapper());
		Assert.state(result != null, "No result map");
		return result;
	}

	@Override
	public <T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType)
			throws DataAccessException {

		return query(sql, paramSource, new SingleColumnRowMapper<>(elementType));
	}

	@Override
	public <T> List<T> queryForList(String sql, Map<String, ?> paramMap, Class<T> elementType)
			throws DataAccessException {

		return queryForList(sql, new MapSqlParameterSource(paramMap), elementType);
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource)
			throws DataAccessException {

		return query(sql, paramSource, new ColumnMapRowMapper());
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap)
			throws DataAccessException {

		return queryForList(sql, new MapSqlParameterSource(paramMap));
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, SqlParameterSource paramSource) throws DataAccessException {
		SqlRowSet result = getJdbcOperations().query(
				getPreparedStatementCreator(sql, paramSource), new SqlRowSetResultSetExtractor());
		Assert.state(result != null, "No result");
		return result;
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, Map<String, ?> paramMap) throws DataAccessException {
		return queryForRowSet(sql, new MapSqlParameterSource(paramMap));
	}

	@Override
	public int update(String sql, SqlParameterSource paramSource) throws DataAccessException {
		return getJdbcOperations().update(getPreparedStatementCreator(sql, paramSource));
	}

	@Override
	public int update(String sql, Map<String, ?> paramMap) throws DataAccessException {
		return update(sql, new MapSqlParameterSource(paramMap));
	}

	@Override
	public int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder)
			throws DataAccessException {

		return update(sql, paramSource, generatedKeyHolder, null);
	}

	@Override
	public int update(
			String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder, String @Nullable [] keyColumnNames)
			throws DataAccessException {

		PreparedStatementCreator psc = getPreparedStatementCreator(sql, paramSource, pscf -> {
			if (keyColumnNames != null) {
				pscf.setGeneratedKeysColumnNames(keyColumnNames);
			}
			else {
				pscf.setReturnGeneratedKeys(true);
			}
		});
		return getJdbcOperations().update(psc, generatedKeyHolder);
	}

	@Override
	public int[] batchUpdate(String sql, SqlParameterSource[] batchArgs) {
		if (batchArgs.length == 0) {
			return new int[0];
		}

		ParsedSql parsedSql = getParsedSql(sql);
		PreparedStatementCreatorFactory pscf = getPreparedStatementCreatorFactory(parsedSql, batchArgs[0]);

		return getJdbcOperations().batchUpdate(
				pscf.getSql(),
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						@Nullable Object[] values = NamedParameterUtils.buildValueArray(parsedSql, batchArgs[i], null);
						pscf.newPreparedStatementSetter(values).setValues(ps);
					}
					@Override
					public int getBatchSize() {
						return batchArgs.length;
					}
				});
	}

	@Override
	public int[] batchUpdate(String sql, Map<String, ?>[] batchValues) {
		return batchUpdate(sql, SqlParameterSourceUtils.createBatch(batchValues));
	}

	@Override
	public int[] batchUpdate(String sql, SqlParameterSource[] batchArgs, KeyHolder generatedKeyHolder) {
		return batchUpdate(sql, batchArgs, generatedKeyHolder, null);
	}

	@Override
	public int[] batchUpdate(String sql, SqlParameterSource[] batchArgs, KeyHolder generatedKeyHolder,
			String @Nullable [] keyColumnNames) {

		if (batchArgs.length == 0) {
			return new int[0];
		}

		ParsedSql parsedSql = getParsedSql(sql);
		SqlParameterSource paramSource = batchArgs[0];
		PreparedStatementCreatorFactory pscf = getPreparedStatementCreatorFactory(parsedSql, paramSource);
		if (keyColumnNames != null) {
			pscf.setGeneratedKeysColumnNames(keyColumnNames);
		}
		else {
			pscf.setReturnGeneratedKeys(true);
		}
		@Nullable Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
		PreparedStatementCreator psc = pscf.newPreparedStatementCreator(params);
		return getJdbcOperations().batchUpdate(psc, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				@Nullable Object[] values = NamedParameterUtils.buildValueArray(parsedSql, batchArgs[i], null);
				pscf.newPreparedStatementSetter(values).setValues(ps);
			}

			@Override
			public int getBatchSize() {
				return batchArgs.length;
			}
		}, generatedKeyHolder);
	}


	/**
	 * Build a {@link PreparedStatementCreator} based on the given SQL and named parameters.
	 * <p>Note: Directly called from all {@code query} variants. Delegates to the common
	 * {@link #getPreparedStatementCreator(String, SqlParameterSource, Consumer)} method.
	 * @param sql the SQL statement to execute
	 * @param paramSource container of arguments to bind
	 * @return the corresponding {@link PreparedStatementCreator}
	 * @see #getPreparedStatementCreator(String, SqlParameterSource, Consumer)
	 */
	protected PreparedStatementCreator getPreparedStatementCreator(String sql, SqlParameterSource paramSource) {
		return getPreparedStatementCreator(sql, paramSource, null);
	}

	/**
	 * Build a {@link PreparedStatementCreator} based on the given SQL and named parameters.
	 * <p>Note: Used for the {@code update} variant with generated key handling, and also
	 * delegated from {@link #getPreparedStatementCreator(String, SqlParameterSource)}.
	 * @param sql the SQL statement to execute
	 * @param paramSource container of arguments to bind
	 * @param customizer callback for setting further properties on the
	 * {@link PreparedStatementCreatorFactory} in use, applied before the
	 * actual {@code newPreparedStatementCreator} call
	 * @return the corresponding {@link PreparedStatementCreator}
	 * @since 5.0.5
	 * @see #getParsedSql(String)
	 * @see PreparedStatementCreatorFactory#PreparedStatementCreatorFactory(String, List)
	 * @see PreparedStatementCreatorFactory#newPreparedStatementCreator(Object[])
	 */
	protected PreparedStatementCreator getPreparedStatementCreator(String sql, SqlParameterSource paramSource,
			@Nullable Consumer<PreparedStatementCreatorFactory> customizer) {

		ParsedSql parsedSql = getParsedSql(sql);
		PreparedStatementCreatorFactory pscf = getPreparedStatementCreatorFactory(parsedSql, paramSource);
		if (customizer != null) {
			customizer.accept(pscf);
		}
		@Nullable Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
		return pscf.newPreparedStatementCreator(params);
	}

	/**
	 * Obtain a parsed representation of the given SQL statement.
	 * <p>The default implementation uses an LRU cache with an upper limit of 256 entries.
	 * @param sql the original SQL statement
	 * @return a representation of the parsed SQL statement
	 */
	protected ParsedSql getParsedSql(String sql) {
		Assert.notNull(sql, "SQL must not be null");
		return this.parsedSqlCache.get(sql);
	}

	/**
	 * Build a {@link PreparedStatementCreatorFactory} based on the given SQL and named parameters.
	 * @param parsedSql parsed representation of the given SQL statement
	 * @param paramSource container of arguments to bind
	 * @return the corresponding {@link PreparedStatementCreatorFactory}
	 * @since 5.1.3
	 * @see #getPreparedStatementCreator(String, SqlParameterSource, Consumer)
	 * @see #getParsedSql(String)
	 */
	protected PreparedStatementCreatorFactory getPreparedStatementCreatorFactory(
			ParsedSql parsedSql, SqlParameterSource paramSource) {

		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		List<SqlParameter> declaredParameters = NamedParameterUtils.buildSqlParameterList(parsedSql, paramSource);
		return new PreparedStatementCreatorFactory(sqlToUse, declaredParameters);
	}

}
