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

package org.springframework.jdbc.object;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;

/**
 * Reusable operation object representing an SQL query.
 *
 * <p>Subclasses must implement the {@link #newRowMapper} method to provide
 * an object that can extract the results of iterating over the
 * {@code ResultSet} created during the execution of the query.
 *
 * <p>This class provides a number of public {@code execute} methods that are
 * analogous to the different convenient JDO query execute methods. Subclasses
 * can either rely on one of these inherited methods, or can add their own
 * custom execution methods, with meaningful names and typed parameters
 * (definitely a best practice). Each custom query method will invoke one of
 * this class's untyped query methods.
 *
 * <p>Like all {@code RdbmsOperation} classes that ship with the Spring
 * Framework, {@code SqlQuery} instances are thread-safe after their
 * initialization is complete. That is, after they are constructed and configured
 * via their setter methods, they can be used safely from multiple threads.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @author Yanming Zhou
 * @param <T> the result type
 * @see SqlUpdate
 */
public abstract class SqlQuery<T> extends SqlOperation {

	/**
	 * Constructor to allow use as a JavaBean.
	 * <p>The {@code DataSource} and SQL must be supplied before
	 * compilation and use.
	 */
	public SqlQuery() {
	}

	/**
	 * Convenient constructor with a {@code DataSource} and SQL string.
	 * @param ds the {@code DataSource} to use to get connections
	 * @param sql the SQL to execute; SQL can also be supplied at runtime
	 * by overriding the {@link #getSql()} method.
	 */
	public SqlQuery(DataSource ds, String sql) {
		setDataSource(ds);
		setSql(sql);
	}


	/**
	 * Central execution method. All un-named parameter execution goes through this method.
	 * @param params parameters, similar to JDO query parameters.
	 * Primitive parameters must be represented by their Object wrapper type.
	 * The ordering of parameters is significant.
	 * @param context the contextual information passed to the {@code mapRow}
	 * callback method. The JDBC operation itself doesn't rely on this parameter,
	 * but it can be useful for creating the objects of the result list.
	 * @return a List of objects, one per row of the ResultSet. Normally all these
	 * will be of the same class, although it is possible to use different types.
	 */
	public List<T> execute(Object @Nullable [] params, @Nullable Map<?, ?> context) throws DataAccessException {
		validateParameters(params);
		RowMapper<T> rowMapper = newRowMapper(params, context);
		return getJdbcTemplate().query(newPreparedStatementCreator(params), rowMapper);
	}

	/**
	 * Central stream method. All un-named parameter execution goes through this method.
	 * @param params parameters, similar to JDO query parameters.
	 * Primitive parameters must be represented by their Object wrapper type.
	 * The ordering of parameters is significant.
	 * @param context the contextual information passed to the {@code mapRow}
	 * callback method. The JDBC operation itself doesn't rely on this parameter,
	 * but it can be useful for creating the objects of the result list.
	 * @return a result Stream of objects, one per row of the ResultSet. Normally all these
	 * will be of the same class, although it is possible to use different types.
	 * @since 7.0
	 */
	public Stream<T> stream(Object @Nullable [] params, @Nullable Map<?, ?> context) throws DataAccessException {
		validateParameters(params);
		RowMapper<T> rowMapper = newRowMapper(params, context);
		return getJdbcTemplate().queryForStream(newPreparedStatementCreator(params), rowMapper);
	}

	/**
	 * Convenient method to execute without context.
	 * @param params parameters for the query. Primitive parameters must
	 * be represented by their Object wrapper type. The ordering of parameters is
	 * significant.
	 */
	public List<T> execute(Object... params) throws DataAccessException {
		return execute(params, null);
	}

	/**
	 * Convenient method to stream without context.
	 * @param params parameters for the query. Primitive parameters must
	 * be represented by their Object wrapper type. The ordering of parameters is
	 * significant.
	 * @since 7.0
	 */
	public Stream<T> stream(Object... params) throws DataAccessException {
		return stream(params, null);
	}

	/**
	 * Convenient method to execute without parameters.
	 * @param context the contextual information for object creation
	 */
	public List<T> execute(Map<?, ?> context) throws DataAccessException {
		return execute((Object[]) null, context);
	}

	/**
	 * Convenient method to stream without parameters.
	 * @param context the contextual information for object creation
	 * @since 7.0
	 */
	public Stream<T> stream(Map<?, ?> context) throws DataAccessException {
		return stream(null, context);
	}

	/**
	 * Convenient method to execute without parameters nor context.
	 */
	public List<T> execute() throws DataAccessException {
		return execute((Object[]) null, null);
	}

