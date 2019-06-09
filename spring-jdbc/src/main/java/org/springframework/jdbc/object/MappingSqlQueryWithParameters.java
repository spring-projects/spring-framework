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

package org.springframework.jdbc.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;

/**
 * Reusable RDBMS query in which concrete subclasses must implement
 * the abstract mapRow(ResultSet, int) method to map each row of
 * the JDBC ResultSet into an object.
 *
 * <p>Such manual mapping is usually preferable to "automatic"
 * mapping using reflection, which can become complex in non-trivial
 * cases. For example, the present class allows different objects
 * to be used for different rows (for example, if a subclass is indicated).
 * It allows computed fields to be set. And there's no need for
 * ResultSet columns to have the same names as bean properties.
 * The Pareto Principle in action: going the extra mile to automate
 * the extraction process makes the framework much more complex
 * and delivers little real benefit.
 *
 * <p>Subclasses can be constructed providing SQL, parameter types
 * and a DataSource. SQL will often vary between subclasses.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Jean-Pierre Pawlak
 * @param <T> the result type
 * @see org.springframework.jdbc.object.MappingSqlQuery
 * @see org.springframework.jdbc.object.SqlQuery
 */
public abstract class MappingSqlQueryWithParameters<T> extends SqlQuery<T> {

	/**
	 * Constructor to allow use as a JavaBean.
	 */
	public MappingSqlQueryWithParameters() {
	}

	/**
	 * Convenient constructor with DataSource and SQL string.
	 * @param ds the DataSource to use to get connections
	 * @param sql the SQL to run
	 */
	public MappingSqlQueryWithParameters(DataSource ds, String sql) {
		super(ds, sql);
	}


	/**
	 * Implementation of protected abstract method. This invokes the subclass's
	 * implementation of the mapRow() method.
	 */
	@Override
	protected RowMapper<T> newRowMapper(@Nullable Object[] parameters, @Nullable Map<?, ?> context) {
		return new RowMapperImpl(parameters, context);
	}

	/**
	 * Subclasses must implement this method to convert each row
	 * of the ResultSet into an object of the result type.
	 * @param rs the ResultSet we're working through
	 * @param rowNum row number (from 0) we're up to
	 * @param parameters to the query (passed to the execute() method).
	 * Subclasses are rarely interested in these.
	 * It can be {@code null} if there are no parameters.
	 * @param context passed to the execute() method.
	 * It can be {@code null} if no contextual information is need.
	 * @return an object of the result type
	 * @throws SQLException if there's an error extracting data.
	 * Subclasses can simply not catch SQLExceptions, relying on the
	 * framework to clean up.
	 */
	@Nullable
	protected abstract T mapRow(ResultSet rs, int rowNum, @Nullable Object[] parameters, @Nullable Map<?, ?> context)
			throws SQLException;


	/**
	 * Implementation of RowMapper that calls the enclosing
	 * class's {@code mapRow} method for each row.
	 */
	protected class RowMapperImpl implements RowMapper<T> {

		@Nullable
		private final Object[] params;

		@Nullable
		private final Map<?, ?> context;

		/**
		 * Use an array results. More efficient if we know how many results to expect.
		 */
		public RowMapperImpl(@Nullable Object[] parameters, @Nullable Map<?, ?> context) {
			this.params = parameters;
			this.context = context;
		}

		@Override
		@Nullable
		public T mapRow(ResultSet rs, int rowNum) throws SQLException {
			return MappingSqlQueryWithParameters.this.mapRow(rs, rowNum, this.params, this.context);
		}
	}

}
