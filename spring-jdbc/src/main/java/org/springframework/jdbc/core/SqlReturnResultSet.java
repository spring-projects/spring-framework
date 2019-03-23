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

package org.springframework.jdbc.core;

/**
 * Represents a returned {@link java.sql.ResultSet} from a stored procedure call.
 *
 * <p>A {@link ResultSetExtractor}, {@link RowCallbackHandler} or {@link RowMapper}
 * must be provided to handle any returned rows.
 *
 * <p>Returned {@link java.sql.ResultSet ResultSets} - like all stored procedure
 * parameters - must have names.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class SqlReturnResultSet extends ResultSetSupportingSqlParameter {

	/**
	 * Create a new instance of the {@link SqlReturnResultSet} class.
	 * @param name name of the parameter, as used in input and output maps
	 * @param extractor ResultSetExtractor to use for parsing the {@link java.sql.ResultSet}
	 */
	public SqlReturnResultSet(String name, ResultSetExtractor<?> extractor) {
		super(name, 0, extractor);
	}

	/**
	 * Create a new instance of the {@link SqlReturnResultSet} class.
	 * @param name name of the parameter, as used in input and output maps
	 * @param handler RowCallbackHandler to use for parsing the {@link java.sql.ResultSet}
	 */
	public SqlReturnResultSet(String name, RowCallbackHandler handler) {
		super(name, 0, handler);
	}

	/**
	 * Create a new instance of the {@link SqlReturnResultSet} class.
	 * @param name name of the parameter, as used in input and output maps
	 * @param mapper RowMapper to use for parsing the {@link java.sql.ResultSet}
	 */
	public SqlReturnResultSet(String name, RowMapper<?> mapper) {
		super(name, 0, mapper);
	}


	/**
	 * This implementation always returns {@code true}.
	 */
	@Override
	public boolean isResultsParameter() {
		return true;
	}

}
