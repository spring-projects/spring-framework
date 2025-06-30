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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.core.RowMapper;

/**
 * Reusable RDBMS query in which concrete subclasses must implement
 * the abstract updateRow(ResultSet, int, context) method to update each
 * row of the JDBC ResultSet and optionally map contents into an object.
 *
 * <p>Subclasses can be constructed providing SQL, parameter types
 * and a DataSource. SQL will often vary between subclasses.
 *
 * @author Thomas Risberg
 * @param <T> the result type
 * @see org.springframework.jdbc.object.SqlQuery
 */
public abstract class UpdatableSqlQuery<T> extends SqlQuery<T> {

	/**
	 * Constructor to allow use as a JavaBean.
	 */
	public UpdatableSqlQuery() {
		setUpdatableResults(true);
	}

	/**
	 * Convenient constructor with DataSource and SQL string.
	 * @param ds the DataSource to use to get connections
	 * @param sql the SQL to run
	 */
	public UpdatableSqlQuery(DataSource ds, String sql) {
		super(ds, sql);
		setUpdatableResults(true);
	}


	/**
	 * Implementation of the superclass template method. This invokes the subclass's
	 * implementation of the {@code updateRow()} method.
	 */
	@Override
	protected RowMapper<T> newRowMapper(@Nullable Object @Nullable [] parameters, @Nullable Map<?, ?> context) {
		return new RowMapperImpl(context);
	}

	/**
	 * Subclasses must implement this method to update each row of the
	 * ResultSet and optionally create object of the result type.
	 * @param rs the ResultSet we're working through
	 * @param rowNum row number (from 0) we're up to
	 * @param context passed to the {@code execute()} method.
	 * It can be {@code null} if no contextual information is need.  If you
	 * need to pass in data for each row, you can pass in a HashMap with
	 * the primary key of the row being the key for the HashMap.  That way
	 * it is easy to locate the updates for each row
	 * @return an object of the result type
	 * @throws SQLException if there's an error updating data.
	 * Subclasses can simply not catch SQLExceptions, relying on the
	 * framework to clean up.
	 */
	protected abstract T updateRow(ResultSet rs, int rowNum, @Nullable Map<?, ?> context) throws SQLException;


	/**
	 * Implementation of RowMapper that calls the enclosing
	 * class's {@code updateRow()} method for each row.
	 */
	protected class RowMapperImpl implements RowMapper<T> {

		private final @Nullable Map<?, ?> context;

		public RowMapperImpl(@Nullable Map<?, ?> context) {
			this.context = context;
		}

		@Override
		public T mapRow(ResultSet rs, int rowNum) throws SQLException {
			T result = updateRow(rs, rowNum, this.context);
			rs.updateRow();
			return result;
		}
	}

}
