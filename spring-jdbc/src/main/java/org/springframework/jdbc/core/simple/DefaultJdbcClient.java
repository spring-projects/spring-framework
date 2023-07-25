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

package org.springframework.jdbc.core.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SimplePropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.Assert;

/**
 * The default implementation of {@link JdbcClient},
 * as created by the static factory methods.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see JdbcClient#create(DataSource)
 * @see JdbcClient#create(JdbcOperations)
 * @see JdbcClient#create(NamedParameterJdbcOperations)
 */
final class DefaultJdbcClient implements JdbcClient {

	private final JdbcOperations classicOps;

	private final NamedParameterJdbcOperations namedParamOps;


	public DefaultJdbcClient(DataSource dataSource) {
		this.classicOps = new JdbcTemplate(dataSource);
		this.namedParamOps = new NamedParameterJdbcTemplate(this.classicOps);
	}

	public DefaultJdbcClient(JdbcOperations jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.classicOps = jdbcTemplate;
		this.namedParamOps = new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	public DefaultJdbcClient(NamedParameterJdbcOperations jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.classicOps = jdbcTemplate.getJdbcOperations();
		this.namedParamOps = jdbcTemplate;
	}


	@Override
	public StatementSpec sql(String sql) {
		return new DefaultStatementSpec(sql);
	}


	private class DefaultStatementSpec implements StatementSpec {

		private final String sql;

		private final List<Object> indexedParams = new ArrayList<>();

		private final MapSqlParameterSource namedParams = new MapSqlParameterSource();

		private SqlParameterSource namedParamSource = this.namedParams;

		public DefaultStatementSpec(String sql) {
			this.sql = sql;
		}

		@Override
		public StatementSpec param(Object value) {
			this.indexedParams.add(value);
			return this;
		}

		@Override
		public StatementSpec param(int jdbcIndex, Object value) {
			if (jdbcIndex < 1) {
				throw new IllegalArgumentException("Invalid JDBC index: needs to start at 1");
			}
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

		@Override
		public StatementSpec param(int jdbcIndex, Object value, int sqlType) {
			return param(jdbcIndex, new SqlParameterValue(sqlType, value));
		}

		@Override
		public StatementSpec param(String name, Object value) {
			this.namedParams.addValue(name, value);
			return this;
		}

		@Override
		public StatementSpec param(String name, Object value, int sqlType) {
			this.namedParams.addValue(name, value, sqlType);
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
		public <T> MappedQuerySpec<T> query(RowMapper<T> rowMapper) {
			return (useNamedParams() ?
					new NamedParamMappedQuerySpec<>(rowMapper) :
					new IndexedParamMappedQuerySpec<>(rowMapper));
		}

		@Override
		public void query(RowCallbackHandler rch) {
			if (useNamedParams()) {
				namedParamOps.query(this.sql, this.namedParams, rch);
			}
			else {
				classicOps.query(this.sql, rch, this.indexedParams.toArray());
			}
		}

		@Override
		public <T> T query(ResultSetExtractor<T> rse) {
			T result = (useNamedParams() ?
					namedParamOps.query(this.sql, this.namedParams, rse) :
					classicOps.query(this.sql, rse, this.indexedParams.toArray()));
			Assert.state(result != null, "No result from ResultSetExtractor");
			return result;
		}

		@Override
		public int update() {
			return (useNamedParams() ?
					namedParamOps.update(this.sql, this.namedParamSource) :
					classicOps.update(this.sql, this.indexedParams.toArray()));
		}

		@Override
		public int update(KeyHolder generatedKeyHolder) {
			return (useNamedParams() ?
					namedParamOps.update(this.sql, this.namedParamSource, generatedKeyHolder) :
					classicOps.update(this.sql, this.indexedParams.toArray(), generatedKeyHolder));
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


		private class IndexedParamResultQuerySpec implements ResultQuerySpec {

			@Override
			public SqlRowSet rowSet() {
				return classicOps.queryForRowSet(sql, indexedParams.toArray());
			}

			@Override
			public List<Map<String, Object>> listOfRows() {
				return classicOps.queryForList(sql, indexedParams.toArray());
			}

			@Override
			public Map<String, Object> singleRow() {
				return classicOps.queryForMap(sql, indexedParams.toArray());
			}

			@Override
			public <T> List<T> singleColumn(Class<T> requiredType) {
				return classicOps.queryForList(sql, requiredType, indexedParams.toArray());
			}
		}


		private class NamedParamResultQuerySpec implements ResultQuerySpec {

			@Override
			public SqlRowSet rowSet() {
				return namedParamOps.queryForRowSet(sql, namedParamSource);
			}

			@Override
			public List<Map<String, Object>> listOfRows() {
				return namedParamOps.queryForList(sql, namedParamSource);
			}

			@Override
			public Map<String, Object> singleRow() {
				return namedParamOps.queryForMap(sql, namedParamSource);
			}

			@Override
			public <T> List<T> singleColumn(Class<T> requiredType) {
				return namedParamOps.queryForList(sql, namedParamSource, requiredType);
			}
		}


		private class IndexedParamMappedQuerySpec<T> implements MappedQuerySpec<T> {

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


		private class NamedParamMappedQuerySpec<T> implements MappedQuerySpec<T> {

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
