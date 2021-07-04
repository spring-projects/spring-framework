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

import java.sql.Types;

/**
 * Represents a returned update count from a stored procedure call.
 *
 * <p>Returned update counts - like all stored procedure
 * parameters - <b>must</b> have names.
 *
 * @author Thomas Risberg
 */
public class SqlReturnUpdateCount extends SqlParameter {

	/**
	 * Create a new SqlReturnUpdateCount.
	 * @param name the name of the parameter, as used in input and output maps
	 */
	public SqlReturnUpdateCount(String name) {
		super(name, Types.INTEGER);
	}


	/**
	 * This implementation always returns {@code false}.
	 */
	@Override
	public boolean isInputValueProvided() {
		return false;
	}

	/**
	 * This implementation always returns {@code true}.
	 */
	@Override
	public boolean isResultsParameter() {
		return true;
	}

}
