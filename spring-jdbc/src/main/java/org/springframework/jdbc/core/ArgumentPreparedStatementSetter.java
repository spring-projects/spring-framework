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

package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.lang.Nullable;

/**
 * Simple adapter for {@link PreparedStatementSetter} that applies a given array of arguments.
 *
 * @author Juergen Hoeller
 * @since 3.2.3
 */
public class ArgumentPreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {

	@Nullable
	private final Object[] args;


	/**
	 * Create a new ArgPreparedStatementSetter for the given arguments.
	 * @param args the arguments to set
	 */
	public ArgumentPreparedStatementSetter(@Nullable Object[] args) {
		this.args = args;
	}


	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		if (this.args != null) {
			for (int i = 0; i < this.args.length; i++) {
				Object arg = this.args[i];
				doSetValue(ps, i + 1, arg);
			}
		}
	}

	/**
	 * Set the value for prepared statements specified parameter index using the passed in value.
	 * This method can be overridden by sub-classes if needed.
	 * @param ps the PreparedStatement
	 * @param parameterPosition index of the parameter position
	 * @param argValue the value to set
	 * @throws SQLException if thrown by PreparedStatement methods
	 */
	protected void doSetValue(PreparedStatement ps, int parameterPosition, Object argValue) throws SQLException {
		if (argValue instanceof SqlParameterValue) {
			SqlParameterValue paramValue = (SqlParameterValue) argValue;
			StatementCreatorUtils.setParameterValue(ps, parameterPosition, paramValue, paramValue.getValue());
		}
		else {
			StatementCreatorUtils.setParameterValue(ps, parameterPosition, SqlTypeValue.TYPE_UNKNOWN, argValue);
		}
	}

	@Override
	public void cleanupParameters() {
		StatementCreatorUtils.cleanupParameters(this.args);
	}

}
