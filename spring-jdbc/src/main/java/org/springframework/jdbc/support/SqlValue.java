/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Simple interface for complex types to be set as statement parameters.
 *
 * <p>Implementations perform the actual work of setting the actual values. They must
 * implement the callback method {@code setValue} which can throw SQLExceptions
 * that will be caught and translated by the calling code. This callback method has
 * access to the underlying Connection via the given PreparedStatement object, if that
 * should be needed to create any database-specific objects.
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 * @see org.springframework.jdbc.core.SqlTypeValue
 * @see org.springframework.jdbc.core.DisposableSqlTypeValue
 */
public interface SqlValue {

	/**
	 * Set the value on the given PreparedStatement.
	 * @param ps the PreparedStatement to work on
	 * @param paramIndex the index of the parameter for which we need to set the value
	 * @throws SQLException if an SQLException is encountered while setting parameter values
	 */
	void setValue(PreparedStatement ps, int paramIndex) throws SQLException;

	/**
	 * Clean up resources held by this value object.
	 */
	void cleanup();

}
