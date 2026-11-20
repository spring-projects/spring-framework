/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

/**
 * Simple adapter for {@link PreparedStatementSetter} that applies a given array
 * of arguments.
 *
 * @author Juergen Hoeller
 * @since 3.2.3
 */
public class ArgumentPreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {

	private final @Nullable Object @Nullable [] args;


	/**
	 * Create a new {@code ArgumentPreparedStatementSetter} for the given arguments.
	 * @param args the arguments to set
	 */
	public ArgumentPreparedStatementSetter(@Nullable Object @Nullable [] args) {
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
	 * Set the value for the prepared statement's specified parameter position
	 * using the supplied value.
	 * <p>This method can be overridden by subclasses if needed.
	 * @param ps the PreparedStatement
	 * @param parameterPosition index of the parameter position
	 * @param argValue the value to set
	 * @throws SQLException if thrown by PreparedStatement methods
	 */
	protected void doSetValue(PreparedStatement ps, int parameterPosition, @Nullable Object argValue)
			throws SQLException {

		if (argValue instanceof SqlParameterValue paramValue) {
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
