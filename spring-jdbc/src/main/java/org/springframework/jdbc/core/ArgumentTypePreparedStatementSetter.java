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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Simple adapter for {@link PreparedStatementSetter} that applies the given
 * arrays of arguments and JDBC argument types.
 *
 * @author Juergen Hoeller
 * @since 3.2.3
 */
public class ArgumentTypePreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {

	private final @Nullable Object @Nullable [] args;

	private final int @Nullable [] argTypes;


	/**
	 * Create a new {@code ArgumentTypePreparedStatementSetter} for the given
	 * arguments and types.
	 * @param args the arguments to set
	 * @param argTypes the corresponding SQL types of the arguments
	 */
	public ArgumentTypePreparedStatementSetter(@Nullable Object @Nullable [] args, int @Nullable [] argTypes) {
		if ((args == null && argTypes != null) || (args != null && (argTypes == null || args.length != argTypes.length))) {
			throw new InvalidDataAccessApiUsageException("args and argTypes parameters must match");
		}
		this.args = args;
		this.argTypes = argTypes;
	}


	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		int parameterPosition = 1;
		if (this.args != null && this.argTypes != null) {
			for (int i = 0; i < this.args.length; i++) {
				Object arg = this.args[i];
				if (arg instanceof Collection<?> entries && this.argTypes[i] != Types.ARRAY) {
					for (Object entry : entries) {
						if (entry instanceof Object[] valueArray) {
							for (Object argValue : valueArray) {
								doSetValue(ps, parameterPosition, this.argTypes[i], argValue);
								parameterPosition++;
							}
						}
						else {
							doSetValue(ps, parameterPosition, this.argTypes[i], entry);
							parameterPosition++;
						}
					}
				}
				else {
					doSetValue(ps, parameterPosition, this.argTypes[i], arg);
					parameterPosition++;
				}
			}
		}
	}

	/**
	 * Set the value for the prepared statement's specified parameter position
	 * using the supplied value and type.
	 * <p>This method can be overridden by subclasses if needed.
	 * @param ps the PreparedStatement
	 * @param parameterPosition index of the parameter position
	 * @param argType the argument type
	 * @param argValue the argument value
	 * @throws SQLException if thrown by PreparedStatement methods
	 */
	protected void doSetValue(PreparedStatement ps, int parameterPosition, int argType, @Nullable Object argValue)
			throws SQLException {

		StatementCreatorUtils.setParameterValue(ps, parameterPosition, argType, argValue);
	}

	@Override
	public void cleanupParameters() {
		StatementCreatorUtils.cleanupParameters(this.args);
	}

}