	/**
	 * Convenient method to stream without parameters nor context.
	 * @since 7.0
	 */
	public Stream<T> stream() throws DataAccessException {
		return stream(null, null);
	}

	/**
	 * Convenient method to execute with a single int parameter and context.
	 * @param p1 single int parameter
	 * @param context the contextual information for object creation
	 */
	public List<T> execute(int p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1}, context);
	}

	/**
	 * Convenient method to execute with a single int parameter.
	 * @param p1 single int parameter
	 */
	public List<T> execute(int p1) throws DataAccessException {
		return execute(p1, null);
	}

	/**
	 * Convenient method to execute with two int parameters and context.
	 * @param p1 first int parameter
	 * @param p2 second int parameter
	 * @param context the contextual information for object creation
	 */
	public List<T> execute(int p1, int p2, @Nullable Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1, p2}, context);
	}

	/**
	 * Convenient method to execute with two int parameters.
	 * @param p1 first int parameter
	 * @param p2 second int parameter
	 */
	public List<T> execute(int p1, int p2) throws DataAccessException {
		return execute(p1, p2, null);
	}

	/**
	 * Convenient method to execute with a single long parameter and context.
	 * @param p1 single long parameter
	 * @param context the contextual information for object creation
	 */
	public List<T> execute(long p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1}, context);
	}

	/**
	 * Convenient method to execute with a single long parameter.
	 * @param p1 single long parameter
	 */
	public List<T> execute(long p1) throws DataAccessException {
		return execute(p1, null);
	}

	/**
	 * Convenient method to execute with a single String parameter and context.
	 * @param p1 single String parameter
	 * @param context the contextual information for object creation
	 */
	public List<T> execute(String p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1}, context);
	}

	/**
	 * Convenient method to execute with a single String parameter.
	 * @param p1 single String parameter
	 */
	public List<T> execute(String p1) throws DataAccessException {
		return execute(p1, null);
	}

	/**
	 * Central execution method. All named parameter execution goes through this method.
	 * @param paramMap parameters associated with the name specified while declaring
	 * the SqlParameters. Primitive parameters must be represented by their Object wrapper
	 * type. The ordering of parameters is not significant since they are supplied in a
	 * SqlParameterMap which is an implementation of the Map interface.
	 * @param context the contextual information passed to the {@code mapRow}
	 * callback method. The JDBC operation itself doesn't rely on this parameter,
	 * but it can be useful for creating the objects of the result list.
	 * @return a List of objects, one per row of the ResultSet. Normally all these
	 * will be of the same class, although it is possible to use different types.
	 */
	public List<T> executeByNamedParam(Map<String, ?> paramMap, @Nullable Map<?, ?> context) throws DataAccessException {
		return queryByNamedParam(paramMap, context, getJdbcTemplate()::query);
	}

	/**
	 * Central stream method. All named parameter execution goes through this method.
	 * @param paramMap parameters associated with the name specified while declaring
	 * the SqlParameters. Primitive parameters must be represented by their Object wrapper
	 * type. The ordering of parameters is not significant since they are supplied in a
	 * SqlParameterMap which is an implementation of the Map interface.
	 * @param context the contextual information passed to the {@code mapRow}
	 * callback method. The JDBC operation itself doesn't rely on this parameter,
	 * but it can be useful for creating the objects of the result list.
	 * @return a Stream of objects, one per row of the ResultSet. Normally all these
	 * will be of the same class, although it is possible to use different types.
	 * @since 7.0
	 */
	public Stream<T> streamByNamedParam(Map<String, ?> paramMap, @Nullable Map<?, ?> context) throws DataAccessException {
		return queryByNamedParam(paramMap, context, getJdbcTemplate()::queryForStream);
	}

	/**
	 * Convenient method to execute without context.
	 * @param paramMap parameters associated with the name specified while declaring
	 * the SqlParameters. Primitive parameters must be represented by their Object wrapper
	 * type. The ordering of parameters is not significant.
	 */
	public List<T> executeByNamedParam(Map<String, ? extends @Nullable Object> paramMap) throws DataAccessException {
		return executeByNamedParam(paramMap, null);
	}

	/**
	 * Convenient method to stream without context.
	 * @param paramMap parameters associated with the name specified while declaring
	 * the SqlParameters. Primitive parameters must be represented by their Object wrapper
	 * type. The ordering of parameters is not significant.
	 * @since 7.0
	 */
	public Stream<T> streamByNamedParam(Map<String, ? extends @Nullable Object> paramMap) throws DataAccessException {
		return streamByNamedParam(paramMap, null);
	}

	private <R> R queryByNamedParam(Map<String, ?> paramMap, @Nullable Map<?, ?> context, BiFunction<PreparedStatementCreator, RowMapper<T>, R> queryFunction) {
		validateNamedParameters(paramMap);
		ParsedSql parsedSql = getParsedSql();
		MapSqlParameterSource paramSource = new MapSqlParameterSource(paramMap);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		@Nullable Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, getDeclaredParameters());
		RowMapper<T> rowMapper = newRowMapper(params, context);
		return queryFunction.apply(newPreparedStatementCreator(sqlToUse, params), rowMapper);
	}


	/**
	 * Generic object finder method, used by all other {@code findObject} methods.
	 * Object finder methods are like EJB entity bean finders, in that it is
	 * considered an error if they return more than one result.
	 * @return the result object, or {@code null} if not found. Subclasses may
	 * choose to treat this as an error and throw an exception.
	 * @see org.springframework.dao.support.DataAccessUtils#singleResult
	 */
	public @Nullable T findObject(Object @Nullable [] params, @Nullable Map<?, ?> context) throws DataAccessException {
		List<T> results = execute(params, context);
		return DataAccessUtils.singleResult(results);
	}

	/**
	 * Convenient method to find a single object without context.
	 */
	public @Nullable T findObject(Object... params) throws DataAccessException {
		return findObject(params, null);
	}

	/**
	 * Convenient method to find a single object given a single int parameter
	 * and a context.
	 */
	public @Nullable T findObject(int p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return findObject(new Object[] {p1}, context);
	}

	/**
	 * Convenient method to find a single object given a single int parameter.
	 */
	public @Nullable T findObject(int p1) throws DataAccessException {
		return findObject(p1, null);
	}

	/**
	 * Convenient method to find a single object given two int parameters
	 * and a context.
	 */
	public @Nullable T findObject(int p1, int p2, @Nullable Map<?, ?> context) throws DataAccessException {
		return findObject(new Object[] {p1, p2}, context);
	}

	/**
	 * Convenient method to find a single object given two int parameters.
	 */
	public @Nullable T findObject(int p1, int p2) throws DataAccessException {
		return findObject(p1, p2, null);
	}

	/**
	 * Convenient method to find a single object given a single long parameter
	 * and a context.
	 */
	public @Nullable T findObject(long p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return findObject(new Object[] {p1}, context);
	}

	/**
	 * Convenient method to find a single object given a single long parameter.
	 */
	public @Nullable T findObject(long p1) throws DataAccessException {
		return findObject(p1, null);
	}

	/**
	 * Convenient method to find a single object given a single String parameter
	 * and a context.
	 */
	public @Nullable T findObject(String p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return findObject(new Object[] {p1}, context);
	}

	/**
	 * Convenient method to find a single object given a single String parameter.
	 */
	public @Nullable T findObject(String p1) throws DataAccessException {
		return findObject(p1, null);
	}

	/**
	 * Generic object finder method for named parameters.
	 * @param paramMap a Map of parameter name to parameter object,
	 * matching named parameters specified in the SQL statement.
	 * Ordering is not significant.
	 * @param context the contextual information passed to the {@code mapRow}
	 * callback method. The JDBC operation itself doesn't rely on this parameter,
	 * but it can be useful for creating the objects of the result list.
	 * @return a List of objects, one per row of the ResultSet. Normally all these
	 * will be of the same class, although it is possible to use different types.
	 */
	public @Nullable T findObjectByNamedParam(Map<String, ?> paramMap, @Nullable Map<?, ?> context) throws DataAccessException {
		List<T> results = executeByNamedParam(paramMap, context);
		return DataAccessUtils.singleResult(results);
	}

	/**
	 * Convenient method to execute without context.
	 * @param paramMap a Map of parameter name to parameter object,
	 * matching named parameters specified in the SQL statement.
	 * Ordering is not significant.
	 */
	public @Nullable T findObjectByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
		return findObjectByNamedParam(paramMap, null);
	}


	/**
	 * Subclasses must implement this method to extract an object per row, to be
	 * returned by the {@code execute} method as an aggregated {@link List}.
	 * @param parameters the parameters to the {@code execute()} method,
	 * in case subclass is interested; may be {@code null} if there
	 * were no parameters.
	 * @param context the contextual information passed to the {@code mapRow}
	 * callback method. The JDBC operation itself doesn't rely on this parameter,
	 * but it can be useful for creating the objects of the result list.
	 * @see #execute
	 */
	protected abstract RowMapper<T> newRowMapper(@Nullable Object @Nullable [] parameters, @Nullable Map<?, ?> context);

}
