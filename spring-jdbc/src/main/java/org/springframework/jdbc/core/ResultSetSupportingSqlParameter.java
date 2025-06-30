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

package org.springframework.jdbc.core;

import java.sql.ResultSet;

import org.jspecify.annotations.Nullable;

/**
 * Common base class for ResultSet-supporting SqlParameters like
 * {@link SqlOutParameter} and {@link SqlReturnResultSet}.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public class ResultSetSupportingSqlParameter extends SqlParameter {

	private @Nullable ResultSetExtractor<?> resultSetExtractor;

	private @Nullable RowCallbackHandler rowCallbackHandler;

	private @Nullable RowMapper<?> rowMapper;


	/**
	 * Create a new ResultSetSupportingSqlParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType) {
		super(name, sqlType);
	}

	/**
	 * Create a new ResultSetSupportingSqlParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param scale the number of digits after the decimal point
	 * (for DECIMAL and NUMERIC types)
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, int scale) {
		super(name, sqlType, scale);
	}

	/**
	 * Create a new ResultSetSupportingSqlParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param typeName the type name of the parameter (optional)
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, @Nullable String typeName) {
		super(name, sqlType, typeName);
	}

	/**
	 * Create a new ResultSetSupportingSqlParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rse the {@link ResultSetExtractor} to use for parsing the {@link ResultSet}
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
		super(name, sqlType);
		this.resultSetExtractor = rse;
	}

	/**
	 * Create a new ResultSetSupportingSqlParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rch the {@link RowCallbackHandler} to use for parsing the {@link ResultSet}
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, RowCallbackHandler rch) {
		super(name, sqlType);
		this.rowCallbackHandler = rch;
	}

	/**
	 * Create a new ResultSetSupportingSqlParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rm the {@link RowMapper} to use for parsing the {@link ResultSet}
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, RowMapper<?> rm) {
		super(name, sqlType);
		this.rowMapper = rm;
	}


	/**
	 * Does this parameter support a ResultSet, i.e. does it hold a
	 * ResultSetExtractor, RowCallbackHandler or RowMapper?
	 */
	public boolean isResultSetSupported() {
		return (this.resultSetExtractor != null || this.rowCallbackHandler != null || this.rowMapper != null);
	}

	/**
	 * Return the ResultSetExtractor held by this parameter, if any.
	 */
	public @Nullable ResultSetExtractor<?> getResultSetExtractor() {
		return this.resultSetExtractor;
	}

	/**
	 * Return the RowCallbackHandler held by this parameter, if any.
	 */
	public @Nullable RowCallbackHandler getRowCallbackHandler() {
		return this.rowCallbackHandler;
	}

	/**
	 * Return the RowMapper held by this parameter, if any.
	 */
	public @Nullable RowMapper<?> getRowMapper() {
		return this.rowMapper;
	}


	/**
	 * This implementation always returns {@code false}.
	 */
	@Override
	public boolean isInputValueProvided() {
		return false;
	}

}
