/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.lang.Nullable;

/**
 * SQL "function" wrapper for a query that returns a single row of results.
 * The default behavior is to return an int, but that can be overridden by
 * using the constructor with an extra return type parameter.
 *
 * <p>Intended to use to call SQL functions that return a single result using a
 * query like "select user()" or "select sysdate from dual". It is not intended
 * for calling more complex stored functions or for using a CallableStatement to
 * invoke a stored procedure or stored function. Use StoredProcedure or SqlCall
 * for this type of processing.
 *
 * <p>This is a concrete class, which there is often no need to subclass.
 * Code using this package can create an object of this type, declaring SQL
 * and parameters, and then invoke the appropriate {@code run} method
 * repeatedly to execute the function. Subclasses are only supposed to add
 * specialized {@code run} methods for specific parameter and return types.
 *
 * <p>Like all RdbmsOperation objects, SqlFunction objects are thread-safe.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @param <T> the result type
 * @see StoredProcedure
 */
public class SqlFunction<T> extends MappingSqlQuery<T> {

	private final SingleColumnRowMapper<T> rowMapper = new SingleColumnRowMapper<>();


	/**
	 * Constructor to allow use as a JavaBean.
	 * A DataSource, SQL and any parameters must be supplied before
	 * invoking the {@code compile} method and using this object.
	 * @see #setDataSource
	 * @see #setSql
	 * @see #compile
	 */
	public SqlFunction() {
		setRowsExpected(1);
	}

	/**
	 * Create a new SqlFunction object with SQL, but without parameters.
	 * Must add parameters or settle with none.
	 * @param ds the DataSource to obtain connections from
	 * @param sql the SQL to execute
	 */
	public SqlFunction(DataSource ds, String sql) {
		setRowsExpected(1);
		setDataSource(ds);
		setSql(sql);
	}

	/**
	 * Create a new SqlFunction object with SQL and parameters.
	 * @param ds the DataSource to obtain connections from
	 * @param sql the SQL to execute
	 * @param types the SQL types of the parameters, as defined in the
	 * {@code java.sql.Types} class
	 * @see java.sql.Types
	 */
	public SqlFunction(DataSource ds, String sql, int[] types) {
		setRowsExpected(1);
		setDataSource(ds);
		setSql(sql);
		setTypes(types);
	}

	/**
	 * Create a new SqlFunction object with SQL, parameters and a result type.
	 * @param ds the DataSource to obtain connections from
	 * @param sql the SQL to execute
	 * @param types the SQL types of the parameters, as defined in the
	 * {@code java.sql.Types} class
	 * @param resultType the type that the result object is required to match
	 * @see #setResultType(Class)
	 * @see java.sql.Types
	 */
	public SqlFunction(DataSource ds, String sql, int[] types, Class<T> resultType) {
		setRowsExpected(1);
		setDataSource(ds);
		setSql(sql);
		setTypes(types);
		setResultType(resultType);
	}


	/**
	 * Specify the type that the result object is required to match.
	 * <p>If not specified, the result value will be exposed as
	 * returned by the JDBC driver.
	 */
	public void setResultType(Class<T> resultType) {
		this.rowMapper.setRequiredType(resultType);
	}


	/**
	 * This implementation of this method extracts a single value from the
	 * single row returned by the function. If there are a different number
	 * of rows returned, this is treated as an error.
	 */
	@Override
	@Nullable
	protected T mapRow(ResultSet rs, int rowNum) throws SQLException {
		return this.rowMapper.mapRow(rs, rowNum);
	}


	/**
	 * Convenient method to run the function without arguments.
	 * @return the value of the function
	 */
	public int run() {
		return run(new Object[0]);
	}

	/**
	 * Convenient method to run the function with a single int argument.
	 * @param parameter single int parameter
	 * @return the value of the function
	 */
	public int run(int parameter) {
		return run(new Object[] {parameter});
	}

	/**
	 * Analogous to the SqlQuery.execute([]) method. This is a
	 * generic method to execute a query, taken a number of arguments.
	 * @param parameters array of parameters. These will be objects or
	 * object wrapper types for primitives.
	 * @return the value of the function
	 */
	public int run(Object... parameters) {
		Object obj = super.findObject(parameters);
		if (!(obj instanceof Number)) {
			throw new TypeMismatchDataAccessException("Couldn't convert result object [" + obj + "] to int");
		}
		return ((Number) obj).intValue();
	}

	/**
	 * Convenient method to run the function without arguments,
	 * returning the value as an object.
	 * @return the value of the function
	 */
	@Nullable
	public Object runGeneric() {
		return findObject((Object[]) null, null);
	}

	/**
	 * Convenient method to run the function with a single int argument.
	 * @param parameter single int parameter
	 * @return the value of the function as an Object
	 */
	@Nullable
	public Object runGeneric(int parameter) {
		return findObject(parameter);
	}

	/**
	 * Analogous to the {@code SqlQuery.findObject(Object[])} method.
	 * This is a generic method to execute a query, taken a number of arguments.
	 * @param parameters array of parameters. These will be objects or
	 * object wrapper types for primitives.
	 * @return the value of the function, as an Object
	 * @see #execute(Object[])
	 */
	@Nullable
	public Object runGeneric(Object[] parameters) {
		return findObject(parameters);
	}

}
