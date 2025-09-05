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

package org.springframework.jdbc.core.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SimplePropertyRowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SimplePropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.Assert;

/**
 * The default implementation of {@link JdbcClient},
 * as created by the static factory methods.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 6.1
 * @see JdbcClient#create(DataSource)
 * @see JdbcClient#create(JdbcOperations)
 * @see JdbcClient#create(NamedParameterJdbcOperations)
 */
final class DefaultJdbcClient implements JdbcClient {

	private final NamedParameterJdbcOperations namedParamOps;

	private final ConversionService conversionService;

	private final Map<Class<?>, RowMapper<?>> rowMapperCache = new ConcurrentHashMap<>();


	public DefaultJdbcClient(DataSource dataSource) {
		this(new JdbcTemplate(dataSource));
	}

	public DefaultJdbcClient(JdbcOperations jdbcTemplate) {
		this(new NamedParameterJdbcTemplate(jdbcTemplate), null);
	}

	public DefaultJdbcClient(NamedParameterJdbcOperations jdbcTemplate, @Nullable ConversionService conversionService) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.namedParamOps = jdbcTemplate;
		this.conversionService =
				(conversionService != null ? conversionService : DefaultConversionService.getSharedInstance());
	}


	@Override
	public StatementSpec sql(String sql) {
		return new DefaultStatementSpec(sql, this.namedParamOps);
	}


	private class DefaultStatementSpec implements StatementSpec {

		private final String sql;

		private JdbcOperations classicOps;

		private NamedParameterJdbcOperations namedParamOps;

		private @Nullable JdbcTemplate customTemplate;

		private final List<@Nullable Object> indexedParams = new ArrayList<>();

		private final MapSqlParameterSource namedParams = new MapSqlParameterSource();

		private SqlParameterSource namedParamSource = this.namedParams;

		public DefaultStatementSpec(String sql, NamedParameterJdbcOperations namedParamOps) {
			this.sql = sql;
			this.classicOps = namedParamOps.getJdbcOperations();
			this.namedParamOps = namedParamOps;
		}

		private JdbcTemplate enforceCustomTemplate() {
			if (this.customTemplate == null) {
				if (!(this.classicOps instanceof JdbcAccessor original)) {
					throw new IllegalStateException(
							"Needs to be bound to a JdbcAccessor for custom settings support: " + this.classicOps);
				}
				this.customTemplate = new JdbcTemplate(original);
				this.classicOps = this.customTemplate;
				this.namedParamOps = (this.namedParamOps instanceof NamedParameterJdbcTemplate originalNamedParam ?
						new NamedParameterJdbcTemplate(originalNamedParam, this.customTemplate) :
						new NamedParameterJdbcTemplate(this.customTemplate));
			}
			return this.customTemplate;
		}

		@Override
		public StatementSpec withFetchSize(int fetchSize) {
			enforceCustomTemplate().setFetchSize(fetchSize);
			return this;
		}

		@Override
		public StatementSpec withMaxRows(int maxRows) {
			enforceCustomTemplate().setMaxRows(maxRows);
			return this;
		}

		@Override
		public StatementSpec withQueryTimeout(int queryTimeout) {
			enforceCustomTemplate().setQueryTimeout(queryTimeout);
			return this;
		}

		@Override
		public StatementSpec param(@Nullable Object value) {
			validateIndexedParamValue(value);
			this.indexedParams.add(value);
			return this;
		}

		@Override
		public StatementSpec param(int jdbcIndex, @Nullable Object value) {
			if (jdbcIndex < 1) {
				throw new IllegalArgumentException("Invalid JDBC index: needs to start at 1");
			}
			validateIndexedParamValue(value);
			int index = jdbcIndex - 1;
			int size = this.indexedParams.size();
			if (index < size) {
				this.indexedParams.set(index, value);
			}
			else {
				for (int i = size; i < index; i++) {
					this.indexedParams.add(null);
				}
				this.indexedParams.add(value);
			}
			return this;
		}

		private void validateIndexedParamValue(@Nullable Object value) {
			if (value instanceof Iterable) {
				throw new IllegalArgumentException("Invalid positional parameter value of type Iterable (" +
						value.getClass().getSimpleName() +
						"): Parameter expansion is only supported with named parameters.");
			}
		}

		@Override
		public StatementSpec param(int jdbcIndex, @Nullable Object value, int sqlType) {
			return param(jdbcIndex, new SqlParameterValue(sqlType, value));
		}

		@Override
		public StatementSpec param(String name, @Nullable Object value) {
			this.namedParams.addValue(name, value);
			return this;
		}

		@Override
		public StatementSpec param(String name, @Nullable Object value, int sqlType) {
			this.namedParams.addValue(name, value, sqlType);
			return this;
		}

		@Override
		public StatementSpec params(Object... values) {
			Collections.addAll(this.indexedParams, values);
			return this;
		}

		@Override
		public StatementSpec params(List<?> values) {
			this.indexedParams.addAll(values);
			return this;
		}

		@Override
		public StatementSpec params(Map<String, ?> paramMap) {
			this.namedParams.addValues(paramMap);
			return this;
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		@Override
		public StatementSpec paramSource(Object namedParamObject) {
			this.namedParamSource = (namedParamObject instanceof Map map ?
					new MapSqlParameterSource(map) :
					new SimplePropertySqlParameterSource(namedParamObject));
			return this;
		}

		@Override
		public StatementSpec paramSource(SqlParameterSource namedParamSource) {
			this.namedParamSource = namedParamSource;
			return this;
		}

		@Override
		public ResultQuerySpec query() {
			return (useNamedParams() ?
					new NamedParamResultQuerySpec() :
					new IndexedParamResultQuerySpec());
		}

		@Override
		@SuppressWarnings({"unchecked", "NullAway"}) // See https://github.com/uber/NullAway/issues/1075
		public <T> MappedQuerySpec<@Nullable T> query(Class<T> mappedClass) {
			RowMapper<?> rowMapper = rowMapperCache.computeIfAbsent(mappedClass, key ->
					BeanUtils.isSimpleProperty(mappedClass) ?
							new SingleColumnRowMapper<>(mappedClass, conversionService) :
							new SimplePropertyRowMapper<>(mappedClass, conversionService));
			return query((RowMapper<@Nullable T>) rowMapper);
		}

		@Override
		public <T extends @Nullable Object> MappedQuerySpec<T> query(RowMapper<T> rowMapper) {
			return (useNamedParams() ?
					new NamedParamMappedQuerySpec<>(rowMapper) :
					new IndexedParamMappedQuerySpec<>(rowMapper));
		}

		@Override
		public void query(RowCallbackHandler rch) {
			if (useNamedParams()) {
				this.namedParamOps.query(this.sql, this.namedParamSource, rch);
			}
			else {
				this.classicOps.query(statementCreatorForIndexedParams(), rch);
			}
		}

		@Override
		public <T extends @Nullable Object> T query(ResultSetExtractor<T> rse) {
			T result = (useNamedParams() ?
					this.namedParamOps.query(this.sql, this.namedParamSource, rse) :
					this.classicOps.query(statementCreatorForIndexedParams(), rse));
			Assert.state(result != null, "No result from ResultSetExtractor");
			return result;
		}

		@Override
		public int update() {
			return (useNamedParams() ?
					this.namedParamOps.update(this.sql, this.namedParamSource) :
					this.classicOps.update(statementCreatorForIndexedParams()));
		}

		@Override
		public int update(KeyHolder generatedKeyHolder) {
			return (useNamedParams() ?
					this.namedParamOps.update(this.sql, this.namedParamSource, generatedKeyHolder) :
					this.classicOps.update(statementCreatorForIndexedParamsWithKeys(null), generatedKeyHolder));
		}

		@Override
		public int update(KeyHolder generatedKeyHolder, String... keyColumnNames) {
			return (useNamedParams() ?
					this.namedParamOps.update(this.sql, this.namedParamSource, generatedKeyHolder, keyColumnNames) :
					this.classicOps.update(statementCreatorForIndexedParamsWithKeys(keyColumnNames), generatedKeyHolder));
		}

		private boolean useNamedParams() {
			boolean hasNamedParams = (this.namedParams.hasValues() || this.namedParamSource != this.namedParams);
			if (hasNamedParams && !this.indexedParams.isEmpty()) {
				throw new IllegalStateException("Configure either named or indexed parameters, not both");
			}
			if (this.namedParams.hasValues() && this.namedParamSource != this.namedParams) {
				throw new IllegalStateException(
						"Configure either individual named parameters or a SqlParameterSource, not both");
			}
			return hasNamedParams;
		}

		private PreparedStatementCreator statementCreatorForIndexedParams() {
			return new PreparedStatementCreatorFactory(this.sql).newPreparedStatementCreator(this.indexedParams);
		}

		private PreparedStatementCreator statementCreatorForIndexedParamsWithKeys(String @Nullable [] keyColumnNames) {
			PreparedStatementCreatorFactory pscf = new PreparedStatementCreatorFactory(this.sql);
			if (keyColumnNames != null) {
				pscf.setGeneratedKeysColumnNames(keyColumnNames);
			}
			else {
				pscf.setReturnGeneratedKeys(true);
			}
			return pscf.newPreparedStatementCreator(this.indexedParams);
		}


		private class IndexedParamResultQuerySpec implements ResultQuerySpec {

			@Override
			public SqlRowSet rowSet() {
				return classicOps.queryForRowSet(sql, indexedParams.toArray());
			}

			@Override
			public List<Map<String, @Nullable Object>> listOfRows() {
				return classicOps.queryForList(sql, indexedParams.toArray());
			}

			@Override
			public Map<String, @Nullable Object> singleRow() {
				return classicOps.queryForMap(sql, indexedParams.toArray());
			}

			@Override
			@SuppressWarnings("NullAway") // See https://github.com/uber/NullAway/issues/1075
			public List<@Nullable Object> singleColumn() {
				return classicOps.queryForList(sql, Object.class, indexedParams.toArray());
			}
		}


		private class NamedParamResultQuerySpec implements ResultQuerySpec {

			@Override
			public SqlRowSet rowSet() {
				return namedParamOps.queryForRowSet(sql, namedParamSource);
			}

			@Override
			public List<Map<String, @Nullable Object>> listOfRows() {
				return namedParamOps.queryForList(sql, namedParamSource);
			}

			@Override
			@SuppressWarnings("NullAway") // See https://github.com/uber/NullAway/issues/1075
			public Map<String, @Nullable Object> singleRow() {
				return namedParamOps.queryForMap(sql, namedParamSource);
			}

			@Override
			@SuppressWarnings("NullAway") // See https://github.com/uber/NullAway/issues/1075
			public List<@Nullable Object> singleColumn() {
				return namedParamOps.queryForList(sql, namedParamSource, Object.class);
			}
		}


		private class IndexedParamMappedQuerySpec<T extends @Nullable Object> implements MappedQuerySpec<T> {

			private final RowMapper<T> rowMapper;

			public IndexedParamMappedQuerySpec(RowMapper<T> rowMapper) {
				this.rowMapper = rowMapper;
			}

			@Override
			public Stream<T> stream() {
				return classicOps.queryForStream(sql, this.rowMapper, indexedParams.toArray());
			}

			@Override
			public List<T> list() {
				return classicOps.query(sql, this.rowMapper, indexedParams.toArray());
			}
		}


		private class NamedParamMappedQuerySpec<T extends @Nullable Object> implements MappedQuerySpec<T> {

			private final RowMapper<T> rowMapper;

			public NamedParamMappedQuerySpec(RowMapper<T> rowMapper) {
				this.rowMapper = rowMapper;
			}

			@Override
			public Stream<T> stream() {
				return namedParamOps.queryForStream(sql, namedParamSource, this.rowMapper);
			}

			@Override
			public List<T> list() {
				return namedParamOps.query(sql, namedParamSource, this.rowMapper);
			}
		}
	}

}
